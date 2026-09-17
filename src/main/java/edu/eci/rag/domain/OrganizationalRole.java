package edu.eci.rag.domain;

/**
 * Roles organizacionales de la empresa ficticia usada en el corpus de prueba.
 * Un documento puede autorizar a uno o varios roles (ver CorpusFrontMatterParser).
 */
public enum OrganizationalRole {
    RRHH,
    FINANZAS,
    GERENCIA;

    public static OrganizationalRole fromLabel(String label) {
        String normalizado = label.trim().toUpperCase();
        return switch (normalizado) {
            case "RRHH", "RECURSOS_HUMANOS", "RECURSOS HUMANOS" -> RRHH;
            case "FINANZAS" -> FINANZAS;
            case "GERENCIA" -> GERENCIA;
            default -> throw new IllegalArgumentException("Rol desconocido: " + label);
        };
    }
}
