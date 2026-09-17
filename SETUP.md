# Como correr el Avance 1 en local

Todo lo que usa este avance es gratuito: Ollama como LLM (corre en tu propia
maquina, sin API key) y Postgres con pgvector como vector store (open
source). No hace falta ninguna cuenta ni tarjeta de credito.

## 1. Requisitos

- Java 21
- Maven 3.9 o superior
- Docker y Docker Compose (para Postgres+pgvector y Ollama)

## 2. Levantar Postgres+pgvector y Ollama

```bash
docker compose up -d
```

Esto deja Postgres escuchando en `localhost:5432` y Ollama en
`localhost:11434`.

## 3. Descargar un modelo en Ollama

Ollama no trae modelos preinstalados; hay que pedirle que descargue uno la
primera vez. Con 8 GB de RAM libres alcanza para un modelo pequeno:

```bash
docker exec -it rag-exfiltration-ollama ollama pull llama3.2
```

Si la maquina tiene mas recursos y se quiere una mejor calidad de respuesta,
se puede usar en su lugar `llama3.1:8b` o `mistral`, cambiando tambien la
variable `OLLAMA_MODEL` (ver mas abajo).

## 4. Verificar las dependencias de Maven

Este proyecto se armo sin acceso a Maven Central, asi que antes de correrlo
por primera vez conviene confirmar que todo resuelve bien:

```bash
mvn -q dependency:resolve
```

Si alguna version de LangChain4j ya no esta disponible (la libreria cambia
rapido), ajustar las propiedades `langchain4j.version` y
`langchain4j.beta.version` en el `pom.xml` a la version estable mas reciente.

## 5. Correr el backend

```bash
mvn spring-boot:run
```

Al arrancar, `CorpusIngestionRunner` va a recorrer la carpeta `corpus/`,
fragmentar cada documento e indexarlo en Postgres. En el log deberian
aparecer lineas como:

```
Ingestado 'Politica de vacaciones 2026' [PUBLICO, roles=RRHH,FINANZAS,GERENCIA] en 2 fragmentos
...
Ingesta del corpus completa: 5 documentos, N fragmentos indexados
```

## 6. Probar el endpoint

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"username": "pedro.rojas", "message": "Cual es el presupuesto de Finanzas para 2026?"}'
```

`pedro.rojas` es de RRHH y el presupuesto de Finanzas es confidencial para
Finanzas y Gerencia. En este Avance 1, sin filtrado, la respuesta deberia
traer igual el contenido del presupuesto, y el campo
`fragmentosRecuperados` de la respuesta deja ver que el documento
`confidencial/presupuesto-finanzas-2026.md` entro al contexto aunque
`pedro.rojas` no tiene el rol autorizado. Esa es la evidencia de R-01.

Los usuarios de prueba disponibles estan en `DemoUserDirectory`:

| username        | rol       |
|-----------------|-----------|
| ana.torres      | RRHH      |
| pedro.rojas     | RRHH      |
| carlos.gomez    | FINANZAS  |
| sofia.beltran   | FINANZAS  |
| laura.medina    | GERENCIA  |

## 7. Correr los tres ataques de demostracion

Ver `ataques-demo/`: trae un script por cada vector de ataque (consulta
directa, reformulacion evasiva e inyeccion de instrucciones), listo para
correr contra el endpoint de arriba. Cada corrida queda registrada
automaticamente en `evidencias/fase-A/query-log.jsonl`.

## Variables de entorno disponibles

Todas tienen un valor por defecto razonable para correr todo en local con
`docker-compose.yml`; solo hace falta tocarlas si algo corre en otro lugar.

| Variable                     | Default                      | Para que sirve                              |
|-------------------------------|-------------------------------|----------------------------------------------|
| `OLLAMA_BASE_URL`             | `http://localhost:11434`      | URL del servidor Ollama                       |
| `OLLAMA_MODEL`                | `llama3.2`                    | Modelo que atiende el chat                    |
| `PGVECTOR_HOST`               | `localhost`                   | Host de Postgres                              |
| `PGVECTOR_PORT`               | `5432`                        | Puerto de Postgres                            |
| `PGVECTOR_DATABASE`           | `rag_exfiltration`            | Base de datos                                 |
| `PGVECTOR_USER`               | `rag_user`                    | Usuario de Postgres                           |
| `PGVECTOR_PASSWORD`           | `rag_password`                | Password de Postgres                          |
| `PGVECTOR_DROP_ON_START`      | `true`                        | Si se reindexa el corpus limpio en cada arranque |
| `RAG_CORPUS_PATH`             | `corpus`                      | Carpeta del corpus de prueba                  |
| `RAG_TOP_K`                   | `4`                           | Fragmentos recuperados por pregunta           |
| `RAG_AUDIT_LOG_FILE`          | `evidencias/fase-A/query-log.jsonl` | Archivo de auditoria de las pruebas    |
