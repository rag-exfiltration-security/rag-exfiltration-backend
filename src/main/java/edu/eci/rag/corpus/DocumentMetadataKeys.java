package edu.eci.rag.corpus;

/**
 * Nombres de las claves de metadatos que se guardan junto a cada fragmento
 * indexado en el vector store. Se centralizan aqui para que el Avance 2 (que
 * va a leer estos mismos metadatos para filtrar) no tenga que adivinarlos.
 */
public final class DocumentMetadataKeys {

    public static final String DOCUMENT_ID = "documentId";
    public static final String TITULO = "titulo";
    public static final String CLASIFICACION = "clasificacion";
    public static final String ROLES_AUTORIZADOS = "rolesAutorizados";
    public static final String USUARIOS_AUTORIZADOS = "usuariosAutorizados";

    private DocumentMetadataKeys() {
    }
}
