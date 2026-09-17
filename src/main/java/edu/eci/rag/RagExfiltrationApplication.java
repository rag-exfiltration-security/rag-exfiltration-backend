package edu.eci.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend. Al arrancar, CorpusIngestionRunner carga el
 * corpus de prueba en el vector store y el endpoint /api/chat queda listo
 * para recibir las pruebas de exfiltracion del Avance 1.
 */
@SpringBootApplication
public class RagExfiltrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagExfiltrationApplication.class, args);
    }
}
