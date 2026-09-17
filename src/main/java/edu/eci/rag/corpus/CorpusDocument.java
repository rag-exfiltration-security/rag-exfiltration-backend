package edu.eci.rag.corpus;

import edu.eci.rag.domain.ClassificationLevel;
import edu.eci.rag.domain.OrganizationalRole;
import java.util.List;

/**
 * Un documento del corpus de prueba, ya parseado: su contenido mas los
 * metadatos de control de acceso que se le asignaron (nivel de clasificacion,
 * roles autorizados y, cuando la restriccion es individual, la lista concreta
 * de usuarios permitidos).
 *
 * Estos metadatos se guardan junto al embedding en el vector store desde el
 * Avance 1, aunque todavia no se usen para filtrar nada: son precisamente el
 * material que el Avance 2 va a necesitar para poder filtrar.
 */
public record CorpusDocument(
        String documentId,
        String titulo,
        ClassificationLevel clasificacion,
        List<OrganizationalRole> rolesAutorizados,
        List<String> usuariosAutorizados,
        String contenido
) {
}
