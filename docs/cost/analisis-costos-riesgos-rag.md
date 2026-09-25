# Análisis económico de riesgos — R-01, R-02, R-03, R-04, R-05, R-08, R-09
### Proyecto: Exfiltración de información mediante RAG (rag-exfiltration-backend)

---

## 0. Supuestos base y método

El repositorio es un prototipo académico con un corpus sintético de 5 documentos. Para producir cifras monetarias defendibles (como exige el ejercicio) en lugar de una metodología abstracta, este informe modela el escenario **como si el sistema estuviera en producción** en la organización ficticia que ya usan los propios documentos del corpus — **Syntrix** — manteniendo los valores internos reales que el equipo ya definió (presupuesto de COP 4.200 M y oferta de adquisición de COP 8.500 M) como anclas de "valor en riesgo", y usando evidencia externa verificable para todo lo demás. Se usa el enfoque SLE/ARO/ALE (NIST SP 800-30), ya adoptado por el propio equipo en `docs/cost`.

**Supuestos numerados (todos declarados explícitamente; se reutilizan en todos los riesgos):**

| # | Supuesto | Valor | Base |
|---|---|---|---|
| A1 | Tasa de cambio | 1 USD ≈ COP 3.150 | Promedio reciente USD/COP, ago. 2026 (CurrencyBeacon) |
| A2 | Tarifa de ingeniería cargada (backend/seguridad, Colombia, semi-senior) | COP 68.000/hora ≈ USD 21,6/hora | Coderhouse (2026): COP 6.000.000–8.500.000/mes → punto medio COP 7.250.000; + 50% de recargo prestacional (cesantías, prima, seguridad social, parafiscales — práctica estándar de costeo laboral en Colombia); 160 h/mes |
| A3 | Tamaño de la organización modelada | ~300 empleados | Supuesto — perfil de PYME/empresa mediana coherente con el proyecto |
| A4 | Costo por registro de dato personal comprometido | USD 160 ≈ COP 504.000 | IBM *Cost of a Data Breach Report 2025* (promedio global, costo por registro) |
| A5 | Probabilidad de sanción formal de la SIC tras un incidente confirmado de fuga de datos personales | 15% | Supuesto conservador (la mayoría de incidentes internos no se denuncian formalmente) |
| A6 | Monto de sanción SIC (Ley 1581/2012, Habeas Data) | Mediana COP 240 M (rango COP 83 M–496 M) | Casos reales verificables 2019–2026 (ver §7) |
| A7 | Traducción de probabilidad cualitativa del risk-register a ARO anual | Alta ≈ 1,5/año · Media ≈ 0,75/año · Baja ≈ 0,25/año | Convención estándar de cuantificación de riesgo (NIST SP 800-30 / ISO 27005) |

---

## 1. Tabla principal (escenario base)

| Riesgo | Evento de pérdida | SLE base (USD) | ARO | ALE anual (USD) | Costo de mitigación | Riesgo residual (USD/año) | Fuentes clave |
|---|---|---|---|---|---|---|---|
| R-01 | Consulta directa expone doc. confidencial/secreto | $12.119 | 1,5 | **$18.179** | $1.036 (única vez + año 1) | $2.727 | IBM 2025; Superfinanciera; corpus interno |
| R-02 | Reformulación evasiva logra el mismo acceso sin disparar filtros | $12.464 | 1,5 | **$18.697** | $259* (marginal, comparte control con R-01) | $2.805 | Ídem + Qi et al. 2024 |
| R-03 | Inyección de instrucciones extrae el documento SECRETO (escenario de la demo `03-inyeccion-instrucciones.sh`) | $4.500 | 0,75 | **$3.375** | $878 | $1.013 | Qi et al. 2024 (arXiv:2402.17840) |
| R-04 | Envenenamiento del vector store (PoisonedRAG) | $864 | 0,25 | **$216** | $345 | $86 | Marco teórico del proyecto; OWASP LLM Top 10 |
| R-05 | Inversión de embeddings reconstruye el corpus | $1.037 | 0,25 | **$259** | $357 (incl. AWS KMS) | $104 | AWS KMS pricing; OWASP LLM08 |
| R-08 | Credenciales/API key expuestas en repo o cliente | $283 | 0,15 | **$42** | $110 | $4 | GitGuardian *State of Secrets Sprawl 2026*; TruffleSecurity 2026 |
| R-09 | Fragmentos sensibles expuestos vía logs de auditoría | $6.059 | 0,75 | **$4.545** | $345 | $1.364 | Deriva de A4–A6 |
| **Total** | | | | **$45.313/año** | **$3.330** (+ recurrentes) | **$8.103/año** | |

\* El control de filtrado por rol/clasificación (Avance 2) mitiga R-01 y R-02 con el mismo desarrollo; el costo de R-02 es el incremento marginal (pruebas de evasión adicionales).

---

## 2. Detalle de cálculo por riesgo

### R-01 — Consulta directa a documentos restringidos
**Vulnerabilidad:** `NaiveRagService` recupera de todo el corpus sin mirar rol ni clasificación (confirmado en el código, Avance 1).

- **Escenario bajo** — se expone el presupuesto financiero (COP 4.200 M, sin datos personales), detectado y contenido rápido:
  `Costo = detección (8 h) + contención (8 h) = 16 h × COP 68.000 = COP 1.088.000 ≈ USD 345`
- **Escenario base** — se expone el resumen de evaluaciones de RRHH (datos agregados + riesgo de reidentificación de los 3 casos individuales mencionados en el documento), sin sanción SIC confirmada pero con probabilidad de investigación:
  `Costo = detección+contención (32 h × 68.000 = 2.176.000) + riesgo legal esperado (A5×A6 = 0,15 × 240.000.000 = 36.000.000) = COP 38.176.000 ≈ USD 12.119`
- **Escenario alto** — se expone el plan de adquisición SECRETO (COP 8.500 M) antes del cierre de la negociación. Una fuga prematura de una operación de M&A típicamente encarece la transacción al alertar a otros interesados o forzar una renegociación; se asume conservadoramente un sobreprecio del 5% sobre el valor de la oferta (supuesto explícito, ante ausencia de un estudio específico para adquisiciones privadas en Colombia):
  `Costo = sobreprecio (5% × 8.500.000.000 = 425.000.000) + investigación forense ampliada (40 h × 68.000 = 2.720.000) + sanción SIC en el percentil alto real (496.000.000) = COP 923.720.000 ≈ USD 293.244`

`ALE = 12.119 × 1,5 = USD 18.179/año`

### R-02 — Reformulación evasiva (paráfrasis/resumen)
Mismo tipo de exposición que R-01, pero la ausencia de control por metadatos obliga a más horas de investigación forense porque el ataque evade el filtrado semántico básico (+16 h respecto a R-01 en el escenario base y alto).

`SLE base = 2.176.000 + 1.088.000 (investigación extra) + 36.000.000 = COP 39.264.000 ≈ USD 12.464`
`ALE = 12.464 × 1,5 = USD 18.697/año`

### R-03 — Inyección de instrucciones
Es el ataque documentado literalmente en `ataques-demo/03-inyeccion-instrucciones.sh`: un usuario de RRHH intenta, mediante una orden embebida en el mensaje, que el modelo ignore restricciones y entregue el documento SECRETO.

- **Bajo** (intento fallido, pero requiere revisión): `8 h × 68.000 = COP 544.000 ≈ USD 173`
- **Base** (fuga parcial de un fragmento del documento SECRETO): `32 h × 68.000 = 2.176.000 + riesgo legal reducido (0,05 × 240.000.000 = 12.000.000) = COP 14.176.000 ≈ USD 4.500`
- **Alto** (éxito total, replica el escenario alto de R-01 + sobrecosto forense de prompt injection): `923.720.000 + 1.088.000 = COP 924.808.000 ≈ USD 293.590`

`ALE = 4.500 × 0,75 = USD 3.375/año`

Este es el riesgo que Qi et al. (2024, *Follow My Instruction and Spill the Beans*, arXiv:2402.17840) cuantifican empíricamente: extracción del datastore con éxito del 100% en 25 GPTs personalizados con máximo 2 consultas, y hasta 41% de un corpus de 77.000 palabras reconstruido verbatim con 100 consultas — evidencia externa directa de que la probabilidad "Media/Alta" que el equipo asignó a R-03 es razonable.

### R-04 — Envenenamiento del vector store (PoisonedRAG)
No existe validación de integridad ni de origen en la ingesta. El costo dominante es la reconstrucción del índice y la auditoría de fuentes, no una pérdida directa de datos:

- **Bajo:** `24 h × 68.000 = COP 1.632.000 ≈ USD 518`
- **Base:** reingesta completa + auditoría de fuentes (`40 h × 68.000 = COP 2.720.000 ≈ USD 864`). El costo de decisiones de negocio basadas en respuestas envenenadas no se cuantifica por falta de un benchmark externo confiable aplicable a este tamaño de organización (limitación declarada).
- **Alto:** reconstrucción del pipeline + validación de integridad ampliada (`120 h × 68.000 = COP 8.160.000 ≈ USD 2.590`)

`ALE = 864 × 0,25 = USD 216/año`

### R-05 — Inversión de embeddings
El almacén vectorial no tiene cifrado en reposo ni control de acceso propio; si se compromete la infraestructura, el corpus completo queda en riesgo de reconstrucción.

- **Bajo:** auditoría de acceso a infraestructura (`16 h × 68.000 = COP 1.088.000 ≈ USD 345`)
- **Base:** cifrado de emergencia + re-vectorización con nuevas claves (`48 h × 68.000 = COP 3.264.000 ≈ USD 1.037`)
- **Alto:** ídem + exposición del documento SECRETO por reconstrucción + sanción SIC: `3.264.000 + 496.000.000 = COP 499.264.000 ≈ USD 158.497`

`ALE = 1.037 × 0,25 = USD 259/año`

### R-08 — Exposición de credenciales/API keys
Ya mitigado parcialmente en el Avance 1 (Fase A: variables de entorno + `.gitignore`). Se modela el riesgo estructural residual usando datos de la industria:

GitGuardian, *State of Secrets Sprawl 2026*: 28,65 millones de secretos nuevos expuestos en GitHub público en 2025 (+34% interanual); las fugas de secretos de servicios de IA crecieron 81% interanual. TruffleSecurity (2026) reporta un tiempo mediano de revocación de 13 días para secretos filtrados.

- **Base:** se asume que una key quedara expuesta y fuera usada de forma indebida durante la ventana mediana de 13 días antes de su revocación, generando ~50 millones de tokens de consumo fraudulento a la tarifa de un modelo económico de referencia (Claude Haiku 3.5, USD 0,80/USD 4,00 por millón de tokens de entrada/salida — docs.anthropic.com): `(35M×0,80 + 15M×4,00)/1.000.000 = USD 88` + horas de rotación/auditoría (`8h×68.000=544.000≈USD173`) = **USD 283**

`ALE = 283 × 0,15 = USD 42/año`

### R-09 — Exposición de fragmentos sensibles en logs
Los logs no tienen control de acceso ni retención definida, por lo que duplican, por un canal secundario, la misma exposición que R-01/R-02.

- **Base:** `16 h × 68.000 = 1.088.000 + riesgo legal reducido a la mitad por ser un canal interno (0,075 × 240.000.000 = 18.000.000) = COP 19.088.000 ≈ USD 6.059`

`ALE = 6.059 × 0,75 = USD 4.545/año`

---

## 3. Rangos de incertidumbre (SLE por escenario, USD)

| Riesgo | Escenario bajo | Escenario base | Escenario alto |
|---|---|---|---|
| R-01 | $345 | $12.119 | $293.244 |
| R-02 | $518 | $12.464 | $293.590 |
| R-03 | $173 | $4.500 | $293.590 |
| R-04 | $518 | $864 | $2.590 |
| R-05 | $345 | $1.037 | $158.497 |
| R-08 | $43 | $283 | $623 |
| R-09 | $173 | $6.059 | $293.244 |

El "escenario alto" de R-01, R-02, R-03, R-05 y R-09 converge alrededor de ~USD 293.000 porque todos representan, en el límite, la misma consecuencia: fuga completa del documento SECRETO (plan de adquisición, COP 8.500 M) más una sanción SIC en el percentil alto observado. Esto es coherente con el mapa de riesgo cualitativo del equipo, donde R-01 y R-02 son "Crítico" precisamente por compartir el mismo activo de máximo impacto.

---

## 4. Costos de mitigación

| Riesgo(s) | Medida | Desarrollo (única vez) | Costo recurrente anual | Fuente de precios |
|---|---|---|---|---|
| R-01, R-02 | Filtrado en la recuperación por rol/clasificación (columnas de metadatos + predicado SQL en la consulta a pgvector) | 32 h × 68.000 = COP 2.176.000 ≈ **USD 691** | COP 544.000 ≈ **USD 173** (mantenimiento) | Tarifa A2 |
| R-03 | Refuerzo del system prompt + validación de la respuesta (segunda pasada) | 24 h × 68.000 = COP 1.632.000 ≈ **USD 518** | Cómputo adicional: upgrade Render Standard→Pro (+USD 30/mes ≈ **USD 360/año**) | render.com/pricing (2026) |
| R-04 | Validación de integridad/origen en la ingesta | 16 h × 68.000 = COP 1.088.000 ≈ **USD 345** | — | Tarifa A2 |
| R-05 | Cifrado en reposo del vector store | 16 h × 68.000 = COP 1.088.000 ≈ **USD 345** | AWS KMS: 1 clave × USD 1/mes = **USD 12/año** | aws.amazon.com/kms/pricing |
| R-08 | Migración de `.env` a gestor de secretos administrado | 4 h × 68.000 = COP 272.000 ≈ **USD 86** | AWS Secrets Manager: 5 secretos × USD 0,40/mes = **USD 24/año** | costbench.com (AWS Secrets Manager, 2026) |
| R-09 | Control de acceso a logs + retención limitada (30–90 días) | 16 h × 68.000 = COP 1.088.000 ≈ **USD 345** | — | Tarifa A2 |
| **Total** | | **≈ USD 2.330** | **≈ USD 569/año** | |

Nota: R-01 y R-02 comparten el mismo control técnico (filtrado por metadatos); en la tabla del §1 se reparte el costo total entre ambos para evitar doble conteo.

---

## 5. Comparación costo-beneficio y ROI

| Riesgo | ALE sin mitigar | Costo de mitigación (año 1) | ALE residual | Beneficio (ALE antes − después) | ROI |
|---|---|---|---|---|---|
| R-01 | $18.179 | $1.036 | $2.727 | $15.452 | **1.291%** |
| R-02 | $18.697 | $259 (marginal) | $2.805 | $15.893 | **6.034%** |
| R-03 | $3.375 | $878 | $1.013 | $2.363 | **169%** |
| R-04 | $216 | $345 | $86 | $130 | **-62%** |
| R-05 | $259 | $357 | $104 | $155 | **-57%** |
| R-08 | $42 | $110 | $4 | $38 | **-65%** |
| R-09 | $4.545 | $345 | $1.364 | $3.181 | **821%** |

**Lectura de los ROI negativos (R-04, R-05, R-08):** el promedio esperado (ALE) es negativo porque la probabilidad anual (ARO) es baja, pero el "escenario alto" de cada uno sigue siendo de decenas a cientos de miles de dólares (§3). Un ROI negativo sobre el promedio no significa que la mitigación sea injustificada: es la firma típica de un riesgo de cola larga (baja frecuencia, alto impacto), donde el criterio correcto no es el ROI esperado sino el costo de mitigación frente a la pérdida máxima razonable — en los tres casos, el costo de mitigación (USD 110–357) es entre 3 y 700 veces menor que el escenario alto correspondiente (USD 623–158.497).

**Riesgos con ROI claramente positivo (R-01, R-02, R-03, R-09):** todos comparten o dependen del control de filtrado por rol/clasificación del Avance 2, que resulta ser, con mucha diferencia, la inversión de mayor retorno del proyecto.

---

## 6. Tabla resumen final

| Riesgo | Costo por incidente (SLE base) | Pérdida anual esperada (ALE) | Costo de mitigación | Pérdida residual | Beneficio esperado de la mitigación |
|---|---|---|---|---|---|
| R-01 | $12.119 | $18.179 | $1.036 | $2.727 | $15.452 |
| R-02 | $12.464 | $18.697 | $259 | $2.805 | $15.893 |
| R-03 | $4.500 | $3.375 | $878 | $1.013 | $2.363 |
| R-04 | $864 | $216 | $345 | $86 | $130 |
| R-05 | $1.037 | $259 | $357 | $104 | $155 |
| R-08 | $283 | $42 | $110 | $4 | $38 |
| R-09 | $6.059 | $4.545 | $345 | $1.364 | $3.181 |
| **Total** | — | **$45.313** | **$3.330** | **$8.103** | **$37.210** |

---

## 7. Fuentes y trazabilidad de los costos

| # | Fuente | Organización/autor | Año | URL | Dato utilizado |
|---|---|---|---|---|---|
| 1 | *Cost of a Data Breach Report 2025* | IBM / Ponemon Institute | 2025 | ibm.com/think/x-force/2025-cost-of-a-data-breach-navigating-ai | Costo promedio por registro comprometido ≈ USD 160 (A4); 97% de brechas relacionadas con IA carecían de controles de acceso adecuados (contexto de R-01/R-02) |
| 2 | *Follow My Instruction and Spill the Beans: Scalable Data Extraction from RAG Systems* | Qi, Zhang, Xing, Kakade, Lakkaraju — Harvard/CMU/MBZUAI | 2024 | arxiv.org/abs/2402.17840 | Extracción del datastore con 100% de éxito en 25 GPTs con ≤2 consultas; 41% del corpus reconstruido verbatim con 100 consultas (R-03) |
| 3 | Sueldo de un Desarrollador Backend en Colombia en 2026 | Coderhouse | 2026 | coderhouse.com/co/sueldos/sueldo-desarrollador-backend-colombia-2025 | Banda salarial semi-senior COP 6.000.000–8.500.000/mes (A2) |
| 4 | *The State of Secrets Sprawl 2026* | GitGuardian | 2026 | blog.gitguardian.com (vía dev.to/gitguardian) | 28,65 M secretos nuevos en GitHub público en 2025 (+34% interanual); +81% en secretos de servicios de IA (R-08) |
| 5 | *State of Secret Sprawl 2026* | TruffleSecurity | 2026 | trufflesecurity.com/research | Tiempo mediano de revocación de un secreto filtrado: 13 días (R-08) |
| 6 | AWS Key Management Service — Pricing | Amazon Web Services | 2026 | aws.amazon.com/kms/pricing | USD 1/mes por clave administrada por el cliente (mitigación R-05) |
| 7 | AWS Secrets Manager — Pricing | Amazon Web Services (vía CostBench) | 2026 | costbench.com/software/secrets-management/aws-secrets-manager | USD 0,40/secreto/mes (mitigación R-08) |
| 8 | Render — Pricing | Render | 2026 | render.com/pricing | Planes Standard (USD 25/mes) y Pro (USD 85/mes) — usados para estimar el sobrecosto de cómputo de la validación en dos pasos de R-03 |
| 9 | Anthropic API — Pricing | Anthropic (docs.anthropic.com) | 2026 | docs.anthropic.com/en/docs/about-claude/pricing | Claude Haiku 3.5: USD 0,80 / USD 4,00 por millón de tokens (entrada/salida) — usado como tarifa de referencia para estimar el costo de abuso de una API key filtrada (R-08) |
| 10 | Certificado TRM / USD-COP | Superintendencia Financiera de Colombia / CurrencyBeacon | 2026 | superfinanciera.gov.co · currencybeacon.com/rates/USD/COP | Tasa de cambio de referencia ≈ COP 3.150/USD (A1) |
| 11 | Sanciones de la SIC por infracción a la Ley 1581 de 2012 (Habeas Data) — casos: Cámaras de Comercio de Villavicencio/Cúcuta/Montería (COP 225 M), Mercado Libre Colombia (COP 214 M), Colombia Telecomunicaciones/Movistar (COP 263,3 M), Banco Davivienda (COP 268 M), Banco Falabella (COP 496 M) | Superintendencia de Industria y Comercio / La República — Asuntos Legales | 2019–2026 | asuntoslegales.com.co; larepublica.co; telemedellin.tv | Rango y mediana de sanciones reales usados en A6 |
| 12 | Documentos internos del corpus del proyecto (`presupuesto-finanzas-2026.md`, `plan-adquisicion-startup.md`) | Equipo del proyecto (datos ficticios) | 2026 | Repositorio, `corpus/confidencial/` y `corpus/secreto/` | Valores de "activo en riesgo": presupuesto COP 4.200 M, oferta de adquisición COP 8.500 M |

---

## 8. Limitaciones declaradas

- El corpus real del repositorio tiene solo 5 documentos sintéticos; las cifras de este informe modelan un despliegue productivo hipotético (A3) sobre esa misma organización ficticia, no el estado literal del prototipo académico.
- No se cuantificó el costo reputacional/pérdida de negocio de mediano plazo (más allá del sobreprecio de M&A en el escenario alto de R-01/R-03) por no existir un benchmark de "pérdida de negocio" aplicable a una organización de este tamaño en Colombia con la misma solidez que las demás fuentes citadas.
- La probabilidad de sanción SIC (A5, 15%) y el porcentaje de sobreprecio de M&A por filtración (5%, escenario alto de R-01/R-03) son supuestos explícitos del equipo analista, no cifras publicadas; se declaran para que puedan ajustarse.
- Los ARO (A7) traducen las etiquetas cualitativas "Alta/Media/Baja" del `risk-register.md` a una frecuencia anual siguiendo una convención estándar de la industria, no una medición empírica del proyecto (no existe aún telemetría de producción).
