# Documentación - API de Generación de Videos con Google AI

## Descripción
Este módulo permite a los usuarios generar videos automáticamente utilizando la API de Google Generative AI (Gemini Video API).

## Configuración

### Variables de Entorno Requeridas

```env
# Google Generative AI
GOOGLE_GENERATIVE_AI_API_KEY=tu_api_key_aqui

# Opcional
GOOGLE_VIDEO_API_TIMEOUT=300          # Timeout en segundos para la API
VIDEO_POLLING_INTERVAL=30              # Intervalo de polling para actualizar estado (segundos)
```

### Configuración en application.yaml

Las siguientes propiedades se pueden configurar en `application.yaml`:

```yaml
google:
  generative-ai-api-key: ${GOOGLE_GENERATIVE_AI_API_KEY:}
  video-api-timeout-seconds: ${GOOGLE_VIDEO_API_TIMEOUT:300}

video-generation:
  polling-interval-seconds: ${VIDEO_POLLING_INTERVAL:30}
```

## Estructura del Proyecto

```
video/
├── application/
│   ├── GenerateVideoUseCase.java       # Genera un nuevo video
│   ├── GetVideoUseCase.java            # Obtiene detalles de un video
│   ├── ListVideosUseCase.java          # Lista videos del usuario
│   ├── DeleteVideoUseCase.java         # Elimina un video
│   └── VideoStatusPollingService.java  # Monitorea estado de videos
├── domain/
│   ├── Video.java                      # Entidad de dominio
│   ├── VideoStatus.java                # Enum de estados
│   ├── VideoRepository.java            # Interfaz del repositorio
│   ├── GenerateVideoRequest.java       # DTO de solicitud
│   └── VideoResponse.java              # DTO de respuesta
├── infrastructure/
│   ├── google/
│   │   ├── GoogleGenerativeAiService.java       # Integración con Google AI
│   │   └── GoogleVideoGenerationResponse.java   # Respuesta de Google
│   ├── persistence/
│   │   ├── VideoEntity.java            # Entidad JPA
│   │   ├── VideoStatusEntity.java      # Enum de persistencia
│   │   ├── VideoJpaRepository.java     # Repositorio Spring Data
│   │   └── VideoStoreImpl.java          # Implementación del repositorio
│   └── web/
│       └── VideosController.java       # Controlador REST
└── db/migration/
    └── V3__create_generated_videos_table.sql # Schema de BD
```

## API Endpoints

### 1. Generar un Video
**POST** `/api/videos/generate`

Genera un nuevo video usando Google AI.

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body:**
```json
{
  "title": "Mi Video Increíble",
  "description": "Descripción del video que quiero generar",
  "prompt": "Un video de 30 segundos mostrando un atardecer hermoso en la playa con música relajante"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "userId": 100,
  "title": "Mi Video Increíble",
  "description": "Descripción del video que quiero generar",
  "prompt": "Un video de 30 segundos mostrando un atardecer hermoso...",
  "status": "PROCESSING",
  "videoUrl": null,
  "googleJobId": "job_1712144400000",
  "errorMessage": null,
  "createdAt": "2026-04-03T10:00:00",
  "updatedAt": "2026-04-03T10:00:00"
}
```

### 2. Obtener Detalles de un Video
**GET** `/api/videos/{id}`

Obtiene los detalles y estado actual de un video.

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "userId": 100,
  "title": "Mi Video Increíble",
  "description": "Descripción del video que quiero generar",
  "prompt": "Un video de 30 segundos mostrando un atardecer...",
  "status": "COMPLETED",
  "videoUrl": "https://storage.googleapis.com/videos/video_1.mp4",
  "googleJobId": "job_1712144400000",
  "errorMessage": null,
  "createdAt": "2026-04-03T10:00:00",
  "updatedAt": "2026-04-03T10:05:30"
}
```

### 3. Listar Videos del Usuario
**GET** `/api/videos`

Lista todos los videos generados por el usuario autenticado.

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "userId": 100,
    "title": "Mi Video Increíble",
    "status": "COMPLETED",
    "videoUrl": "https://storage.googleapis.com/videos/video_1.mp4",
    ...
  },
  {
    "id": 2,
    "userId": 100,
    "title": "Otro Video",
    "status": "PROCESSING",
    "videoUrl": null,
    ...
  }
]
```

### 4. Eliminar un Video
**DELETE** `/api/videos/{id}`

Elimina un video generado.

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response (204 No Content)**

## Estados de Video

| Estado | Descripción |
|--------|-------------|
| `PENDING` | Video en espera de procesamiento |
| `PROCESSING` | Google AI está generando el video |
| `COMPLETED` | Video generado exitosamente y disponible |
| `ERROR` | Error durante la generación |

## Monitoreo de Estado

El sistema incluye un servicio de **polling automático** que consulta el estado de los videos en procesamiento cada 30 segundos (configurable).

Los videos con estado `PROCESSING` se actualizan automáticamente:
- Cuando se completan → `COMPLETED` con `videoUrl`
- Cuando fallan → `ERROR` con `errorMessage`

## Manejo de Errores

### Errores Comunes

**API Key no configurada:**
```json
{
  "error": "Google Generative AI no está configurado correctamente",
  "status": 500
}
```

**Usuario no autenticado:**
```json
{
  "error": "Usuario no autenticado",
  "status": 401
}
```

**Video no encontrado:**
```json
{
  "error": "Video no encontrado",
  "status": 404
}
```

**Permiso denegado:**
```json
{
  "error": "No tienes permiso para acceder a este video",
  "status": 403
}
```

## Flujo Típico de Uso

1. **Usuario genera video**
   - POST `/api/videos/generate` con prompt
   - Recibe respuesta con `status: PROCESSING`

2. **Sistema monitorea generación**
   - Cada 30 segundos, consulta estado en Google AI
   - Actualiza `status` y `videoUrl` cuando esté listo

3. **Usuario consulta estado**
   - GET `/api/videos/{id}` periódicamente
   - O escucha WebSocket (futuro)

4. **Video disponible**
   - Cuando `status: COMPLETED`, `videoUrl` contiene la URL del video
   - Usuario puede descargar o compartir el video

## Integración con Google Generative AI

### Pasos para Obtener API Key

1. Ir a [Google AI Studio](https://aistudio.google.com/)
2. Crear un nuevo proyecto o seleccionar uno existente
3. Habilitar Gemini Video API
4. Generar una API key
5. Guardar en variable de entorno `GOOGLE_GENERATIVE_AI_API_KEY`

### Límites y Cuotas

- Consultar los límites actuales en [Google Cloud Console](https://console.cloud.google.com/)
- La API tiene rate limiting y límites de cuota
- Se recomienda implementar retry logic en producción

## Notas de Implementación

- **Autenticación**: Se requiere token JWT válido
- **Autorización**: Los usuarios solo pueden ver/eliminar sus propios videos
- **Almacenamiento**: Los videos se almacenan en Google Cloud Storage (URL externa)
- **Async**: La generación es asincrónica mediante polling
- **Bases de Datos**: PostgreSQL con Flyway para migraciones

## Próximas Mejoras

- [ ] Soporte para WebSocket para actualizaciones en tiempo real
- [ ] Caché de estados para reducir consultas a Google AI
- [ ] Validación avanzada de prompts
- [ ] Análisis de contenido generado
- [ ] Integración con almacenamiento local/S3
- [ ] Webhook para notificaciones de Google

