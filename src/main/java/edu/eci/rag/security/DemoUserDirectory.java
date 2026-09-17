package edu.eci.rag.security;

import edu.eci.rag.domain.OrganizationalRole;
import edu.eci.rag.domain.UserProfile;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Directorio de usuarios de prueba para las evidencias del proyecto.
 *
 * Esto reemplaza, de forma deliberadamente simplificada, al sistema de
 * autenticacion real de una organizacion (LDAP, SSO, base de datos de
 * usuarios, etc). Lo que sí se conserva de un sistema real es el principio:
 * el rol de un usuario nunca lo declara el cliente en la peticion, lo
 * resuelve el backend a partir de una fuente propia. Sin esto, cualquiera
 * podria mandar {"username": "x", "role": "GERENCIA"} y saltarse todo control
 * de acceso que se construya despues.
 *
 * Para las pruebas de exfiltracion del Avance 1 (sin filtrado) esta
 * resolucion de rol no cambia el resultado, porque el pipeline todavia no
 * filtra nada. El directorio existe para dejar ya construido el punto de
 * extension que va a usar el filtrado del Avance 2.
 */
@Component
public class DemoUserDirectory {

    private static final Map<String, OrganizationalRole> USUARIOS = Map.of(
            "ana.torres", OrganizationalRole.RRHH,
            "pedro.rojas", OrganizationalRole.RRHH,
            "carlos.gomez", OrganizationalRole.FINANZAS,
            "sofia.beltran", OrganizationalRole.FINANZAS,
            "laura.medina", OrganizationalRole.GERENCIA
    );

    public Optional<UserProfile> resolve(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        OrganizationalRole role = USUARIOS.get(username.trim());
        if (role == null) {
            return Optional.empty();
        }
        return Optional.of(new UserProfile(username.trim(), role));
    }
}
