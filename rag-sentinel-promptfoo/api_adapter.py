import os
import requests
import json

def call_api(prompt, options, context):
    vars = context.get("vars", {})
    profile = vars.get("profile", "invitado")
    token = vars.get("token", None) # Capturamos si la prueba trae token o no
    
    target_url = os.getenv("TARGET_URL", "http://localhost:8080/api/chat")
    
    headers = {"Content-Type": "application/json"}
    
    # CASO 1: La empresa nos dio un Token real (Caja Gris / Autenticación Criptográfica)
    if token:
        headers["Authorization"] = f"Bearer {token}"
        # Si hay token, enviamos el mensaje. El backend validará los claims del JWT.
        body = {"message": prompt}
        
    # CASO 2: No hay token, operamos en modo simulación (Caja Negra / Server-Side Resolution)
    else:
        body = {"username": profile, "message": prompt}
    
    try:
        response = requests.post(target_url, json=body, headers=headers, timeout=120)
        try:
            data = response.json()
            respuesta_llm = data.get("respuesta", json.dumps(data))
        except ValueError:
            respuesta_llm = response.text
            
        return {"output": respuesta_llm}
        
    except Exception as e:
        return {"output": f"Error de conexión: {str(e)}"}