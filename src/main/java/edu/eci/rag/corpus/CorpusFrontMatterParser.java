package edu.eci.rag.corpus;

import edu.eci.rag.domain.ClassificationLevel;
import edu.eci.rag.domain.OrganizationalRole;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Lee el encabezado de metadatos que llevan los archivos del corpus, 
 * El formato esperado es:
 *
 * <pre>
 * ---
 * titulo: Politica de vacaciones 2026
 * clasificacion: publico
 * roles: rrhh, finanzas, gerencia
 * usuarios:
 * ---
 * Contenido del documento en Markdown...
 * </pre>
 *
 * El campo "usuarios" es opcional y solo se usa cuando la restriccion es
 * individual ademas de por rol o clasificacion.
 */
@Component
public class CorpusFrontMatterParser {

    private static final String DELIMITADOR = "---";

    public CorpusDocument parse(String documentId, String contenidoCrudo) {
        String[] lineas = contenidoCrudo.replace("\r\n", "\n").split("\n", -1);

        if (lineas.length < 2 || !lineas[0].trim().equals(DELIMITADOR)) {
            throw new IllegalArgumentException(
                    "El documento " + documentId + " no empieza con el front matter '---'");
        }

        int finEncabezado = -1;
        for (int i = 1; i < lineas.length; i++) {
            if (lineas[i].trim().equals(DELIMITADOR)) {
                finEncabezado = i;
                break;
            }
        }
        if (finEncabezado == -1) {
            throw new IllegalArgumentException(
                    "El documento " + documentId + " no cierra el front matter con '---'");
        }

        String titulo = null;
        ClassificationLevel clasificacion = null;
        List<OrganizationalRole> roles = new ArrayList<>();
        List<String> usuarios = new ArrayList<>();

        for (int i = 1; i < finEncabezado; i++) {
            String linea = lineas[i].trim();
            if (linea.isEmpty()) {
                continue;
            }
            int separador = linea.indexOf(':');
            if (separador < 0) {
                continue;
            }
            String clave = linea.substring(0, separador).trim().toLowerCase();
            String valor = linea.substring(separador + 1).trim();

            switch (clave) {
                case "titulo" -> titulo = valor;
                case "clasificacion" -> clasificacion = ClassificationLevel.fromLabel(valor);
                case "roles" -> roles = parseListaRoles(valor);
                case "usuarios" -> usuarios = parseListaUsuarios(valor);
                default -> { /* clave desconocida, se ignora */ }
            }
        }

        if (titulo == null || clasificacion == null) {
            throw new IllegalArgumentException(
                    "El documento " + documentId + " debe declarar 'titulo' y 'clasificacion'");
        }

        String contenido = String.join("\n",
                Arrays.copyOfRange(lineas, finEncabezado + 1, lineas.length)).trim();

        return new CorpusDocument(documentId, titulo, clasificacion, roles, usuarios, contenido);
    }

    private List<OrganizationalRole> parseListaRoles(String valor) {
        if (valor.isBlank()) {
            return List.of();
        }
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(OrganizationalRole::fromLabel)
                .toList();
    }

    private List<String> parseListaUsuarios(String valor) {
        if (valor.isBlank()) {
            return List.of();
        }
        return Arrays.stream(valor.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
