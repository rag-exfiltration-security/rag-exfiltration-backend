package edu.eci.rag.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Deja cada interaccion registrada en dos lugares: el log de la aplicacion
 * (para seguirla en vivo durante una demo) y un archivo JSONL en
 * evidencias/fase-A (para poder anexarla despues como evidencia del avance
 * o procesarla con un script al calcular metricas).
 *
 * Este logger no toma ninguna decision de seguridad; solo registra lo que
 * paso. La idea es que el mismo formato de log sirva sin cambios para los
 * tres avances, asi las comparaciones entre fases son directas.
 */
@Component
public class QueryAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(QueryAuditLogger.class);

    private final ObjectMapper objectMapper;
    private final Path archivoLog;

    public QueryAuditLogger(
            @Value("${rag.audit.log-file:evidencias/fase-A/query-log.jsonl}") String archivoLog) {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.archivoLog = Path.of(archivoLog);
    }

    public synchronized void registrar(QueryLogEntry entrada) {
        log.info("[AUDITORIA] usuario={} rol={} pregunta=\"{}\" fragmentos={} respuestaLen={}",
                entrada.username(), entrada.rolResuelto(), entrada.pregunta(),
                entrada.fragmentosRecuperados().size(), entrada.respuesta().length());

        try {
            if (archivoLog.getParent() != null) {
                Files.createDirectories(archivoLog.getParent());
            }
            String linea = objectMapper.writeValueAsString(entrada) + System.lineSeparator();
            Files.writeString(archivoLog, linea,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("No se pudo escribir el log de auditoria en '{}': {}",
                    archivoLog.toAbsolutePath(), e.getMessage());
        }
    }
}
