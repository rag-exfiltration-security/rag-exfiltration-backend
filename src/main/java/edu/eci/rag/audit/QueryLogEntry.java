package edu.eci.rag.audit;

import java.time.Instant;
import java.util.List;

/**
 * AAA
 * Una linea del log de auditoria: quien pregunto, que fragmentos se
 * recuperaron para responderle (con su clasificacion) y que respondio el
 * modelo. Es el insumo crudo para las evidencias del Avance 1 y, mas
 * adelante, para calcular la metrica de "ventaja de acceso" del Avance 2.
 */
public record QueryLogEntry(
        Instant timestamp,
        String username,
        String rolResuelto,
        String pregunta,
        List<FragmentoRecuperado> fragmentosRecuperados,
        String respuesta
) {
    public record FragmentoRecuperado(
            String documentId,
            String titulo,
            String clasificacion,
            String rolesAutorizados,
            double score
    ) {
    }
}
