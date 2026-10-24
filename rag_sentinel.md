# RAG-Sentinel: BlackBox LLM Scanner (Versión Legacy - Basada en Streamlit)

## 1. Descripción General
La versión inicial de **RAG-Sentinel** operaba como una herramienta monolítica desarrollada en Python utilizando **Streamlit** para la interfaz gráfica y un esquema de evaluación determinista basado en diccionarios estáticos e indicadores de fuga (`leak_indicators`).

## 2. Funcionamiento y Arquitectura
* **Interfaz de Control:** Se ejecutaba enteramente en un entorno web local donde el auditor configuraba la URL del backend y seleccionaba perfiles de prueba de forma manual.
* **Inyección de Payloads:** El sistema leía un archivo estructurado `test_suite.json`, combinaba el rol del usuario con la consulta maliciosa y enviaba peticiones HTTP directas al endpoint del backend RAG.
* **Evaluación Determinista (`leak_indicators`):** 
  * Para detectar si un sistema era vulnerable, el script comparaba la respuesta de texto plano del LLM contra una lista estática de palabras prohibidas (ej. cifras exactas, nombres de proyectos como "Cobalto", contraseñas).
  * Si la palabra clave aparecía en la respuesta y el usuario no era un perfil autorizado (y no se trataba de una negación formal), el escáner catalogaba la prueba como **Vulnerable**.

## 3. Limitaciones Críticas (¿Qué le faltaba?)
* **Ausencia de LLM-as-a-Judge:** Al carecer de un modelo evaluador inteligente, el sistema dependía estrictamente de coincidencias exactas de cadenas de texto (subadenas). Esto generaba una alta tasa de falsos positivos y falsos negativos ante paráfrasis sutiles del modelo.
* **Rigidez Operativa:** Las listas de `leak_indicators` requerían mantenimiento manual constante por cada documento nuevo ingresado al corpus vectorial de la empresa.
* **Escalabilidad Limitada:** La arquitectura monolítica en Streamlit dificultaba la integración de los escaneos en pipelines de CI/CD automatizados y pruebas de regresión masivas.


## Donde encontrar RAG_SENTINEL?

```
rag-exfiltration-backend/
│
├── .github/
│   └── workflows/
│       └── rag-sentinel-audit.yml
│
├── ataques-demo/
├── corpus/
├── docs/
├── evidencias/
│
├── rag-sentinel-promptfoo/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── edu/
│       │       └── eci/
│       │           └── rag/
│       │               ├── audit/
│       │               ├── config/
│       │               ├── corpus/
│       │               ├── domain/
│       │               ├── rag/
│       │               │
│       │               ├── rag_sentinel/   ← CARPETA PRINCIPAL
│       │               │   ├── empresarial.txt
│       │               │   ├── rag_sentinel.py
│       │               │   ├── target_config.json
│       │               │   └── test_suite.json
│       │               │
│       │               ├── security/
│       │               └── web/
│       │
│       └── resources/
│
├── target/
├── .env
└── README.md

```

# Ejecución de RAG_SENTINEL

## 0. Navegar al directorio de la herramienta
```
cd src/main/java/edu/eci/rag/rag_sentinel
```

## 1. Levantar la infraestructura base
```
docker compose up -d
```

## 2. Descargar el modelo de IA local
```
docker exec -it rag-exfiltration-ollama ollama pull <nombre_del_modelo>
```

## 3. Instalar dependencias de Python
```
pip install streamlit requests
```

## 4. Ejecutar la interfaz gráfica
```
streamlit run rag_sentinel.py
```