# 📋 Risk Register. Exfiltración de información mediante RAG

Este documento es el **registro central de riesgos** del proyecto. Identifica, evalúa y asigna responsables a los riesgos de seguridad detectados en el sistema RAG, y se actualiza en cada avance A, B y C a medida que se implementan nuevos controles.

Los conceptos aplicados aquí, como amenaza, vulnerabilidad, riesgo, apetito y tolerancia, riesgo inherente y residual, y tratamiento del riesgo, siguen el marco visto en el curso y se alinean con ISO/IEC 27001 y con el enfoque de gestión de riesgos basado en NIST CSF 2.0.

## 🧭 Definiciones rápidas

- **Riesgo inherente:** el riesgo tal como existe *antes* de aplicar controles, que es la situación del Avance 1 con el sistema sin filtrado.
- **Riesgo residual:** el riesgo que permanece *después* de aplicar controles, en los Avances 2 y 3.
- **Apetito de riesgo del proyecto:** el equipo no acepta que un usuario autenticado sin permisos reciba contenido de nivel *confidencial* o *secreto*, bajo ningún escenario de prueba.
- **Tolerancia:** se acepta un margen residual de riesgo frente a ataques avanzados combinados, descritos en las secciones 6.1 y 6.3 del marco teórico, mientras se documenten y se planeen controles adicionales para el Avance 3.

## 📊 Tabla de riesgos

| ID | Activo afectado | Amenaza | Vulnerabilidad explotada | Probabilidad | Impacto | Nivel de riesgo | Estrategia de tratamiento | Fase que lo atiende | Riesgo residual esperado | Responsable |
|----|---|---|---|---|---|---|---|---|---|---|
| R-01 | Documentos confidenciales y secretos del corpus | Consulta directa a documentos restringidos | Ausencia de autorización a nivel de documento en la recuperación | Alta | Alto | 🔴 Crítico | **Mitigar.** Filtrado en la recuperación por usuario, rol y clasificación | B | Bajo | Backend y RAG |
| R-02 | Documentos confidenciales y secretos del corpus | Reformulación evasiva mediante resumen o paráfrasis para evadir filtros por palabra clave | Filtrado basado sólo en similitud semántica, sin control de metadatos | Alta | Alto | 🔴 Crítico | **Mitigar.** Filtrado por metadatos de clasificación antes de llegar al modelo | B | Bajo | Backend y RAG |
| R-03 | Contexto ya entregado al modelo en la conversación | Inyección de instrucciones o prompt injection para ignorar restricciones configuradas | El modelo obedece instrucciones incrustadas en la consulta o en documentos recuperados | Media | Alto | 🟠 Alto | **Mitigar.** Refuerzo del mensaje del sistema más validación de la respuesta | C | Medio, riesgo residual reconocido en la literatura | Backend y prompt engineering |
| R-04 | Almacén vectorial de embeddings | Envenenamiento del almacén vectorial, conocido como PoisonedRAG | Ingesta de documentos sin verificación de integridad ni de origen | Baja | Alto | 🟠 Alto | **Mitigar.** Validación de fuentes en la ingesta y monitoreo de nuevos documentos | C | Medio | Backend e ingesta |
| R-05 | Representaciones vectoriales o embeddings | Inversión de incrustaciones para reconstruir el texto original | Almacén vectorial sin cifrado en reposo ni control de acceso propio | Baja | Alto | 🟡 Medio | **Mitigar.** Cifrado en reposo del vector store y control de acceso a la infraestructura | C | Medio | Infraestructura |
| R-06 | Permisos de usuario | Permisos desactualizados cuando un usuario cambia de rol y el sistema no lo refleja | Falta de sincronización entre roles organizacionales y roles del sistema RAG | Media | Medio | 🟡 Medio | **Mitigar.** Revisión periódica de roles y expiración de sesión y permisos | B | Bajo | Backend y Auth |
| R-07 | Disponibilidad y usabilidad del asistente | Filtrado demasiado estricto que genera falsos rechazos a usuarios legítimos | Clasificación de documentos mal etiquetada o incompleta | Media | Bajo | 🟡 Medio | **Aceptar.** Documentar como limitación conocida y revisar el etiquetado del corpus | B | Bajo | Documentación y corpus |
| R-08 | Credenciales y API keys del LLM externo | Exposición de credenciales en el repositorio o en el cliente | Uso de variables de entorno mal gestionadas o credenciales escritas en el código | Baja | Alto | 🟡 Medio | **Evitar.** Variables de entorno más `.gitignore`, sin exponer nunca claves en el frontend | A | Bajo | Todo el equipo |
| R-09 | Logs de consultas | Exposición de fragmentos sensibles dentro de los propios logs de auditoría | Logs sin control de acceso ni retención definida | Media | Medio | 🟡 Medio | **Mitigar.** Control de acceso a logs y retención limitada | C | Bajo | Infraestructura |

**Leyenda de nivel de riesgo:** 🔴 Crítico · 🟠 Alto · 🟡 Medio · 🟢 Bajo

## 🗺️ Matriz de riesgos cualitativa

| Probabilidad \ Impacto | Bajo | Medio | Alto |
|---|---|---|---|
| **Alta** | 🟡 Medio | 🟠 Alto | 🔴 Crítico: R-01, R-02 |
| **Media** | 🟢 Bajo | 🟡 Medio: R-06, R-09 | 🟠 Alto: R-03 |
| **Baja** | 🟢 Bajo | 🟡 Medio: R-05, R-08 | 🟠 Alto: R-04 |

## 🛠️ Estrategias de tratamiento aplicadas

Siguiendo las 4 estrategias clásicas de tratamiento del riesgo:

- **Evitar:** eliminar directamente la actividad que genera el riesgo, por ejemplo nunca exponer API keys en el cliente.
- **Mitigar:** reducir probabilidad o impacto mediante controles técnicos como el filtrado por rol y clasificación, la validación de la respuesta o el cifrado en reposo. Es la estrategia predominante en este proyecto.
- **Aceptar:** asumir conscientemente un riesgo de bajo impacto y documentarlo, como los falsos rechazos por mala clasificación de documentos.
- **Transferir:** no aplica de forma directa en este proyecto académico, ya que no se contratan terceros para operar el sistema en producción.

## 🔁 Ciclo de actualización

Este registro se revisa y actualiza al final de cada fase:

- **Fase A, Avance 1:** riesgos identificados sobre el sistema *sin filtrado*, es decir el riesgo inherente.
- **Fase B, Avance 2:** se documenta la reducción de riesgo lograda con el filtrado por rol y clasificación.
- **Fase C, Avance 3:** se documenta el riesgo residual tras el refuerzo de prompt y la validación de respuesta, y se decide si algún riesgo pasa de mitigar a aceptar.

Las evidencias que sustentan cada actualización se guardan en [`evidencias/`](evidencias/), organizadas por fase.
