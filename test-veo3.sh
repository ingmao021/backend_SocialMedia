#!/bin/bash

# ===========================================
# Script de Prueba para Veo 3 Fast - Vertex AI
# ===========================================

# CONFIGURACIÓN - Modifica este valor con tu Project ID
PROJECT_ID="${GOOGLE_CLOUD_PROJECT_ID:-social-video-ia-seguridad}"
PROMPT="A red ball rolling slowly on green grass, sunny day"

echo "╔════════════════════════════════════════════╗"
echo "║     Prueba de Veo 3 Fast - Vertex AI       ║"
echo "╚════════════════════════════════════════════╝"
echo ""

# 0. Verificar gcloud
echo "0️⃣  Verificando configuración..."

if ! command -v gcloud &> /dev/null; then
    echo "❌ Error: gcloud CLI no está instalado"
    echo "   Instala desde: https://cloud.google.com/sdk/docs/install"
    exit 1
fi
echo "   ✅ gcloud CLI encontrado"

# Verificar cuenta activa
ACTIVE_ACCOUNT=$(gcloud config get-value account 2>/dev/null)
if [ -z "$ACTIVE_ACCOUNT" ] || [ "$ACTIVE_ACCOUNT" == "(unset)" ]; then
    echo "❌ Error: No hay cuenta activa en gcloud"
    echo ""
    echo "   Ejecuta estos comandos:"
    echo "   1. gcloud auth login"
    echo "   2. gcloud config set project $PROJECT_ID"
    exit 1
fi
echo "   ✅ Cuenta: $ACTIVE_ACCOUNT"

# Verificar proyecto
CURRENT_PROJECT=$(gcloud config get-value project 2>/dev/null)
if [ -z "$CURRENT_PROJECT" ] || [ "$CURRENT_PROJECT" == "(unset)" ]; then
    echo "⚠️  Proyecto no configurado, usando: $PROJECT_ID"
    gcloud config set project "$PROJECT_ID" 2>/dev/null
else
    PROJECT_ID="$CURRENT_PROJECT"
fi
echo "   ✅ Proyecto: $PROJECT_ID"

echo ""
echo "Prompt: $PROMPT"
echo ""

# 1. Obtener token
echo "1️⃣  Obteniendo access token..."
ACCESS_TOKEN_OUTPUT=$(gcloud auth print-access-token 2>&1)
ACCESS_TOKEN_EXIT_CODE=$?

# Verificar si hubo error
if [ $ACCESS_TOKEN_EXIT_CODE -ne 0 ]; then
    echo "❌ Error al obtener token (código: $ACCESS_TOKEN_EXIT_CODE)"
    echo "   Mensaje: $ACCESS_TOKEN_OUTPUT"
    echo ""
    echo "💡 Soluciones:"
    echo "   1. gcloud auth login"
    echo "   2. gcloud auth application-default login"
    echo "   3. Verifica tu conexión a internet"
    exit 1
fi

ACCESS_TOKEN="$ACCESS_TOKEN_OUTPUT"

if [ -z "$ACCESS_TOKEN" ]; then
    echo "❌ Error: Token vacío"
    echo "   Ejecuta: gcloud auth login"
    exit 1
fi

# Verificar que el token parece válido (no es un mensaje de error)
if [[ "$ACCESS_TOKEN" == *"ERROR"* ]] || [[ "$ACCESS_TOKEN" == *"error"* ]] || [[ "$ACCESS_TOKEN" == *"WARNING"* ]]; then
    echo "❌ Error: Respuesta inesperada al obtener token:"
    echo "   $ACCESS_TOKEN"
    echo ""
    echo "💡 Ejecuta: gcloud auth login"
    exit 1
fi

TOKEN_LENGTH=${#ACCESS_TOKEN}
if [ $TOKEN_LENGTH -lt 100 ]; then
    echo "⚠️  Advertencia: Token parece muy corto ($TOKEN_LENGTH caracteres)"
    echo "   Token: ${ACCESS_TOKEN:0:50}..."
fi

echo "   ✅ Token obtenido ($TOKEN_LENGTH caracteres)"

# 2. Enviar solicitud
echo ""
echo "2️⃣  Enviando solicitud de generación..."
echo "   URL: https://us-central1-aiplatform.googleapis.com/.../veo-3.0-fast-generate-001"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
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

# Separar respuesta y código HTTP
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "   HTTP Status: $HTTP_CODE"

# Verificar código HTTP
if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ Error HTTP $HTTP_CODE"
    echo ""
    echo "Respuesta:"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    echo ""
    
    case $HTTP_CODE in
        400) echo "💡 El prompt puede contener contenido no permitido" ;;
        401) echo "💡 Token inválido. Ejecuta: gcloud auth login" ;;
        403) 
            if echo "$BODY" | grep -q "billing"; then
                echo "💡 Habilita billing en: https://console.cloud.google.com/billing"
            else
                echo "💡 Verifica permisos: tu cuenta necesita rol 'Vertex AI User'"
            fi
            ;;
        404) echo "💡 Verifica que el PROJECT_ID sea correcto: $PROJECT_ID" ;;
        429) echo "💡 Cuota excedida. Espera 1-2 minutos e intenta de nuevo" ;;
    esac
    exit 1
fi

# Verificar si hay error en el body
ERROR_CODE=$(echo "$BODY" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ -n "$ERROR_CODE" ]; then
    echo "❌ Error en la respuesta (código: $ERROR_CODE)"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    exit 1
fi

# Extraer operation name
OPERATION_NAME=$(echo "$BODY" | grep -o '"name":"[^"]*"' | cut -d'"' -f4)

if [ -z "$OPERATION_NAME" ]; then
    echo "❌ Error: No se recibió operation name"
    echo "Respuesta: $BODY"
    exit 1
fi

OPERATION_ID=$(echo "$OPERATION_NAME" | rev | cut -d'/' -f1 | rev)
echo "   ✅ Operación iniciada: $OPERATION_ID"

# 3. Polling del estado
echo ""
echo "3️⃣  Esperando generación del video..."
echo "   (Veo 3 Fast tarda ~30-60 segundos para videos de 4 seg)"
echo ""

for i in {1..24}; do
    sleep 10
    
    STATUS_RESPONSE=$(curl -s -X GET \
      "https://us-central1-aiplatform.googleapis.com/v1/projects/${PROJECT_ID}/locations/us-central1/operations/${OPERATION_ID}" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}")
    
    DONE=$(echo "$STATUS_RESPONSE" | grep -o '"done":true')
    
    if [ -n "$DONE" ]; then
        echo ""
        
        # Verificar si hay error
        if echo "$STATUS_RESPONSE" | grep -q '"error"'; then
            ERROR_MSG=$(echo "$STATUS_RESPONSE" | grep -o '"message":"[^"]*"' | head -1 | cut -d'"' -f4)
            echo "❌ Error en la generación:"
            echo "   $ERROR_MSG"
            echo ""
            echo "Respuesta completa:"
            echo "$STATUS_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$STATUS_RESPONSE"
            exit 1
        fi
        
        # Extraer URL del video
        VIDEO_URL=$(echo "$STATUS_RESPONSE" | grep -o '"gcsUri":"[^"]*"' | cut -d'"' -f4)
        
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
            echo "Para ver en Cloud Console:"
            echo "  https://console.cloud.google.com/storage/browser"
        else
            echo "✅ Operación completada"
            echo ""
            echo "Respuesta:"
            echo "$STATUS_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$STATUS_RESPONSE"
        fi
        exit 0
    fi
    
    ELAPSED=$((i*10))
    echo "   ⏳ [$i/24] Procesando... (${ELAPSED}s)"
done

echo ""
echo "❌ Timeout: El video tardó más de 240 segundos"
echo ""
echo "Puedes consultar el estado manualmente:"
echo "  OPERATION_ID=$OPERATION_ID"
echo "  curl -H \"Authorization: Bearer \$(gcloud auth print-access-token)\" \\"
echo "    \"https://us-central1-aiplatform.googleapis.com/v1/projects/${PROJECT_ID}/locations/us-central1/operations/\$OPERATION_ID\""
exit 1
