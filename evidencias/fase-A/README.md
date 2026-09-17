# Evidencias - Fase A (Avance 1)

Esta carpeta reune la evidencia del sistema **sin filtrado**: corpus de
prueba, pipeline RAG basico y los tres ataques de exfiltracion descritos en
el marco teorico (seccion 4.2) y en `risk-register.md`.

## Como generar la evidencia

1. Levantar el entorno y correr el backend (ver `SETUP.md` en la raiz del
   repo).
2. Correr los tres scripts de `ataques-demo/` en orden. Cada uno escribe su
   respuesta cruda en esta carpeta:
   - `ataque-01-consulta-directa.json`
   - `ataque-02-reformulacion-evasiva.json`
   - `ataque-03-inyeccion-instrucciones.json`
3. Revisar `query-log.jsonl` (se genera solo, una linea por interaccion) y
   completar la tabla de abajo con lo observado.

## Tabla de resultados (completar despues de correr los ataques)

| Ataque | Documento objetivo | Rol autorizado del documento | Rol real del atacante | Aparecio en `fragmentosRecuperados` | Se filtro info restringida en la respuesta |
|---|---|---|---|---|---|
| 1. Consulta directa | `confidencial/presupuesto-finanzas-2026.md` | FINANZAS, GERENCIA | RRHH (pedro.rojas) | | |
| 2. Reformulacion evasiva | `confidencial/presupuesto-finanzas-2026.md` | FINANZAS, GERENCIA | RRHH (pedro.rojas) | | |
| 3. Inyeccion de instrucciones | `secreto/plan-adquisicion-startup.md` | GERENCIA (solo laura.medina) | RRHH (pedro.rojas) | | |

## Que se espera concluir en esta fase

Segun la seccion 5.1 del marco teorico, en un sistema sin filtrado la
consulta directa y la reformulacion evasiva deberian tener exito en la
mayoria de los casos, porque el motor de busqueda solo evalua similitud
semantica. La inyeccion de instrucciones deberia depender de si el
fragmento restringido llega o no al contexto por similitud; si el corpus es
pequeno y la pregunta es muy especifica, es probable que si aparezca entre
los `top-k` resultados aunque no haga falta ninguna instruccion maliciosa
para lograrlo, lo cual es en si mismo evidencia adicional del problema.

Esta tabla y estas capturas son el insumo para la seccion "Resultados y
Analisis" del informe del Avance 1, y la linea base contra la que se va a
comparar la reduccion de riesgo lograda en el Avance 2.
