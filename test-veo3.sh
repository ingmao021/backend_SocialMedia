#!/bin/bash

# ===========================================
# Script de Prueba para Veo 3 Fast - Vertex AI
# ===========================================

# CONFIGURACIÓN - Modifica estos valores
PROJECT_ID="${GOOGLE_CLOUD_PROJECT_ID: social-video-ia-seguridad}"
PROMPT="A red ball rolling slowly on green grass, sunny day"

echo "╔════════════════════════════════════════════╗"
echo "║     Prueba de Veo 3 Fast - Vertex AI       ║"
echo "╚════════════════════════════════════════════╝"
echo ""
echo "Proyecto: $PROJECT_ID"
echo "Prompt: $PROMPT"
echo ""

# 1. Obtener token
echo "1️⃣  Obteniendo access token..."
ACCESS_TOKEN=$(gcloud auth print-access-token 2>/dev/null)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "❌ Error: No se pudo obtener el token."
    echo "   Ejecuta: gcloud auth login"
    exit 1
fi
echo "✅ Token obtenido (${#ACCESS_TOKEN} caracteres)"

# 2. Enviar solicitud
echo ""
echo "2️⃣  Enviando solicitud de generación..."
RESPONSE=$(curl -s -X POST \
  "https://us-central1-aiplatform.googleapis.com/v1/projects/${PROJECT_ID}/locations/us-central1/publishers/google/models/veo-3.0-fast-generate-001:predictLongRunning" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"instances\": [{\"prompt\": \"${PROMPT}\"}],
    \"parameters\": {
      \"aspectRatio\": \"16:9\",
      \"sampleCount\": 1,
      \"durationSeconds\": 4,
      \"resolution\": \"720p\"
    }
  }")

# Verificar si hay error en la respuesta
ERROR_CODE=$(echo $RESPONSE | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ -n "$ERROR_CODE" ]; then
    echo "❌ Error en la solicitud (código: $ERROR_CODE)"
    echo ""
    echo "Respuesta completa:"
    echo $RESPONSE | python3 -m json.tool 2>/dev/null || echo $RESPONSE
    echo ""
    
    case $ERROR_CODE in
        400) echo "💡 Solución: Verifica los parámetros del prompt" ;;
        401) echo "💡 Solución: Ejecuta 'gcloud auth login'" ;;
        403) echo "💡 Solución: Verifica permisos IAM o habilita billing" ;;
        404) echo "💡 Solución: Verifica el PROJECT_ID y la región" ;;
        429) echo "💡 Solución: Espera 1-2 minutos (cuota excedida)" ;;
    esac
    exit 1
fi

# Extraer operation name
OPERATION_NAME=$(echo $RESPONSE | grep -o '"name":"[^"]*"' | cut -d'"' -f4)

if [ -z "$OPERATION_NAME" ]; then
    echo "❌ Error: No se recibió operation name"
    echo "Respuesta: $RESPONSE"
    exit 1
fi

OPERATION_ID=$(echo $OPERATION_NAME | rev | cut -d'/' -f1 | rev)
echo "✅ Operación iniciada: $OPERATION_ID"

# 3. Polling del estado
echo ""
echo "3️⃣  Esperando generación del video (puede tardar 30-60 segundos)..."
echo ""

for i in {1..20}; do
    sleep 10
    
    STATUS_RESPONSE=$(curl -s -X GET \
      "https://us-central1-aiplatform.googleapis.com/v1/projects/${PROJECT_ID}/locations/us-central1/operations/${OPERATION_ID}" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}")
    
    DONE=$(echo $STATUS_RESPONSE | grep -o '"done":true')
    
    if [ -n "$DONE" ]; then
        echo ""
        
        # Verificar si hay error
        ERROR_MSG=$(echo $STATUS_RESPONSE | grep -o '"message":"[^"]*"' | head -1 | cut -d'"' -f4)
        if [ -n "$ERROR_MSG" ] && echo $STATUS_RESPONSE | grep -q '"error"'; then
            echo "❌ Error en la generación:"
            echo "   $ERROR_MSG"
            exit 1
        fi
        
        # Extraer URL del video
        VIDEO_URL=$(echo $STATUS_RESPONSE | grep -o '"gcsUri":"[^"]*"' | cut -d'"' -f4)
        
        if [ -n "$VIDEO_URL" ]; then
            echo "╔════════════════════════════════════════════╗"
            echo "║     🎬 VIDEO GENERADO EXITOSAMENTE!        ║"
            echo "╚════════════════════════════════════════════╝"
            echo ""
            echo "URL del video:"
            echo "  $VIDEO_URL"
            echo ""
            echo "Para descargar:"
            echo "  gsutil cp \"$VIDEO_URL\" ./video_test.mp4"
            echo ""
            echo "Para ver en navegador (si es público):"
            echo "  Abre Cloud Console > Cloud Storage > Busca el archivo"
        else
            echo "✅ Operación completada"
            echo ""
            echo "Respuesta completa:"
            echo $STATUS_RESPONSE | python3 -m json.tool 2>/dev/null || echo $STATUS_RESPONSE
        fi
        exit 0
    fi
    
    printf "  ⏳ [%2d/20] Procesando... (%d segundos)\r" $i $((i*10))
done

echo ""
echo "❌ Timeout: El video tardó más de 200 segundos"
echo "   Puedes consultar manualmente el estado con:"
echo "   curl -H \"Authorization: Bearer \$(gcloud auth print-access-token)\" \\"
echo "     \"https://us-central1-aiplatform.googleapis.com/v1/projects/${PROJECT_ID}/locations/us-central1/operations/${OPERATION_ID}\""
exit 1
