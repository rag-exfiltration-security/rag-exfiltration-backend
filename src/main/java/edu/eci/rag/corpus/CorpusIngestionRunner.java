package edu.eci.rag.corpus;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Al arrancar la aplicacion, recorre la carpeta del corpus, parsea cada
 * archivo con CorpusFrontMatterParser, lo fragmenta y guarda cada fragmento
 * en el vector store junto con sus metadatos de clasificacion.
 *
 */
@Component
@Order(1)
public class CorpusIngestionRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CorpusIngestionRunner.class);

    private final CorpusFrontMatterParser parser;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final String corpusPath;
    private final boolean ingestarAlArrancar;

    public CorpusIngestionRunner(
            CorpusFrontMatterParser parser,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            @Value("${rag.corpus.path:corpus}") String corpusPath,
            @Value("${rag.corpus.ingest-on-start:true}") boolean ingestarAlArrancar) {
        this.parser = parser;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.corpusPath = corpusPath;
        this.ingestarAlArrancar = ingestarAlArrancar;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!ingestarAlArrancar) {
            log.info("Ingesta del corpus deshabilitada (rag.corpus.ingest-on-start=false)");
            return;
        }

        Path raiz = Path.of(corpusPath);
        if (!Files.isDirectory(raiz)) {
            log.warn("No se encontro la carpeta del corpus en '{}'. No se ingesto nada.",
                    raiz.toAbsolutePath());
            return;
        }

        List<Path> archivos;
        try (Stream<Path> stream = Files.walk(raiz)) {
            archivos = stream.filter(p -> p.toString().endsWith(".md")).sorted().toList();
        }

        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
        int totalFragmentos = 0;
        int documentosIngestados = 0;

        for (Path archivo : archivos) {
            String documentId = raiz.relativize(archivo).toString().replace('\\', '/');
            String contenidoCrudo = Files.readString(archivo);

            CorpusDocument doc;
            try {
                doc = parser.parse(documentId, contenidoCrudo);
            } catch (IllegalArgumentException e) {
                log.error("No se pudo parsear '{}': {}", documentId, e.getMessage());
                continue;
            }

            Metadata metadata = Metadata.from(Map.of(
                    DocumentMetadataKeys.DOCUMENT_ID, doc.documentId(),
                    DocumentMetadataKeys.TITULO, doc.titulo(),
                    DocumentMetadataKeys.CLASIFICACION, doc.clasificacion().name(),
                    DocumentMetadataKeys.ROLES_AUTORIZADOS, joinRoles(doc),
                    DocumentMetadataKeys.USUARIOS_AUTORIZADOS, String.join(",", doc.usuariosAutorizados())
            ));

            Document documentoLangchain = Document.from(doc.contenido(), metadata);
            List<TextSegment> segmentos = splitter.split(documentoLangchain);

            Response<List<Embedding>> embeddings = embeddingModel.embedAll(segmentos);
            embeddingStore.addAll(embeddings.content(), segmentos);

            totalFragmentos += segmentos.size();
            documentosIngestados++;
            log.info("Ingestado '{}' [{}, roles={}] en {} fragmentos",
                    doc.titulo(), doc.clasificacion(), joinRoles(doc), segmentos.size());
        }

        log.info("Ingesta del corpus completa: {} documentos, {} fragmentos indexados",
                documentosIngestados, totalFragmentos);
    }

    private String joinRoles(CorpusDocument doc) {
        return doc.rolesAutorizados().stream()
                .map(Enum::name)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }
}
