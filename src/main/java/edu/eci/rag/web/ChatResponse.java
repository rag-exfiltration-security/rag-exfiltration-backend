package edu.eci.rag.web;

import java.util.List;

/**
 * Respuesta del endpoint /api/chat.
 *
 * El campo "fragmentosRecuperados" existe para instrumentar las pruebas de
 * exfiltracion del Avance 1: deja ver, para cada ataque, exactamente que
 * documentos entraron al contexto del modelo y con que nivel de
 * clasificacion. En un sistema real esto no se expondria en la respuesta de
 * la API.
 */
public record ChatResponse(
        String respuesta,
        String usuario,
        String rolResuelto,
        List<FragmentoInfo> fragmentosRecuperados
) {
    public record FragmentoInfo(
            String documentId,
            String titulo,
            String clasificacion,
            String rolesAutorizados,
            double score
    ) {
    }
}
