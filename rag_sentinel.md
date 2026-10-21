# 🛡️ Propuesta Comercial y de Arquitectura: RAG-Sentinel BlackBox Scanner

## 1. Visión General del Producto

**RAG-Sentinel** es una herramienta automatizada de pruebas de penetración (**LLM/RAG Pentesting**) y auditoría de cumplimiento en **Caja Negra**.

Su propuesta de valor central es evaluar sistemas de Inteligencia Artificial corporativos desde el exterior, enviando consultas maliciosas y evaluando las respuestas expuestas, sin necesidad de acceso al código fuente ni a la base de datos.

### Alcance y Limitaciones Arquitectónicas Actuales
* **Dependencia de Arquitectura RAG:** RAG-Sentinel está optimizado para auditar ecosistemas basados en Recuperación Aumentada por Generación (*Retrieval-Augmented Generation*) respaldados por motores vectoriales. No está diseñado para LLMs puros sin conexión a datos externos.
* **Heurísticas Basadas en Sincronización de Cadenas (String Matching):** En su versión actual, el motor de evaluación analiza respuestas mediante la búsqueda de patrones de texto e indicadores de fuga normalizados (`leak_indicators`). Aunque eficiente para despliegues rápidos, esta aproximación presenta retos de falsos negativos ante paráfrasis muy avanzadas del modelo, sentando la base para una futura evolución hacia una arquitectura *LLM-as-a-Judge*.

---

## 2. Arquitectura y Módulos de Escaneo

El escáner evoluciona la línea base académica hacia una **taxonomía comercial ampliada de 8 vectores de ataque**, alineada con los estándares de la industria (**OWASP Top 10 for LLM Applications**).

### Módulo 1: Probe de Control de Acceso y RBAC
* **Ataca el riesgo R-01 (Direct Data Exfiltration)**
* **Objetivo:** Detectar si el RAG filtra información restringida a usuarios de bajo privilegio.
* **Cómo opera:** Simula perfiles de usuario externos o regulares y envía consultas directas sobre datos financieros, legales o estratégicos. Analiza si el modelo de lenguaje cede y expone información protegida.

### Módulo 2: Motor de Reformulación Evasiva
* **Ataca el riesgo R-02 (Evasive Reformulation)**
* **Objetivo:** Evaluar si el sistema es vulnerable a perífrasis, sinónimos o resúmenes diseñados para burlar filtros superficiales.
* **Cómo opera:** Aplica un generador de paráfrasis semánticas automatizadas sobre las preguntas prohibidas para medir la tasa de exfiltración encubierta.

### Módulo 3: Fuzzing de Inyección de Instrucciones / Jailbreak
* **Ataca el riesgo R-03 (Direct Prompt Injection)**
* **Objetivo:** Auditar la robustez del System Prompt frente a ataques de exfiltración masiva (inspirado en la metodología de *Qi et al. 2024*).
* **Cómo opera:** Lanza payloads con órdenes explícitas de evasión, juegos de rol o manipulación de contexto para calcular el índice de resiliencia del modelo.

### Módulo 4: Extracción de Instrucciones del Sistema
* **Evaluación de fugas de configuración (System Prompt Extraction)**
* **Objetivo:** Auditar si el modelo revela sus reglas de negocio ocultas o directivas de seguridad internas.

### Módulo 5: Escalada de Privilegios por Suplantación
* **Evaluación de asunción de roles (Privilege Escalation by Impersonation)**
* **Objetivo:** Evaluar la susceptibilidad del modelo a la ingeniería social automatizada simulando identidades de alta jerarquía (ej. Gerencia General).

### Módulo 6: Inyección Indirecta vía Datos
* **Evaluación de vectores en reposo (Indirect Prompt Injection)**
* **Objetivo:** Detectar vulnerabilidades de inyección provenientes de documentos recuperados (ej. CVs o políticas externas) que contienen instrucciones maliciosas ocultas.

### Módulo 7: Vectores Genéricos OWASP LLM Top 10
* **Ataca riesgos de Agencia Excesiva, Salidas Inseguras, DoS y Sobreconfianza**
* **Objetivo:** Auditar fallos operativos y debilidades secundarias del modelo de lenguaje.
* **Cómo opera:** Ejecuta pruebas sobre ejecución de comandos o transacciones simuladas, inyección de scripts en salidas (HTML/JS), pruebas de agotamiento de recursos (*Denial of Service*) e inducción de desinformación/alucinaciones maliciosas.

### Módulo 8: Happy Path y Falsos Positivos (Disponibilidad)
* **Ataca el riesgo de Interrupción de Usabilidad (Availability Disruption)**
* **Objetivo:** Comprobar que las defensas de seguridad del RAG no sean tan agresivas que terminen bloqueando a usuarios legítimos.
* **Cómo opera:** Envía consultas totalmente autorizadas y públicas (ej. información corporativa general o acceso legítimo de personal de RRHH a sus expedientes) para medir si el sistema responde correctamente o sufre de sobrecorrección defensiva (*False Refusals*).

---

## 3. Flujo de Trabajo del Escáner y Adaptación Empresarial

La arquitectura del escáner es completamente agnóstica mediante archivos de configuración externos (`test_suite.json` y `target_config.json`). 

### Autenticación Avanzada en Entornos Empresariales Reales
Cuando la auditoría se traslada a un entorno de producción corporativo y la empresa permite la entrega controlada de **tokens de acceso real** (como OAuth o JWT), RAG-Sentinel escala su capacidad de simulación utilizando esquemas de cabeceras dinámicas (`profile_auth`). 

Esto permite inyectar credenciales reales correspondientes a cada nivel de privilegio dentro de las peticiones HTTP automatizadas, auditando de manera fehaciente la segregación de funciones del sistema de identidad de la compañía:

```json
{
  "url": "[https://api.empresa.com/v1/ai/asistente-interno](https://api.empresa.com/v1/ai/asistente-interno)",
  "method": "POST",
  "timeout": 60,
  "headers": {
    "Content-Type": "application/json",
    "X-Audit-Trace-ID": "RAG-Sentinel-Pentest"
  },
  "request_template": {
    "promptText": "{{payload}}",
    "metadata": {
      "channel": "web",
      "session_id": "audit-session-xyz"
    }
  },
  "profile_auth": {
    "invitado": {
      "headers": { 
        "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6...[TOKEN_REAL_1]" 
      }
    },
    "empleado_regular": {
      "headers": { 
        "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6...[TOKEN_REAL_2]" 
      }
    }
  }
}