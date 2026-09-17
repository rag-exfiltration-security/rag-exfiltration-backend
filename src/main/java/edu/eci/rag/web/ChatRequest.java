package edu.eci.rag.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Peticion del endpoint /api/chat.
 *
 * A proposito, este DTO no tiene un campo "role": el rol nunca lo declara
 * el cliente, lo resuelve el backend a partir de "username" usando
 * DemoUserDirectory. Ver la nota de diseno en esa clase.
 */
public record ChatRequest(
        @NotBlank(message = "username es obligatorio") String username,
        @NotBlank(message = "message es obligatorio") String message
) {
}
