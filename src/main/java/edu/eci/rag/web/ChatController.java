package edu.eci.rag.web;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import edu.eci.rag.corpus.DocumentMetadataKeys;
import edu.eci.rag.domain.UserProfile;
import edu.eci.rag.rag.NaiveRagService;
import edu.eci.rag.security.DemoUserDirectory;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unico endpoint del Avance 1: recibe una pregunta de un usuario, resuelve
 * su rol de forma server-side (nunca confiando en lo que mande el cliente),
 * y delega la respuesta al pipeline RAG sin filtrado.
 *
 * Este es el punto de entrada que usan los tres ataques de exfiltracion
 * (ver evidencias/fase-A y ataques-demo/): todos mandan una peticion aqui,
 * la unica diferencia entre ellos es como formulan "message".
 */
@RestController
public class ChatController {

    private final DemoUserDirectory userDirectory;
    private final NaiveRagService ragService;

    public ChatController(DemoUserDirectory userDirectory, NaiveRagService ragService) {
        this.userDirectory = userDirectory;
        this.ragService = ragService;
    }

    @PostMapping("/api/chat")
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request) {
        Optional<UserProfile> usuario = userDirectory.resolve(request.username());

        if (usuario.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponse("Usuario desconocido: " + request.username()));
        }

        return ResponseEntity.ok(responder(usuario.get(), request.message()));
    }

    /**
     * Cuerpo de una respuesta de error. Se declara aparte, en vez de devolver
     * un String suelto, para que el codigo de estado 401 no le cambie el tipo
     * de dato al metodo (eso fue justo lo que hizo fallar la compilacion la
     * primera vez: un ResponseEntity<ChatResponse> y un ResponseEntity<String>
     * no unifican dentro del mismo Optional.map/orElseGet).
     */
    private record ErrorResponse(String error) {
    }

    private ChatResponse responder(UserProfile usuario, String pregunta) {
        NaiveRagService.RagAnswer respuesta = ragService.responder(usuario, pregunta);

        List<ChatResponse.FragmentoInfo> fragmentos = respuesta.fragmentosRecuperados().stream()
                .map(this::aFragmentoInfo)
                .toList();

        return new ChatResponse(
                respuesta.respuesta(),
                usuario.username(),
                usuario.role().name(),
                fragmentos
        );
    }

    private ChatResponse.FragmentoInfo aFragmentoInfo(EmbeddingMatch<TextSegment> match) {
        return new ChatResponse.FragmentoInfo(
                match.embedded().metadata().getString(DocumentMetadataKeys.DOCUMENT_ID),
                match.embedded().metadata().getString(DocumentMetadataKeys.TITULO),
                match.embedded().metadata().getString(DocumentMetadataKeys.CLASIFICACION),
                match.embedded().metadata().getString(DocumentMetadataKeys.ROLES_AUTORIZADOS),
                match.score()
        );
    }
}