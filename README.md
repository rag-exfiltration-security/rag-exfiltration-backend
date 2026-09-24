<div align="center">

# 🛡️ Exfiltración de Información mediante RAG. Backend

### API en Java con Spring Boot y LangChain4j. Control de acceso por usuario, rol y clasificación

[![Estado](https://img.shields.io/badge/estado-en%20desarrollo-yellow)]()
[![Avance](https://img.shields.io/badge/avance-1%20%2F%203-blue)]()
[![Curso](https://img.shields.io/badge/curso-Fundamentos%20de%20Seguridad%20de%20la%20Informaci%C3%B3n-informational)]()
[![Repo frontend](https://img.shields.io/badge/repo-frontend-purple)](https://github.com/ORG_O_USUARIO/rag-exfiltration-frontend)

*Escuela Colombiana de Ingeniería Julio Garavito*

</div>

---

## 📑 Tabla de contenidos

- [Descripción del proyecto](#-descripción-del-proyecto)
- [Equipo](#-equipo)
- [Objetivos](#-objetivos)
- [Arquitectura](#-arquitectura)
- [Modelo de amenazas DFD](#-modelo-de-amenazas-dfd)
- [Tecnologías](#-tecnologías)
- [Cómo funciona](#-cómo-funciona)
- [Estructura de este repositorio](#-estructura-de-este-repositorio)
- [Metodología y fases del proyecto](#-metodología-y-fases-del-proyecto)
- [Cómo ejecutar el backend](#-cómo-ejecutar-el-backend)
- [Declaración de uso de IA](#-declaración-de-uso-de-ia)
- [Análisis de costos y riesgos económicos](#-análisis-de-costos-y-riesgos-económicos)
- [Referencias](#-referencias)
- [Conclusiones](#-conclusiones)

---

## 📖 Descripción del proyecto

Los asistentes conversacionales corporativos usan cada vez más RAG, sigla de Retrieval Augmented Generation, para responder preguntas apoyándose en documentos internos, en lugar de depender únicamente del conocimiento aprendido durante el entrenamiento del modelo.

Esta arquitectura resuelve problemas de actualidad y precisión de la información, pero introduce una superficie de ataque nueva. Cuando el motor de recuperación no respeta los permisos que la organización asigna a cada usuario, el asistente puede terminar exponiendo fragmentos de documentos restringidos a personas que jamás deberían haber tenido acceso a ellos. OWASP incluye este riesgo dentro de su listado de riesgos críticos para aplicaciones basadas en LLM, en la categoría de debilidades de vectores e incrustaciones.

Este proyecto construye un asistente RAG básico, diseña ataques de exfiltración controlados contra él, y evalúa mecanismos de control de acceso por usuario, rol y nivel de clasificación que impidan que el asistente revele información confidencial a usuarios no autorizados.

> 📎 El marco teórico completo del proyecto está en [`docs/marco-teorico/`](docs/marco-teorico/).

## 👥 Equipo

**Grupo 4L. Ciberseguridad y Desarrollo**

| Integrante | Rol en el proyecto |
|---|---|
| Juan Pablo Caballero Castellanos | Desarrollo backend y RAG |
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

El sistema se organiza en tres zonas con niveles de confianza distintos: el cliente, que es el navegador del usuario o del Red Team, la infraestructura propia del proyecto con el frontend, el backend y el almacén vectorial, y un servicio de terceros que provee el modelo de lenguaje. Esa separación es la que define dónde se puede aplicar un control y dónde ya no, que es justamente lo que el proyecto quiere medir.

### Componentes

| Componente | Tecnología | Repositorio | Despliegue |
|---|---|---|---|
| Frontend del chat | Next.js | `rag-exfiltration-frontend` | Vercel |
| Backend y API | Java 21 con Spring Boot 3.3.13 | `rag-exfiltration-backend`, este repo | Render o Railway |
| Orquestación RAG | LangChain4j 1.6.x | este repo | No aplica |
| Vector store | PostgreSQL + pgvector, open source | este repo | Render o Railway |
| Embeddings | Modelo `all-MiniLM-L6-v2` | este repo | No aplica |
| Corpus documental | 4 niveles de clasificación | este repo, carpeta `corpus/` | No aplica |
| LLM generador | Ollama local, modelo Llama 3.2 | este repo | Local, sin servicio de terceros |

### Qué hace cada pieza

**Frontend en Next.js sobre Vercel.** Es únicamente la interfaz de chat. No tiene lógica de autorización ni credenciales del LLM. Su única responsabilidad es autenticar al usuario y enviar la consulta al backend. Cualquier control implementado aquí sería trivial de evadir, porque un atacante puede llamar directamente al endpoint `/chat` sin pasar por la interfaz. Por eso el frontend se trata como código no confiable en el modelo de amenazas.

**Backend y API con Spring Boot y LangChain4j.** Es el punto donde se concentra toda la seguridad del sistema. Recibe la consulta junto con la identidad y el rol del usuario, genera el embedding, consulta el almacén vectorial, arma el prompt y llama al LLM. Es el único componente que conoce simultáneamente la identidad del usuario y el contenido de los fragmentos recuperados, y por eso es el único lugar donde tiene sentido decidir qué puede salir hacia el modelo. Las tres capas de defensa del proyecto se implementan aquí: filtrado en la recuperación, refuerzo del mensaje del sistema y validación de la respuesta.

**Vector store con PostgreSQL y pgvector.** Guarda las incrustaciones de cada fragmento del corpus junto con sus metadatos: nivel de clasificación, roles autorizados y, cuando la restricción es individual, la lista de usuarios permitidos. Esos metadatos son lo que hace posible el filtrado del Avance 2. Sin ellos, la búsqueda sólo puede ordenar por similitud semántica y no tiene forma de saber quién pregunta.

**Corpus documental.** Archivos de prueba etiquetados en cuatro niveles equivalentes a los que sugiere ISO/IEC 27001: público, interno, confidencial y secreto. Cada documento se asocia además a roles como recursos humanos, finanzas o gerencia. Es material sintético creado para el proyecto y no contiene información real de ninguna organización.

**LLM generador con Ollama, ejecutado localmente.** Redacta la respuesta final a partir de los fragmentos que le entrega el backend, corriendo el modelo Llama 3.2 en la propia infraestructura del proyecto en lugar de un servicio de terceros. Esto reduce el riesgo de exposición de credenciales de API y de que un fragmento salga fuera del perímetro controlado, aunque el resto de la regla de diseño principal del sistema se mantiene igual: un fragmento no autorizado nunca debe llegar al prompt, porque una vez enviado al modelo ya no hay control posible sobre él.

### Decisiones de diseño relevantes para la seguridad

- **El filtrado ocurre antes de la generación, no después.** Filtrar la respuesta del modelo sería una defensa tardía, porque el contenido restringido ya habría salido hacia el proveedor externo. La validación de la respuesta del Avance 3 existe como última línea de defensa, no como control principal.
- **Backend y frontend están en repositorios y servicios de despliegue separados.** Esto mantiene las credenciales del LLM y la lógica de autorización fuera de cualquier artefacto que se entregue al navegador, lo que atiende el riesgo R-08 del risk register.
- **La identidad del usuario viaja con cada consulta.** El backend no confía en un rol enviado por el cliente sin verificar. La sesión autenticada es la fuente de verdad del perfil con el que se filtra.
- **Todo el tráfico externo va cifrado con HTTPS y TLS**, tanto del cliente hacia el backend como del backend hacia el LLM.

## 🧩 Modelo de amenazas DFD

![DFD del sistema](docs/diagrams/dfd_rag.png)

El diagrama de flujo de datos descompone el sistema en procesos, almacenes y entidades externas, y traza sobre ellos los límites de confianza, es decir las fronteras que un dato cruza al pasar de un dominio controlado a otro. Los riesgos aparecen precisamente en esos cruces, por lo que el DFD es la base del [`risk-register.md`](risk-register.md).

### Elementos del diagrama

**Entidades externas**

- **Usuario autenticado.** Persona legítima con un rol asignado de RRHH, Finanzas o Gerencia. Representa el flujo normal del sistema y sirve de línea base para medir la métrica de ventaja de acceso.
- **Atacante o Red Team.** El mismo rol de usuario autenticado, pero con intención de acceder a material por encima de su nivel. Es un usuario legítimo del sistema, no un intruso externo. El modelo de amenazas asume que la autenticación ya fue superada y que lo que falla es la autorización.
- **Blue Team.** Analista que revisa los logs para detectar intentos de exfiltración. Aporta la capa de detección del proyecto.

**Procesos**

- **P1, Backend API.** Autentica, orquesta el pipeline RAG y registra la actividad. Concentra los tres puntos de control del sistema.
- **P2, LLM generador.** Redacta la respuesta final. Es un proceso que el equipo no controla ni puede auditar.

**Almacenes de datos**

- **D1, Vector store.** Incrustaciones y metadatos de clasificación del corpus.
- **D2, Logs de consultas.** Registro de usuario, fragmentos recuperados y respuesta entregada. Da trazabilidad a las pruebas, cumpliendo el accounting del modelo AAA, y es a la vez un activo sensible porque puede contener fragmentos del material restringido, lo que corresponde al riesgo R-09.

### Límite de confianza 1: del cliente hacia el backend

Toda petición viaja por HTTPS y TLS, y aquí se ubica el primer punto de control, que es la autenticación y la autorización. Lo que cruza esta frontera es una consulta más una identidad. Nada de lo que llegue del cliente puede considerarse confiable, incluido el rol declarado. Si el backend aceptara el perfil que le envía el navegador, un atacante sólo tendría que modificar la petición para elevarse a Gerencia.

Los tres vectores de ataque del proyecto entran por esta frontera:

| Vector | Qué intenta | Riesgo asociado |
|---|---|---|
| **Consulta directa** | Pedir explícitamente el contenido de un documento restringido | R-01 |
| **Reformulación evasiva** | Pedir un resumen o paráfrasis para evitar filtros por palabra clave | R-02 |
| **Inyección de instrucciones** | Insertar una orden que le pida al modelo ignorar las restricciones | R-03 |

Los dos primeros se neutralizan en la etapa de recuperación durante el Avance 2. Si el fragmento nunca se recupera, da igual cómo se formule la pregunta. El tercero no, porque no ataca al recuperador sino al modelo, y por eso se atiende en el Avance 3 con refuerzo del prompt y validación de la respuesta.

### Límite de confianza 2: del backend hacia el LLM externo

El prompt y el contexto recuperado se procesan con el modelo servido por Ollama en la infraestructura propia del proyecto. Aun así, la regla de diseño se mantiene igual de estricta: sólo debe llegar al prompt lo que el usuario ya está autorizado a ver, porque una vez que el modelo recibe un fragmento ya no hay control posterior posible sobre lo que hace con él.

### Riesgos que no cruzan ninguna frontera

El DFD también deja ver amenazas que actúan directamente sobre los almacenes, sin pasar por la interfaz conversacional:

- **Envenenamiento del vector store, riesgo R-04.** Documentos maliciosos insertados en la ingesta, que luego se recuperan ante consultas específicas.
- **Inversión de incrustaciones, riesgo R-05.** Reconstrucción aproximada del texto original a partir de su representación numérica, por parte de quien tenga acceso al almacén.

Ambos confirman que el filtrado en la recuperación protege la interfaz conversacional, pero no sustituye la protección del almacén vectorial con sus propios controles de acceso y cifrado en reposo.

> 📋 El detalle de probabilidad, impacto, tratamiento y responsable de cada riesgo está en [`risk-register.md`](risk-register.md).

## ⚙️ Tecnologías

- **Lenguaje:** Java 21
- **Framework:** Spring Boot 3.3.13
- **Orquestación RAG:** LangChain4j 1.6.x
- **Vector store:** PostgreSQL + pgvector, open source
- **Embeddings:** Modelo `all-MiniLM-L6-v2`
- **LLM:** Ollama corriendo en local, modelo Llama 3.2
- **Control de versiones:** Git y GitHub
- **Despliegue:** Render o Railway

## 🔄 Cómo funciona

1. El usuario envía una consulta desde el chat del repo `rag-exfiltration-frontend`.
2. Este backend recibe la consulta junto con el usuario y su rol asignado.
3. Genera el embedding de la consulta con el modelo `all-MiniLM-L6-v2` y recupera de PostgreSQL/pgvector los fragmentos más similares del corpus.
4. Los fragmentos recuperados se envían junto con la consulta al LLM servido por Ollama (Llama 3.2), que redacta la respuesta.
5. La respuesta se devuelve al frontend y toda la interacción queda registrada en logs con el usuario, los fragmentos recuperados y la respuesta.
6. Sobre este flujo se ejecutan los ataques de prueba definidos en la metodología, para medir cuánta información se filtra según la capa de control de acceso activa en cada avance.

## 📂 Estructura de este repositorio

El proyecto se reparte en dos repositorios. Este, `rag-exfiltration-backend`, contiene la API, el pipeline RAG y toda la documentación de seguridad. El otro, [`rag-exfiltration-frontend`](https://github.com/ORG_O_USUARIO/rag-exfiltration-frontend), contiene el chat en Next.js. La documentación se concentra aquí porque es este repo el que contiene la lógica y las pruebas de seguridad del sistema.

```
.
├── README.md                     # Este archivo
├── risk-register.md              # Registro y matriz de riesgos del proyecto
├── pom.xml                       # Próximo paso: proyecto Maven Spring Boot
├── src/                          # Próximo paso: código fuente del backend
├── corpus/                       # Documentos de prueba clasificados
├── docs/
│   ├── marco-teorico/             # Paper base y referencias del proyecto
│   │   └── Exfiltracion_RAG_Marco_Teorico.pdf
│   ├── estadisticas/              # Informe de evidencia estadística de riesgos RAG/LLM
│   │   └── RAG_Exfiltration_Riesgos_Informe.docx
│   ├── cost/                      # Análisis de costos y riesgos económicos
│   │   └── RAG_Exfiltration_Analisis_Costos_Riesgos.docx
│   └── diagrams/
│       ├── da_rag.png            # Diagrama de arquitectura
│       └── dfd_rag.png           # Diagrama de flujo de datos
└── evidencias/
    ├── fase-A/                   # Evidencias del Avance 1
    ├── fase-B/                   # Evidencias del Avance 2
    └── fase-C/                   # Evidencias del Avance 3
```

## 🗺️ Metodología y fases del proyecto

| Fase | Avance | Contenido |
|---|---|---|
| **A** | Avance 1 | Corpus de prueba, pipeline RAG básico **sin filtrado**, demostración de los 3 ataques de exfiltración |
| **B** | Avance 2 | Filtrado en la recuperación por usuario, rol y clasificación, más la métrica de ventaja de acceso |
| **C** | Avance 3 | Refuerzo del prompt del sistema, validación de la respuesta, análisis de riesgo residual |

Cada fase agrega sus capturas, logs y resultados en `evidencias/fase-X/`.

## ▶️ Cómo ejecutar el backend

> 🚧 Sección en construcción. Se completará junto con el `pom.xml` y la implementación del Avance 1, incluyendo build, variables de entorno, ejecución local y despliegue.

## 🤖 Declaración de uso de IA

En cumplimiento del compromiso ético del equipo, se deja constancia de que se usaron herramientas de inteligencia artificial, concretamente Claude de Anthropic, como apoyo para:

- Planeación de arquitectura y estructura de los repositorios.
- Generación de los diagramas base de arquitectura y DFD, y del contenido de este README.
- Redacción y organización de la documentación.

Las evidencias de las pruebas de exfiltración, el código del sistema RAG y los resultados presentados corresponden al trabajo realizado por los integrantes del grupo, desarrollado en entornos controlados y autorizados, tal como se certifica en el marco teórico del proyecto.

## 📊 Evidencia externa: riesgos de exfiltración en sistemas RAG/LLM

Como complemento al marco teórico y al risk register, el equipo elaboró un informe de investigación que reúne evidencia estadística verificable sobre los riesgos de exfiltración de datos en sistemas RAG y LLM, conectando cada hallazgo con los mecanismos que este proyecto demuestra.

> 📎 Informe completo: [`docs/estadisticas/RAG_Exfiltration_Riesgos_Informe.docx`](docs/estadisticas/RAG_Exfiltration_Riesgos_Informe.docx)

**Resumen**

El informe documenta siete problemas de seguridad, cada uno con fuente primaria, metodología, muestra y limitaciones declaradas:

1. **Inyección de instrucciones**, vector #1 de exfiltración en RAG/LLM según OWASP (LLM01:2025). El estudio de Qi et al. (2024, Harvard/CMU/MBZUAI) logró 100% de éxito extrayendo el datastore de 25 GPTs de producción con máximo 2 consultas, y reconstruyó el 41% de un corpus de 77.000 palabras y el 3% de uno de 1.569.000 palabras con 100 consultas.
2. **La exfiltración escala con el tamaño del corpus y el número de consultas**: los corpus pequeños (como el de 5 documentos usado en las demos de este proyecto) son proporcionalmente más vulnerables.
3. **Brechas de datos vinculadas a IA generativa**: 13% de las organizaciones sufrió una brecha de sus modelos o aplicaciones de IA; de esas, 97% no tenía controles de acceso adecuados (IBM/Ponemon, 2025).
4. **Ausencia de gobernanza de IA**: 63% de las organizaciones no tiene política de gobernanza de IA (IBM, 2025).
5. **Shadow AI**: 81% de los empleados usa herramientas de IA no aprobadas; el porcentaje de datos corporativos sensibles compartidos con IA se triplicó del 10,7% (2023) al 34,8% (2025) (UpGuard/Cyberhaven).
6. **Control de acceso roto**, categoría #1 del OWASP Top 10 desde 2021, causa raíz transversal que el Avance 1 de este proyecto reproduce deliberadamente al no filtrar por rol ni clasificación.
7. **Brecha entre adopción y gobernanza**: las organizaciones despliegan IA más rápido de lo que implementan controles; Gartner proyecta que más del 40% de las brechas de IA para 2027 se originará en el uso indebido de GenAI a través de fronteras.

Las cifras centrales del informe (Qi et al. 2024, IBM Cost of a Data Breach 2025 y el comunicado de Gartner) fueron contrastadas directamente contra sus fuentes primarias y se confirmaron como exactas. El propio informe señala con transparencia sus cifras de menor respaldo, como el 69% atribuido a Gartner sobre uso de GenAI no autorizada, verificado solo a través de una fuente secundaria.

**¿En qué empresas y sistemas reales se basó cada problema?**

Cada uno de los siete problemas está respaldado por una muestra concreta de organizaciones o sistemas atacados/encuestados, no por una afirmación genérica. Esta es la base empresarial/muestral que documenta el informe completo:

| # | Problema | Empresas / sistemas base |
|---|---|---|
| 1 | Inyección de instrucciones (vector #1) | 25 GPTs de producción de **OpenAI** (dominios de ciberseguridad, derecho, finanzas, medicina y religión) + 9 LLM open-source (Llama2, Mistral, Mixtral, Vicuna, SOLAR, WizardLM, Qwen1.5, Platypus2), atacados por investigadores de **Harvard, Carnegie Mellon University y MBZUAI** (Qi et al., 2024) |
| 2 | Exfiltración escala con tamaño del corpus | Sistemas RAG construidos con la técnica **Retrieval-In-Context (RIC)**, el mismo patrón que usa LangChain4j en este proyecto; mismo estudio de Qi et al. (2024) |
| 3 | Brechas de datos ligadas a IA generativa | **600 organizaciones** con brecha de datos confirmada entre marzo de 2024 y febrero de 2025, en 16 sectores industriales y 17 países (**IBM Security / Ponemon Institute, 2025**) |
| 4 | Ausencia de gobernanza de IA | Las mismas 600 organizaciones de **IBM/Ponemon (2025)**, con cifras del mismo orden confirmadas de forma independiente por encuestas de **Salesforce** (Workforce AI Survey, 2026) y **Gartner** (AI Governance Survey, 2026) |
| 5 | Shadow AI (fuga por empleados) | **UpGuard**: 1.500 líderes de seguridad y empleados encuestados en EE. UU., Reino Unido, Canadá, Australia, Nueva Zelanda, Singapur e India (State of Shadow AI Report 2025); **Cyberhaven**: telemetría real (no encuesta) de tráfico corporativo hacia herramientas de IA generativa (AI Adoption and Risk Report 2025); **Menlo Security**: telemetría de tráfico web agregado |
| 6 | Control de acceso roto (causa raíz) | Transversal a cualquier sistema con datos de distinta sensibilidad; marco **OWASP Foundation** (OWASP Top 10:2021 y OWASP Top 10 for LLM Applications 2025, sobre bases de datos vectoriales multiusuario) y el estudio académico *"SoK: Privacy Risks and Mitigations in Retrieval-Augmented Generation Systems"* (2026) |
| 7 | Brecha adopción vs. gobernanza | Encuestas de clientes empresariales de **Gartner** y de **LayerX** (*Enterprise AI and SaaS Data Security Report 2025*, basado en telemetría real de navegador, no solo encuesta); síntesis adicional de **Vectra AI** (2026) |

Ninguna de estas organizaciones corresponde a NovaTech Andina ni a datos internos del proyecto: son las fuentes externas verificables que sustentan cada hallazgo. El detalle completo por fuente (metodología, tamaño de muestra, año, enlace y limitaciones declaradas) está en el informe: [`docs/estadisticas/RAG_Exfiltration_Riesgos_Informe.docx`](docs/estadisticas/RAG_Exfiltration_Riesgos_Informe.docx).

## 💰 Análisis de costos y riesgos económicos

Como complemento al risk register, el equipo elaboró un informe técnico que estima el costo económico de los riesgos y de cada componente de la arquitectura a lo largo de los tres avances. Sigue el enfoque de NIST SP 800-30 y calcula la pérdida anualizada esperada como `ALE = SLE × ARO`, donde SLE es el costo de un solo evento y ARO es cuántas veces se espera que ocurra por año.

> 📎 Informe completo: [`docs/cost/RAG_Exfiltration_Analisis_Costos_Riesgos.docx`](docs/cost/analisis-costos-riesgos-rag.md)

**Resumen**

El informe analiza siete riesgos (R-01, R-02, R-03, R-04, R-05, R-08 y R-09) con cifras absolutas en USD/COP, no solo con fórmulas. Modela el proyecto como si estuviera en producción en la organización ficticia que ya usan los documentos del corpus (NovaTech Andina), usando como anclas de "valor en riesgo" los propios datos internos (presupuesto de COP 4.200 M y oferta de adquisición de COP 8.500 M) y evidencia externa verificable para todo lo demás: IBM *Cost of a Data Breach Report 2025*, Qi et al. (2024), GitGuardian *State of Secrets Sprawl 2026*, precios oficiales de AWS KMS/Secrets Manager, Render y Anthropic, y sanciones reales de la Superintendencia de Industria y Comercio (SIC) por infracción a la Ley 1581 de 2012.

| Riesgo | Costo por incidente (SLE) | Pérdida anual esperada (ALE) | Costo de mitigación | Riesgo residual/año |
|---|---|---|---|---|
| R-01 — Consulta directa | USD 12.119 | USD 18.179 | USD 1.036 | USD 2.727 |
| R-02 — Reformulación evasiva | USD 12.464 | USD 18.697 | USD 259 | USD 2.805 |
| R-03 — Inyección de instrucciones | USD 4.500 | USD 3.375 | USD 878 | USD 1.013 |
| R-04 — Envenenamiento del vector store | USD 864 | USD 216 | USD 345 | USD 86 |
| R-05 — Inversión de embeddings | USD 1.037 | USD 259 | USD 357 | USD 104 |
| R-08 — Exposición de credenciales | USD 283 | USD 42 | USD 110 | USD 4 |
| R-09 — Logs con fragmentos sensibles | USD 6.059 | USD 4.545 | USD 345 | USD 1.364 |
| **Total** | — | **USD 45.313/año** | **USD 3.330** | **USD 8.103/año** |

1. **R-01 y R-02 concentran casi el 82% de la pérdida anual esperada**, porque ambos comparten el mismo activo de máximo impacto: el documento SECRETO del plan de adquisición (COP 8.500 M). El "escenario alto" de ambos supera los USD 293.000 si la fuga ocurre antes del cierre de la negociación.
2. **El filtrado por rol/clasificación del Avance 2 es, con mucha diferencia, el control de mayor retorno.** Cierra R-01 y R-02 con un costo conjunto de ≈USD 950 y evita ≈USD 31.000/año en pérdida esperada combinada (ROI de cuatro cifras).
3. **R-04, R-05 y R-08 dan ROI negativo sobre el promedio, pero no por eso son prescindibles.** Su ARO anual es bajo, pero su "escenario alto" sigue valiendo decenas o cientos de miles de dólares — es la firma típica de un riesgo de baja frecuencia y alto impacto; el criterio correcto no es el ROI esperado sino comparar el costo de mitigar (USD 110–357) contra la pérdida máxima razonable.
4. **El Avance 3 (validación de la respuesta) reduce R-03 solo de forma probabilística.** En el estudio de Qi et al. (2024) las mitigaciones más efectivas bajan la reconstrucción de 88,9% a 52,3%, por lo que su riesgo residual anual (USD 1.013) nunca llega a cero en el modelo.
5. **Ollama local evita el costo por token**, pero la validación en dos pasos del Avance 3 implica un sobrecosto de cómputo estimado en ≈USD 360/año (upgrade de plan de hosting).

**Supuestos clave declarados** (ver el detalle completo con fórmulas en el informe): tasa de cambio COP 3.150/USD; tarifa de ingeniería cargada COP 68.000/hora (Coderhouse, 2026, con recargo prestacional del 50%); organización modelada de ~300 empleados; costo por registro de dato personal comprometido USD 160 (IBM 2025); probabilidad de sanción SIC del 15% tras un incidente confirmado, con monto tomado de la mediana de sanciones reales (COP 240 M, rango COP 83 M–496 M).

**Limitaciones declaradas:** el análisis se hizo con los siete riesgos descritos en este README y en `risk-register.md`. El tamaño de la organización (300 empleados) y la probabilidad de sanción SIC (15%) son supuestos explícitos del equipo, no cifras publicadas, y deben ajustarse si se cuenta con datos internos reales. Los precios de proveedores (AWS, Render, Anthropic) corresponden a septiembre de 2026 y deben reverificarse antes de usarse en un documento final. **Nota:** esta tabla resume el cálculo cuantificado más reciente del equipo; el archivo `docs/cost/RAG_Exfiltration_Analisis_Costos_Riesgos.docx` enlazado arriba aún corresponde a una versión previa, únicamente con fórmulas (sin cifras absolutas), y está pendiente de actualizarse para que coincida con esta tabla.

## 📚 Referencias

Ver la lista completa de referencias en el documento del marco teórico: [`docs/estadisticas/RAG_Exfiltration_Riesgos_Informe.docx`](docs/estadisticas/RAG_Exfiltration_Riesgos_Informe.docx).

## ✅ Conclusiones

> 🚧 Sección en construcción. Se irá completando al cierre de cada avance, con los hallazgos acumulados del proyecto.

---

<div align="center">

*Proyecto académico. Fundamentos de Seguridad de la Información. Septiembre 2026*

</div>