package edu.eci.rag.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Arma los tres componentes de LangChain4j que usa el pipeline RAG del
 * Avance 1: el modelo de embeddings, el LLM generador y el vector store.
 * Ninguno de los tres aplica ningun control de acceso todavia; ese es
 * precisamente el punto de este avance.
 *
 * Nota de version: en LangChain4j 1.6.0 la interfaz para el LLM generador
 * es ChatModel (con el metodo chat(String)); la interfaz mas vieja
 * ChatLanguageModel con generate(String) ya no existe en esta version.
 */
@Configuration
public class RagBeansConfig {

    /**
     * Modelo de embeddings all-MiniLM-L6-v2. Corre dentro del proceso de la
     * aplicacion (ONNX), no hace llamadas externas y no tiene costo. Produce
     * vectores de 384 dimensiones.
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * LLM generador. Ollama corre local (por ejemplo via el docker-compose
     * incluido en el repo) y no requiere API key ni tiene costo. El modelo
     * concreto (por ejemplo "llama3.2") se define en application.yml.
     */
    @Bean
    public ChatModel chatModel(
            @Value("${rag.ollama.base-url}") String baseUrl,
            @Value("${rag.ollama.model-name}") String modelName) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }

    /**
     * Vector store en Postgres con la extension pgvector. Se declara con
     * createTable(true) para que la tabla se cree sola en el primer arranque;
     * dropTableFirst se deja configurable porque en el Avance 1 conviene
     * poder reiniciar el corpus limpio en cada demo.
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            @Value("${rag.vectorstore.host}") String host,
            @Value("${rag.vectorstore.port}") int port,
            @Value("${rag.vectorstore.database}") String database,
            @Value("${rag.vectorstore.user}") String user,
            @Value("${rag.vectorstore.password}") String password,
            @Value("${rag.vectorstore.table}") String table,
            @Value("${rag.vectorstore.dimension}") int dimension,
            @Value("${rag.vectorstore.drop-on-start:true}") boolean dropOnStart) {
        return PgVectorEmbeddingStore.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(user)
                .password(password)
                .table(table)
                .dimension(dimension)
                .createTable(true)
                .dropTableFirst(dropOnStart)
                .build();
    }
}