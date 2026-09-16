# 📋 Risk Register — Exfiltración de información mediante RAG

Este documento es el **registro central de riesgos** del proyecto: identifica, evalúa y asigna responsables a los riesgos de seguridad detectados en el sistema RAG, y se actualiza en cada avance (A, B, C) a medida que se implementan nuevos controles.

Los conceptos aplicados aquí (amenaza, vulnerabilidad, riesgo, apetito/tolerancia, riesgo inherente/residual, tratamiento del riesgo) siguen el marco visto en el curso y se alinean con ISO/IEC 27001 y el enfoque de gestión de riesgos basado en NIST CSF 2.0.

## 🧭 Definiciones rápidas

- **Riesgo inherente:** el riesgo tal como existe *antes* de aplicar controles (situación del Avance 1, sistema sin filtrado).
- **Riesgo residual:** el riesgo que permanece *después* de aplicar controles (Avances 2 y 3).
- **Apetito de riesgo del proyecto:** el equipo no acepta que un usuario autenticado sin permisos reciba contenido de nivel *confidencial* o *secreto*, bajo ningún escenario de prueba.
- **Tolerancia:** se acepta un margen residual de riesgo frente a ataques avanzados combinados (ver sección 6.1 y 6.3 del marco teórico) mientras se documenten y se planeen controles adicionales para el Avance 3.

## 📊 Tabla de riesgos

| ID | Activo afectado | Amenaza | Vulnerabilidad explotada | Probabilidad | Impacto | Nivel de riesgo | Estrategia de tratamiento | Fase que lo atiende | Riesgo residual esperado | Responsable |
|----|---|---|---|---|---|---|---|---|---|---|
| R-01 | Documentos confidenciales/secretos del corpus | Consulta directa a documentos restringidos | Ausencia de autorización a nivel de documento en la recuperación | Alta | Alto | 🔴 Crítico | **Mitigar** — filtrado en la recuperación por usuario/rol/clasificación | B | Bajo | Backend / RAG |
| R-02 | Documentos confidenciales/secretos del corpus | Reformulación evasiva (resumen/paráfrasis para evadir filtros por palabra clave) | Filtrado basado solo en similitud semántica, sin control de metadatos | Alta | Alto | 🔴 Crítico | **Mitigar** — filtrado por metadatos de clasificación antes de llegar al modelo | B | Bajo | Backend / RAG |
| R-03 | Contexto ya entregado al modelo en la conversación | Inyección de instrucciones (prompt injection) para ignorar restricciones configuradas | El modelo obedece instrucciones incrustadas en la consulta o en documentos recuperados | Media | Alto | 🟠 Alto | **Mitigar** — refuerzo del mensaje del sistema + validación de la respuesta | C | Medio (riesgo residual reconocido en la literatura) | Backend / Prompt engineering |
| R-04 | Almacén vectorial (embeddings) | Envenenamiento del almacén vectorial (PoisonedRAG) | Ingesta de documentos sin verificación de integridad/origen | Baja | Alto | 🟠 Alto | **Mitigar** — validación de fuentes en la ingesta; monitoreo de nuevos documentos | C | Medio | Backend / Ingesta |
| R-05 | Representaciones vectoriales (embeddings) | Inversión de incrustaciones para reconstruir el texto original | Almacén vectorial sin cifrado en reposo ni control de acceso propio | Baja | Alto | 🟡 Medio | **Mitigar** — cifrado en reposo del vector store + control de acceso a la infraestructura | C | Medio | Infraestructura |
| R-06 | Permisos de usuario | Permisos desactualizados (usuario cambia de rol y el sistema no lo refleja) | Falta de sincronización entre roles organizacionales y roles del sistema RAG | Media | Medio | 🟡 Medio | **Mitigar** — revisión periódica de roles; expiración de sesión/permisos | B | Bajo | Backend / Auth |
| R-07 | Disponibilidad y usabilidad del asistente | Filtrado demasiado estricto genera falsos rechazos a usuarios legítimos | Clasificación de documentos mal etiquetada o incompleta | Media | Bajo | 🟡 Medio | **Aceptar** — documentar como limitación conocida; revisar etiquetado del corpus | B | Bajo | Documentación / Corpus |
| R-08 | Credenciales / API keys del LLM externo | Exposición de credenciales en el repositorio o en el cliente | Uso de variables de entorno mal gestionadas o hardcoded | Baja | Alto | 🟡 Medio | **Evitar** — variables de entorno + `.gitignore`, nunca exponer claves en frontend | A | Bajo | Todo el equipo |
| R-09 | Logs de consultas | Exposición de fragmentos sensibles dentro de los propios logs de auditoría | Logs sin control de acceso ni retención definida | Media | Medio | 🟡 Medio | **Mitigar** — control de acceso a logs, retención limitada | C | Bajo | Infraestructura |

**Leyenda de nivel de riesgo:** 🔴 Crítico · 🟠 Alto · 🟡 Medio · 🟢 Bajo

## 🗺️ Matriz de riesgos (cualitativa)

| Probabilidad \ Impacto | Bajo | Medio | Alto |
|---|---|---|---|
| **Alta** | 🟡 Medio | 🟠 Alto | 🔴 Crítico (R-01, R-02) |
| **Media** | 🟢 Bajo | 🟡 Medio (R-06, R-09) | 🟠 Alto (R-03) |
| **Baja** | 🟢 Bajo | 🟡 Medio (R-05, R-08) | 🟠 Alto (R-04) |

## 🛠️ Estrategias de tratamiento aplicadas

Siguiendo las 4 estrategias clásicas de tratamiento del riesgo:

- **Evitar:** eliminar directamente la actividad que genera el riesgo (p. ej. nunca exponer API keys en el cliente).
- **Mitigar:** reducir probabilidad y/o impacto mediante controles técnicos (filtrado por rol/clasificación, validación de respuesta, cifrado en reposo). Es la estrategia predominante en este proyecto.
- **Aceptar:** asumir conscientemente un riesgo de bajo impacto y documentarlo (p. ej. falsos rechazos por mala clasificación de documentos).
- **Transferir:** no aplica de forma directa en este proyecto académico, ya que no se contratan terceros para operar el sistema en producción.

## 🔁 Ciclo de actualización

Este registro se revisa y actualiza al final de cada fase:

- **Fase A (Avance 1):** riesgos identificados sobre el sistema *sin filtrado* (riesgo inherente).
- **Fase B (Avance 2):** se documenta la reducción de riesgo lograda con el filtrado por rol/clasificación.
- **Fase C (Avance 3):** se documenta el riesgo residual tras refuerzo de prompt y validación de respuesta, y se decide si algún riesgo pasa de "mitigar" a "aceptar".

Las evidencias que sustentan cada actualización se guardan en [`evidencias/`](evidencias/), organizadas por fase.
