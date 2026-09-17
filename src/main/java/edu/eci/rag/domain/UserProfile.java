package edu.eci.rag.domain;

/**
 * Identidad de un usuario del chat, resuelta por el backend a partir del
 * nombre de usuario. El cliente nunca envia el rol directamente: si lo
 * hiciera, un atacante solo tendria que editar la peticion para declararse
 * Gerencia. Ver DemoUserDirectory.
 *
 * En este Avance 1 el perfil solo se usa para quedar registrado en el log de
 * auditoria; todavia no se usa para filtrar la recuperacion.
 */
public record UserProfile(String username, OrganizationalRole role) {
}
