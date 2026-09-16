<div align="center">

# 🛡️ Exfiltración de Información mediante RAG — Backend

### API en Java (Spring Boot + LangChain4j) — Control de acceso por usuario, rol y clasificación

[![Estado](https://img.shields.io/badge/estado-en%20desarrollo-yellow)]()
[![Avance](https://img.shields.io/badge/avance-1%20%2F%203-blue)]()
[![Curso](https://img.shields.io/badge/curso-Fundamentos%20de%20Seguridad%20de%20la%20Informaci%C3%B3n-informational)]()
[![Repo frontend](https://img.shields.io/badge/repo-frontend-purple)](https://github.com/ORG_O_USUARIO/rag-exfiltration-frontend)

*Escuela Colombiana de Ingeniería Julio Garavito*

</div>

---

> 📦 Este proyecto está dividido en **dos repositorios**:
> - **`rag-exfiltration-backend`** (este repo) — API, pipeline RAG, documentación del proyecto, risk register, diagramas y evidencias.
> - **[`rag-exfiltration-frontend`](https://github.com/ORG_O_USUARIO/rag-exfiltration-frontend)** — chat en Next.js desplegado en Vercel.
>
> Este repo concentra toda la documentación porque contiene la lógica y las pruebas de seguridad del sistema.

## 📑 Tabla de contenidos

- [Descripción del proyecto](#-descripción-del-proyecto)
- [Equipo](#-equipo)
- [Objetivos](#-objetivos)
- [Arquitectura](#-arquitectura)
- [Modelo de amenazas (DFD)](#-modelo-de-amenazas-dfd)
- [Tecnologías](#-tecnologías)
- [Cómo funciona](#-cómo-funciona)
- [Estructura de este repositorio](#-estructura-de-este-repositorio)
- [Metodología y fases del proyecto](#-metodología-y-fases-del-proyecto)
- [Cómo ejecutar el backend](#-cómo-ejecutar-el-backend)
- [Declaración de uso de IA](#-declaración-de-uso-de-ia)
- [Referencias](#-referencias)
- [Conclusiones](#-conclusiones)

---

## 📖 Descripción del proyecto

Los asistentes conversacionales corporativos usan cada vez más **RAG (Retrieval Augmented Generation)** para responder preguntas apoyándose en documentos internos, en lugar de depender únicamente del conocimiento aprendido durante el entrenamiento del modelo.

Esta arquitectura resuelve problemas de actualidad y precisión de la información, pero introduce una superficie de ataque nueva: cuando el motor de recuperación no respeta los permisos que la organización asigna a cada usuario, el asistente puede terminar exponiendo fragmentos de documentos restringidos a personas que jamás deberían haber tenido acceso a ellos. OWASP incluye este riesgo dentro de su listado de riesgos críticos para aplicaciones basadas en LLM (*Vector and Embedding Weaknesses*).

Este proyecto construye un asistente RAG básico, diseña ataques de exfiltración controlados contra él, y evalúa mecanismos de control de acceso (por usuario, rol y nivel de clasificación) que impidan que el asistente revele información confidencial a usuarios no autorizados.

> 📎 El marco teórico completo del proyecto está en [`docs/marco-teorico/`](docs/marco-teorico/).

## 👥 Equipo

**Grupo 4L — Ciberseguridad y Desarrollo**

| Integrante | Rol en el proyecto |
|---|---|
| Juan Pablo Caballero Castellanos | Desarrollo backend / RAG |
| Robinson Steven Nuñez Portela | Arquitectura y seguridad |
| Oscar Andrés Sánchez Porras | Pruebas de exfiltración y documentación |

**Asignatura:** Fundamentos de Seguridad de la Información
**Profesora:** Tatiana Marcela Gómez Sarmiento

## 🎯 Objetivos

**Objetivo general**

Diseñar y evaluar mecanismos de control de acceso para un sistema RAG que impidan la exfiltración de documentos restringidos hacia usuarios sin los permisos correspondientes.

**Objetivos específicos**

- Caracterizar la arquitectura de un sistema RAG y los puntos donde puede producirse una fuga de información.
- Diseñar escenarios de prueba que intenten recuperar documentos restringidos mediante consultas directas e indirectas.
- Proponer y evaluar filtros de acceso aplicados por usuario, por rol y por nivel de clasificación de la información.
- Analizar la efectividad y las limitaciones de dichos filtros frente a técnicas de evasión conocidas.

## 🏗️ Arquitectura

![Arquitectura del sistema](docs/diagrams/da_rag.png)

| Componente | Tecnología | Repositorio | Despliegue |
|---|---|---|---|
| Frontend (chat) | Next.js | `rag-exfiltration-frontend` | Vercel |
| Backend / API | Java 21 + Spring Boot | `rag-exfiltration-backend` (este repo) | Render / Railway |
| Orquestación RAG | LangChain4j | este repo | — |
| Vector store | Chroma / pgvector | este repo | Render / Railway |
| Corpus documental | 4 niveles de clasificación | este repo (`corpus/`) | — |
| LLM generador | Claude / GPT (API externa) | — | Servicio de terceros |

> ⚠️ Vercel no soporta un runtime de Java, por eso el backend vive en un repo y servicio de despliegue separados del frontend.

## 🧩 Modelo de amenazas (DFD)

![DFD del sistema](docs/diagrams/dfd_rag.png)

El diagrama de flujo de datos identifica dos límites de confianza:

1. **Cliente → Backend:** toda petición viaja por HTTPS/TLS; aquí se ubica el primer punto de control (autenticación y autorización).
2. **Backend → LLM externo:** el prompt y el contexto recuperado salen de la infraestructura propia hacia un servicio de terceros; aquí se ubica el riesgo de fuga hacia un proveedor externo.

Los vectores de ataque evaluados en el proyecto (ver [`risk-register.md`](risk-register.md)) son: **consulta directa**, **reformulación evasiva** e **inyección de instrucciones**.

## ⚙️ Tecnologías

- **Lenguaje:** Java 21
- **Framework:** Spring Boot
- **Orquestación RAG:** LangChain4j
- **Vector store:** Chroma o pgvector
- **LLM:** API de Claude o GPT
- **Control de versiones:** Git / GitHub (repo separado del frontend)
- **Despliegue:** Render o Railway

## 🔄 Cómo funciona

1. El usuario envía una consulta desde el chat (repo `rag-exfiltration-frontend`).
2. Este backend recibe la consulta junto con el usuario y su rol asignado.
3. Genera el embedding de la consulta y recupera del vector store los fragmentos más similares del corpus.
4. Los fragmentos recuperados se envían junto con la consulta al LLM, que redacta la respuesta.
5. La respuesta se devuelve al frontend y toda la interacción queda registrada en logs (usuario, fragmentos recuperados, respuesta).
6. Sobre este flujo se ejecutan los ataques de prueba definidos en la metodología, para medir cuánta información se filtra según la capa de control de acceso activa en cada avance.

## 📂 Estructura de este repositorio

```
.
├── README.md                     # Este archivo
├── risk-register.md              # Registro y matriz de riesgos del proyecto
├── pom.xml                       # (próximo paso) proyecto Maven Spring Boot
├── src/                          # (próximo paso) código fuente del backend
├── corpus/                       # Documentos de prueba clasificados
├── docs/
│   ├── marco-teorico/            # Paper base y referencias del proyecto
│   └── diagrams/
│       ├── da_rag.png            # Diagrama de arquitectura
│       └── dfd_rag.png           # Diagrama de flujo de datos (DFD)
└── evidencias/
    ├── fase-A/                   # Evidencias del Avance 1
    ├── fase-B/                   # Evidencias del Avance 2
    └── fase-C/                   # Evidencias del Avance 3
```

## 🗺️ Metodología y fases del proyecto

| Fase | Avance | Contenido |
|---|---|---|
| **A** | Avance 1 | Corpus de prueba, pipeline RAG básico **sin filtrado**, demostración de los 3 ataques de exfiltración |
| **B** | Avance 2 | Filtrado en la recuperación por usuario, rol y clasificación; métrica de "ventaja de acceso" |
| **C** | Avance 3 | Refuerzo del prompt del sistema, validación de la respuesta, análisis de riesgo residual |

Cada fase agrega sus capturas, logs y resultados en `evidencias/fase-X/`.

## ▶️ Cómo ejecutar el backend

> 🚧 Sección en construcción — se completará junto con el `pom.xml` y la implementación del Avance 1 (build, variables de entorno, ejecución local y despliegue).

## 🤖 Declaración de uso de IA

En cumplimiento del compromiso ético del equipo, se deja constancia de que se usaron herramientas de inteligencia artificial (Claude, de Anthropic) como apoyo para:

- Planeación de arquitectura y estructura de los repositorios.
- Generación de los diagramas base (DFD y arquitectura) y del contenido de este README.
- Redacción y organización de la documentación.

Las evidencias de las pruebas de exfiltración, el código del sistema RAG y los resultados presentados corresponden al trabajo realizado por los integrantes del grupo, desarrollado en entornos controlados y autorizados, tal como se certifica en el marco teórico del proyecto.

## 📚 Referencias

Ver la lista completa de referencias en el documento del marco teórico: [`docs/marco-teorico/Exfiltracion_RAG_Marco_Teorico.pdf`](docs/marco-teorico/Exfiltracion_RAG_Marco_Teorico.pdf).

## ✅ Conclusiones

> 🚧 Sección en construcción — se irá completando al cierre de cada avance, con los hallazgos acumulados del proyecto.

---

<div align="center">

*Proyecto académico — Fundamentos de Seguridad de la Información — Septiembre 2026*

</div>
