# RAG-Sentinel: Auditoría de Seguridad Automatizada (Arquitectura Profesional - Promptfoo & LLM-as-a-Judge)

## 1. Introducción y Justificación de la Migración
**RAG-Sentinel** ha sido migrado a **Promptfoo**, un framework estándar de la industria para la evaluación y benchmarking de modelos de lenguaje. Esta evolución elimina la dependencia de listas estáticas (`leak_indicators`) y adopta un paradigma de **Caja Negra** basado en **LLM-as-a-Judge (Rúbricas Semánticas)**, permitiendo auditorías robustas, escalables y agnósticas al backend.

## 2. Modelo de Evaluación: ¿Caja Negra y Control por Roles?
El escáner actúa bajo un modelo estrictamente de **Caja Negra** desde la perspectiva del cliente de pruebas, gestionando la identidad y la autorización de las siguientes maneras:
* **Resolución de Identidad del Lado del Servidor (`Server-Side Resolution`):** El backend de Spring Boot no confía ciegamente en lo que el cliente envía. A través de la variable `profile` (ej. `pedro.rojas`), el adaptador (`api_adapter.py`) inyecta la identidad en la petición.
* **Soporte Dinámico de Autenticación (Tokens JWT / Bearer):** 
  * *Si la empresa provee tokens de autenticación (JWT/OAuth):* El adaptador puede configurarse para inyectar credenciales criptográficas reales en la cabecera `Authorization: Bearer <TOKEN>`.
  * *Si la empresa **no** provee tokens y opera en modo simulación:* El sistema recurre al directorio de usuarios autorizados del backend (`DemoUserDirectory`), simulando perfiles con privilegios diferenciados (invitados, empleados regulares, gerencia) para validar si el RBAC (Role-Based Access Control) y el filtrado vectorial funcionan correctamente.
  * *Si el usuario no existe:* El sistema responde de forma segura con un código HTTP `401 Unauthorized`, validando la robustez de la pasarela de seguridad.

## 3. ¿Cómo Funciona la Detección de Vulnerabilidades sin Palabras Clave?
En lugar de buscar coincidencias de cadenas estáticas, el escáner utiliza un modelo juez local (ej. Qwen a través de Ollama) evaluando mediante **Rúbricas de IA (`type: llm-rubric`)**:
1. El backend RAG procesa la consulta de seguridad y emite una respuesta corporativa.
2. Promptfoo envía la pregunta, el rol evaluado y la respuesta al Juez junto a una rúbrica analítica (criterios estrictos de negocio).
3. El Juez analiza el contexto semántico completo y determina de manera inteligente si se produjo una exfiltración, escalada de privilegios o un falso positivo (disrupción de disponibilidad), emitiendo un veredicto estructurado (`[PASS]` o `[FAIL]`) acompañado de una **recomendación de remediación técnica**.

## 4. Visualización y Reportes Ejecutivos
La ejecución genera un **Dashboard Interactivo Local (`promptfoo view`)** que permite:
* Filtrar los hallazgos por **Nivel de Severidad (Critical, High, Medium, Low)** y **Módulos de Riesgo (OWASP LLM Top 10)**.
* Auditar la traza completa de latencia, tokens consumidos (Eficiencia de Costos, prevenir DoS,impacto de latencias) y la respuesta íntegra del RAG corporativo frente a cada vector de ataque.

## 5. 🛠️ Guía de Instalación y Ejecución (Desde Cero)

Para desplegar y ejecutar la suite de auditoría **RAG-Sentinel** en un entorno local o de evaluación corporativa, sigue estos pasos.

### 5.1. 📋 Prerrequisitos del Sistema

Antes de comenzar, verifica que tengas instaladas las siguientes herramientas:

- **Node.js (v18 o superior):** requerido para ejecutar el motor principal de Promptfoo.
- **Python (v3.9 o superior) y pip:** necesarios para ejecutar el adaptador de red `api_adapter.py`, encargado de comunicarse con el backend RAG.

### 5.2. 📦 Instalación de Promptfoo e Inicialización

Instala el framework globalmente mediante NPM y crea el directorio de trabajo para la auditoría:

```bash
# 1. Instalar Promptfoo globalmente
npm install -g promptfoo

# 2. Crear la carpeta para la suite de seguridad
mkdir rag-sentinel-promptfoo
cd rag-sentinel-promptfoo

# 3. Inicializar el entorno
promptfoo init
```

Al inicializar, Promptfoo creará un archivo `promptfooconfig.yaml` en la raíz de la carpeta.

### 5.3. 🌐 Configuración del Adaptador de Red (`api_adapter.py`)

Para que RAG-Sentinel pueda evaluar el sistema RAG auditado, utiliza un adaptador en Python. El cliente debe configurar este archivo para que apunte al entorno desplegado (Dev, QA o Producción), en lugar de utilizar `localhost`.

Crea o reemplaza el archivo `api_adapter.py` en la raíz de la carpeta `rag-sentinel-promptfoo` con el siguiente código:

```python
import os
import requests
import json


def call_api(prompt, options, context):
    vars = context.get("vars", {})
    profile = vars.get("profile", "invitado")
    token = vars.get("token", None)  # Captura el token JWT si está definido en el test

    # ⚠️ CONFIGURACIÓN DEL CLIENTE:
    # Busca la variable de entorno TARGET_URL.
    # Si no existe, utiliza la URL predeterminada.
    target_url = os.getenv(
        "TARGET_URL",
        "https://api.tu-empresa.com/v1/chat"
    )

    headers = {"Content-Type": "application/json"}

    # CASO 1: Autenticación real (caja gris con JWT/OAuth)
    if token:
        headers["Authorization"] = f"Bearer {token}"
        body = {"message": prompt}

    # CASO 2: Modo simulación (caja negra con perfiles simulados)
    else:
        body = {"username": profile, "message": prompt}

    try:
        # Petición HTTP al backend auditado
        response = requests.post(
            target_url,
            json=body,
            headers=headers,
            timeout=120
        )

        try:
            data = response.json()
            respuesta_llm = data.get("respuesta", json.dumps(data))
        except ValueError:
            respuesta_llm = response.text

        return {"output": respuesta_llm}

    except Exception as e:
        return {"output": f"Error de conexión: {str(e)}"}
```

**📌 Nota para los auditores:** No es necesario modificar el código Python para cambiar la URL objetivo. Puedes configurar la dirección de la API mediante la variable de entorno `TARGET_URL` antes de ejecutar la auditoría.

Ejemplo en Bash:

```bash
export TARGET_URL="https://mi-rag-produccion.com/chat"
```

En PowerShell (Windows):

```powershell
$env:TARGET_URL = "https://mi-rag-produccion.com/chat"
```

**⚠️ Importante:** Antes de ejecutar la auditoría, verifica que la API acepte el formato JSON enviado por el adaptador y que las claves `message`, `username` y `respuesta` coincidan con el contrato real del backend. Ejecuta pruebas únicamente contra sistemas para los que tengas autorización.

### 5.4. 🤖 Configuración del Modelo Juez de IA

El cliente debe definir qué modelo actuará como **Juez de IA** en el archivo `promptfooconfig.yaml`.

Si no dispone de Ollama instalado localmente, puede utilizar proveedores en la nube, como OpenAI (GPT-4o), Azure u otros compatibles con Promptfoo.

Ejemplo de `promptfooconfig.yaml`:

```yaml
description: "RAG-Sentinel: Auditoría OWASP LLM 2025"

providers:
  - id: file://api_adapter.py
    label: "RAG Corporativo (Auditado)"

defaultTest:
  options:
    provider:
      # Utilizar OpenAI como modelo juez
      id: openai:chat:gpt-4o

      # Alternativa: utilizar un modelo local compatible
      # id: openai:chat:qwen2.5-coder:7b
      # config:
      #   apiBaseUrl: "http://localhost:11434/v1"
      #   apiKey: "ollama"

tests:
  - tests/*.yaml
```

**🔑 Configuración de credenciales:** Si utilizas OpenAI, configura la variable de entorno `OPENAI_API_KEY` con una clave válida. No incluyas claves API directamente en archivos versionados.

**📌 Nota:** El identificador del proveedor local debe corresponder a un modelo y una configuración compatibles con la versión instalada de Promptfoo.

### 5.5. 🚀 Ejecución de la Auditoría y Dashboard

Una vez configurados la URL de la API objetivo y el modelo juez, ejecuta la batería de pruebas:

```bash
# Ejecutar los casos de prueba sin utilizar caché
promptfoo eval --no-cache

# Abrir el dashboard interactivo
promptfoo view
```

El comando `promptfoo view` permite consultar los resultados mediante una interfaz web local. Por defecto, Promptfoo suele utilizar el puerto `15500`, aunque el puerto efectivo puede depender de la configuración y de la versión instalada.

Desde el dashboard, el equipo de seguridad podrá revisar los resultados, identificar pruebas aprobadas o fallidas, consultar latencias y examinar las explicaciones generadas por el evaluador.

- 🔴 **FAIL:** prueba fallida según los criterios de evaluación configurados.
- 🟢 **PASS:** prueba aprobada según los criterios de evaluación configurados.

**⚠️ Importante:** Un resultado `FAIL` no implica automáticamente una vulnerabilidad crítica. Su significado depende de la prueba, los criterios configurados y la evidencia obtenida.

---

## 6. 🔄 Integración DevSecOps (Pipelines CI/CD)

El mayor valor de **RAG-Sentinel** es su capacidad para operar de forma invisible y continua dentro del ciclo de desarrollo del cliente (CI/CD). A continuación, se detallan los pasos para que el equipo de DevOps de la empresa auditada integre esta suite en sus propios repositorios.

### 6.1. ¿Qué debe copiar el cliente a su repositorio?
El equipo de desarrollo debe integrar en la raíz de su proyecto (o en un repositorio de QA dedicado) la siguiente estructura entregada por el proveedor:

```text
/su-repositorio-rag
 ├── .github/workflows/rag-sentinel-audit.yml  # Pipeline de ejecución
 ├── rag-sentinel-promptfoo/                   # Core del motor de auditoría
 │   ├── promptfooconfig.yaml
 │   ├── api_adapter.py
 │   └─

Este archivo permite definir la ejecución automatizada de las pruebas de auditoría dentro del pipeline del proyecto.

**📌 Recomendaciones de configuración:**

- 🔐 Almacena las claves API y demás credenciales sensibles en los secretos de GitHub Actions.
- 🌐 Configura la URL del sistema RAG mediante variables de entorno o secretos, según corresponda.
- 🧪 Ejecuta las pruebas contra entornos autorizados de desarrollo, QA o evaluación.
- 🚨 Define criterios de aprobación y bloqueo del pipeline según la severidad y los resultados de las pruebas.
- 📊 Conserva las evidencias y los resultados de auditoría para facilitar el análisis y la trazabilidad.

**✅ Resultado esperado:** RAG-Sentinel puede incorporarse al flujo DevSecOps para ayudar a detectar problemas de seguridad en sistemas RAG antes de promover cambios a entornos posteriores.

