package edu.eci.rag.domain;

/**
 * Niveles de clasificacion de la informacion, equivalentes a los que sugiere
 * ISO/IEC 27001:2022 (control A.5.12): publico, interno, confidencial y secreto.
 *
 * El orden de declaracion importa: se usa el ordinal para comparar niveles
 * (ver Avance 2, cuando se compare el nivel del fragmento contra el nivel
 * maximo autorizado para el rol del usuario). En el Avance 1 este campo solo
 * se guarda como metadato; todavia no se usa para filtrar nada.
 */
public enum ClassificationLevel {
    PUBLICO,
    INTERNO,
    CONFIDENCIAL,
    SECRETO;

    /**
     * Interpreta el valor de clasificacion escrito en el front matter de un
     * documento del corpus.
     */
    public static ClassificationLevel fromLabel(String label) {
        if (label == null) {
            throw new IllegalArgumentException("El documento no declara un nivel de clasificacion");
        }
        String normalizado = label.trim().toUpperCase();
        return switch (normalizado) {
            case "PUBLICO", "PÚBLICO" -> PUBLICO;
            case "INTERNO" -> INTERNO;
            case "CONFIDENCIAL" -> CONFIDENCIAL;
            case "SECRETO" -> SECRETO;
            default -> throw new IllegalArgumentException(
                    "Nivel de clasificacion desconocido: " + label);
        };
    }
}
