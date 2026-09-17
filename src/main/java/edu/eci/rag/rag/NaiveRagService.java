package edu.eci.rag.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import edu.eci.rag.audit.QueryAuditLogger;
import edu.eci.rag.audit.QueryLogEntry;
import edu.eci.rag.corpus.DocumentMetadataKeys;
import edu.eci.rag.domain.UserProfile;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Pipeline RAG del Avance 1: EXACTAMENTE lo que describe la seccion 5.1 del
 * marco teorico como "sistema sin filtrado". Dado un usuario y una pregunta:
 *
 * <ol>
 *     <li>Genera el embedding de la pregunta.</li>
 *     <li>Recupera los k fragmentos mas similares de TODO el corpus, sin
 *         mirar el rol del usuario ni la clasificacion del fragmento.</li>
 *     <li>Arma un prompt con esos fragmentos y se lo pasa al LLM tal cual.</li>
 *     <li>Registra la interaccion completa (que se recupero, que se
 *         respondio) en el log de auditoria.</li>
 * </ol>
 *
 * Esta clase es intencionalmente vulnerable: es el objetivo de los tres
 * ataques de exfiltracion del Avance 1 (consulta directa, reformulacion
 * evasiva e inyeccion de instrucciones). El Avance 2 no va a modificar esta
 * clase por dentro; va a insertar un paso de filtrado ANTES del paso 3,
 * usando los mismos metadatos que ya se guardan en el vector store.
 */
@Service
public class NaiveRagService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatModel chatModel;
    private final QueryAuditLogger auditLogger;
    private final int topK;
    private final double minScore;

    public NaiveRagService(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            ChatModel chatModel,
            QueryAuditLogger auditLogger,
            @Value("${rag.retrieval.top-k:4}") int topK,
            @Value("${rag.retrieval.min-score:0.0}") double minScore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
        this.auditLogger = auditLogger;
        this.topK = topK;
        this.minScore = minScore;
    }

    public RagAnswer responder(UserProfile usuario, String pregunta) {
        Embedding embeddingPregunta = embeddingModel.embed(pregunta).content();

        EmbeddingSearchRequest solicitud = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingPregunta)
                .maxResults(topK)
                .minScore(minScore)
                // Sin filtro: en LangChain4j esto se resolveria pasando un
                // Filter aqui, construido a partir del rol y clasificacion
                // del usuario. Ese filtro es exactamente lo que introduce
                // el Avance 2 y lo unico que cambia en este metodo.
                .build();

        EmbeddingSearchResult<TextSegment> resultado = embeddingStore.search(solicitud);
        List<EmbeddingMatch<TextSegment>> coincidencias = resultado.matches();

        String contexto = construirContexto(coincidencias);
        String prompt = construirPrompt(pregunta, contexto);

        String respuesta = chatModel.chat(prompt);

        registrarAuditoria(usuario, pregunta, coincidencias, respuesta);

        return new RagAnswer(respuesta, coincidencias);
    }

    private String construirContexto(List<EmbeddingMatch<TextSegment>> coincidencias) {
        StringBuilder sb = new StringBuilder();
        for (EmbeddingMatch<TextSegment> match : coincidencias) {
            String titulo = match.embedded().metadata().getString(DocumentMetadataKeys.TITULO);
            sb.append("### ").append(titulo).append('\n')
                    .append(match.embedded().text()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Prompt deliberadamente simple, sin ninguna instruccion de seguridad.
     * No le dice al modelo que evite revelar informacion restringida porque,
     * en este avance, no existe todavia el concepto de "restringido para
     * este usuario": todo lo que llego al contexto se puede usar. El
     * refuerzo de este mensaje de sistema es justamente el control que
     * introduce el Avance 3.
     */
    private String construirPrompt(String pregunta, String contexto) {
        return """
                Eres el asistente interno de la empresa. Responde la pregunta del
                usuario usando el siguiente contexto recuperado de la base de
                documentos de la empresa.

                Contexto:
                %s

                Pregunta: %s
                """.formatted(contexto, pregunta);
    }

    private void registrarAuditoria(
            UserProfile usuario,
            String pregunta,
            List<EmbeddingMatch<TextSegment>> coincidencias,
            String respuesta) {

        List<QueryLogEntry.FragmentoRecuperado> fragmentos = coincidencias.stream()
                .map(match -> new QueryLogEntry.FragmentoRecuperado(
                        match.embedded().metadata().getString(DocumentMetadataKeys.DOCUMENT_ID),
                        match.embedded().metadata().getString(DocumentMetadataKeys.TITULO),
                        match.embedded().metadata().getString(DocumentMetadataKeys.CLASIFICACION),
                        match.embedded().metadata().getString(DocumentMetadataKeys.ROLES_AUTORIZADOS),
                        match.score()))
                .toList();

        auditLogger.registrar(new QueryLogEntry(
                Instant.now(),
                usuario.username(),
                usuario.role().name(),
                pregunta,
                fragmentos,
                respuesta
        ));
    }

    /**
     * Resultado devuelto al controlador. Incluye los fragmentos recuperados
     * y su clasificacion a proposito: en produccion esto no se expondria en
     * la respuesta de la API, pero para las pruebas de exfiltracion del
     * Avance 1 es justo lo que se necesita ver para documentar que se filtro
     * y que no.
     */
    public record RagAnswer(String respuesta, List<EmbeddingMatch<TextSegment>> fragmentosRecuperados) {
    }
}