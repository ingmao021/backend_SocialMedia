# Frontend Instructions: Reproducción de Videos desde GCS

> **Stack backend:** Spring Boot 3.x en Render (`https://backend-socialmedia-ixsm.onrender.com`)
> **Videos:** Google Cloud Storage (bucket privado, acceso mediante Signed URLs)
> **Autenticación:** JWT Bearer token (Google OAuth)

---

## 1. Arquitectura del Flujo

```
Frontend (Vercel)  →  Backend (Render)  →  Vertex AI  →  GCS (bucket privado)
     ↑                                                        |
     └──────── Signed URL V4 (acceso directo al .mp4) ────────┘
```

- El frontend **NUNCA** accede a la base de datos ni al bucket directamente.
- El backend genera **Signed URLs V4** temporales (válidas 7 días) que permiten al navegador descargar el video directamente desde GCS.
- La generación de video es **asíncrona**: el backend inicia el proceso y el frontend debe hacer **polling** hasta que termine.

---

## 2. Autenticación (JWT)

**Todas las peticiones** a `/api/videos/*` requieren el header:

```
Authorization: Bearer <JWT_TOKEN>
```

El token se obtiene tras el login con Google (`POST /api/auth/google`).

Si el token falta o expiró → el backend responde **401 Unauthorized**.

---

## 3. Endpoints y Contratos

### 3.1 Generar Video

```http
POST /api/videos/generate
Content-Type: application/json
Authorization: Bearer <JWT>

{
  "prompt": "Un atardecer en la playa con olas suaves",
  "durationSeconds": 4
}
```

`durationSeconds` acepta: `4`, `6`, u `8`.

**Response (202 Accepted):**

```json
{
  "id": "e3093147-f3a0-4039-9283-6de542a0ea9b",
  "prompt": "Un atardecer en la playa con olas suaves",
  "durationSeconds": 4,
  "status": "PROCESSING",
  "signedUrl": null,
  "signedUrlExpiresAt": null,
  "errorMessage": null,
  "createdAt": "2026-05-10T21:00:00Z",
  "updatedAt": "2026-05-10T21:00:00Z"
}
```

### 3.2 Consultar Estado (Polling)

```http
GET /api/videos/{videoId}/status
Authorization: Bearer <JWT>
```

**Response (200):**

```json
{
  "status": "PROCESSING",
  "signedUrl": null
}
```

Cuando el video termina:

```json
{
  "status": "COMPLETED",
  "signedUrl": "https://storage.googleapis.com/mis-videos-veo-31/videos/1/e309.../sample_0.mp4?X-Goog-Signature=..."
}
```

Si falla:

```json
{
  "status": "FAILED",
  "signedUrl": null
}
```

### 3.3 Listar Videos del Usuario (Paginado)

```http
GET /api/videos?page=0&size=10
Authorization: Bearer <JWT>
```

**Response (200) — Paginado de Spring:**

```json
{
  "content": [
    {
      "id": "uuid",
      "prompt": "...",
      "durationSeconds": 4,
      "status": "COMPLETED",
      "signedUrl": "https://storage.googleapis.com/...",
      "signedUrlExpiresAt": "2026-05-17T21:00:00Z",
      "errorMessage": null,
      "createdAt": "...",
      "updatedAt": "..."
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "number": 0,
  "size": 10
}
```

> ⚠️ **IMPORTANTE:** Los videos están en `response.data.content`, **NO** en `response.data` directamente.

### 3.4 Obtener Video Individual

```http
GET /api/videos/{videoId}
Authorization: Bearer <JWT>
```

Devuelve el mismo formato que un elemento de `content`.

### 3.5 Eliminar Video

```http
DELETE /api/videos/{videoId}
Authorization: Bearer <JWT>
```

Response: `204 No Content`

---

## 4. Códigos de Error del Backend

| Status | Código | Significado |
|--------|--------|-------------|
| 400 | `VALIDATION_ERROR` | Datos inválidos (prompt vacío, duración incorrecta) |
| 401 | `Unauthorized` | JWT faltante o expirado → redirigir a login |
| 403 | `QUOTA_EXCEEDED` | Límite de 2 videos completados alcanzado |
| 404 | `NOT_FOUND` | Video no encontrado o no pertenece al usuario |
| 500 | `INTERNAL_ERROR` | Error interno del backend |

---

## 5. Estrategia de Polling

### 5.1 Implementación Recomendada

```typescript
const useVideoPolling = (videoId: string) => {
  const [status, setStatus] = useState<'PROCESSING' | 'COMPLETED' | 'FAILED'>('PROCESSING');
  const [signedUrl, setSignedUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (status !== 'PROCESSING') return;

    const interval = setInterval(async () => {
      try {
        const { data } = await api.get(`/api/videos/${videoId}/status`);
        setStatus(data.status);

        if (data.status === 'COMPLETED' && data.signedUrl) {
          setSignedUrl(data.signedUrl);
          clearInterval(interval);
        }
        if (data.status === 'FAILED') {
          setError('La generación del video falló');
          clearInterval(interval);
        }
      } catch (err) {
        console.error('Polling error:', err);
        // NO detener el polling por errores de red transitorios
      }
    }, 10_000); // Cada 10 segundos

    // Timeout máximo: 12 minutos (backend tiene 10 min de timeout)
    const timeout = setTimeout(() => {
      clearInterval(interval);
      setStatus('FAILED');
      setError('La generación excedió el tiempo máximo');
    }, 12 * 60 * 1000);

    return () => {
      clearInterval(interval);
      clearTimeout(timeout);
    };
  }, [videoId, status]);

  return { status, signedUrl, error };
};
```

### 5.2 Parámetros Clave

| Parámetro | Valor | Razón |
|-----------|-------|-------|
| Intervalo de polling | 10 segundos | Balance entre latencia y carga en Render free tier |
| Timeout máximo | 12 minutos | Backend timeout es 10 min + margen |
| Detener polling cuando | `COMPLETED` o `FAILED` | Evitar requests innecesarios |

---

## 6. Reproducción del Video

### 6.1 Implementación Correcta

```tsx
// ✅ CORRECTO — URL directa en el src, key fuerza re-mount
<video
  key={signedUrl}
  src={signedUrl}
  controls
  playsInline
  preload="metadata"
/>
```

```tsx
// ❌ INCORRECTO — NO usar fetch/blob (causa CORS y problemas de memoria)
const blob = await fetch(signedUrl).then(r => r.blob());
const blobUrl = URL.createObjectURL(blob);
<video src={blobUrl} />
```

### 6.2 Atributos del `<video>`

| Atributo | Valor | Razón |
|----------|-------|-------|
| `src` | `{signedUrl}` | URL firmada directa al .mp4 en GCS |
| `key` | `{signedUrl}` | Fuerza re-mount en React cuando la URL cambia |
| `controls` | presente | Controles nativos del navegador |
| `playsInline` | presente | Evita fullscreen automático en iOS |
| `preload` | `"metadata"` | Solo carga metadatos (eficiente) |

> ⚠️ **NO usar** `crossOrigin="anonymous"` a menos que necesites acceso programático al video (canvas, capturas). Sin él, el `<video>` hace requests "opaque" que no requieren CORS.

### 6.3 Manejo de Errores de Reproducción

```tsx
<video
  key={signedUrl}
  src={signedUrl}
  controls
  playsInline
  preload="metadata"
  onError={(e) => {
    const mediaError = (e.target as HTMLVideoElement).error;
    switch (mediaError?.code) {
      case MediaError.MEDIA_ERR_NETWORK:
        // La URL pudo haber expirado → refetch desde backend
        console.error('Error de red. Verificar si la URL expiró.');
        break;
      case MediaError.MEDIA_ERR_DECODE:
        console.error('Error de decodificación del video.');
        break;
      case MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED:
        console.error('Formato de video no soportado.');
        break;
      default:
        console.error('Error desconocido:', mediaError);
    }
  }}
/>
```

---

## 7. Manejo de Signed URLs

### 7.1 Reglas Fundamentales

1. **NO modificar la URL.** Contiene firma criptográfica. Cualquier cambio la invalida.
2. **NO hacer `fetch()` de la URL.** Usar directamente como `src` del `<video>`.
3. **NO almacenar en localStorage** por seguridad. Siempre obtenerla del backend.
4. **El backend regenera automáticamente** URLs expiradas o próximas a expirar (margen de 1 hora).

### 7.2 Verificar Expiración

```typescript
const isSignedUrlExpired = (expiresAt: string | null): boolean => {
  if (!expiresAt) return true;
  const expiry = new Date(expiresAt);
  const margin = 60 * 60 * 1000; // 1 hora de margen
  return Date.now() > expiry.getTime() - margin;
};

// Si la URL expiró, pedir una nueva al backend
if (isSignedUrlExpired(video.signedUrlExpiresAt)) {
  const { data } = await api.get(`/api/videos/${video.id}`);
  // data.signedUrl tendrá una URL fresca
}
```

### 7.3 Formato de la Signed URL

```
https://storage.googleapis.com/mis-videos-veo-31/videos/{userId}/{videoId}/.../sample_0.mp4
  ?X-Goog-Algorithm=GOOG4-RSA-SHA256
  &X-Goog-Credential=backend-socialmedia%40gen-lang-client-...
  &X-Goog-Date=20260510T210000Z
  &X-Goog-Expires=604800
  &X-Goog-SignedHeaders=host
  &X-Goog-Signature=...
```

---

## 8. CORS

El backend configura automáticamente CORS en el bucket GCS al iniciar la aplicación.

Si hay problemas CORS:
1. Verificar que el backend se haya reiniciado/redeployado después de los últimos cambios
2. En Render, la variable `CORS_ALLOWED_ORIGINS` debe contener la URL del frontend (ej: `https://backend-social-media-ai.vercel.app`)
3. Si `crossOrigin="anonymous"` causa errores en el `<video>`, **quitarlo** — la reproducción básica no lo necesita

---

## 9. Manejo de Estados en la UI

### 9.1 Componente VideoCard

```tsx
type VideoState = 'polling' | 'ready' | 'error';

const VideoCard: React.FC<{ video: VideoResponse }> = ({ video }) => {
  const getState = (): VideoState => {
    if (video.status === 'PROCESSING') return 'polling';
    if (video.status === 'FAILED') return 'error';
    if (video.status === 'COMPLETED' && video.signedUrl) return 'ready';
    return 'error';
  };

  const state = getState();

  return (
    <div className="video-card">
      <p className="prompt">{video.prompt}</p>
      <span>{video.durationSeconds}s</span>

      {state === 'polling' && (
        <div className="video-placeholder">
          <Spinner />
          <p>Generando video con IA...</p>
          <p className="hint">Esto puede tardar hasta 5 minutos</p>
        </div>
      )}

      {state === 'error' && (
        <div className="video-error">
          <p>❌ {video.errorMessage || 'Error al generar el video'}</p>
        </div>
      )}

      {state === 'ready' && video.signedUrl && (
        <video
          key={video.signedUrl}
          src={video.signedUrl}
          controls
          playsInline
          preload="metadata"
        />
      )}

      <time>{new Date(video.createdAt).toLocaleDateString()}</time>
    </div>
  );
};
```

### 9.2 Flujo Completo

```
1. Usuario escribe prompt → POST /api/videos/generate
2. Recibe { id, status: "PROCESSING" } → Mostrar card con spinner
3. Iniciar polling GET /api/videos/{id}/status cada 10s
4. status === "COMPLETED" → Detener polling, renderizar <video src={signedUrl}>
5. status === "FAILED" → Detener polling, mostrar errorMessage
6. Timeout (12 min) → Detener polling, mostrar "Generación tardó demasiado"
```

---

## 10. Compatibilidad de Formatos

| Propiedad | Valor |
|-----------|-------|
| Contenedor | MP4 |
| Codec | H.264 (AVC) — soportado por todos los navegadores |
| Resolución | 720p |
| Aspect Ratio | 16:9 |
| Tamaño estimado | 3-10 MB (videos de 4-8 segundos) |

---

## 11. Buenas Prácticas

1. **`preload="metadata"`** en vez de `"auto"` — evita descargar todo el video al cargar la página
2. **NO autoplay** sin interacción del usuario (bloqueado por navegadores)
3. **Lazy loading** — solo cargar el `<video>` cuando sea visible en el viewport (IntersectionObserver)
4. **Key en React** — usar `key={signedUrl}` para forzar re-mount cuando cambia la URL
5. **Cuota** — el backend permite máximo 2 videos completados por usuario. Manejar el error 403 `QUOTA_EXCEEDED` en la UI

---

## 12. Checklist de Debug

Si el video no se reproduce, verificar en este orden:

- [ ] ¿`signedUrl` tiene valor (no es `null` ni `undefined`)?
- [ ] ¿`status` es `"COMPLETED"`?
- [ ] ¿La URL empieza con `https://storage.googleapis.com/`?
- [ ] ¿La URL funciona al pegarla directamente en una pestaña del navegador?
- [ ] ¿Se usa `<video src={signedUrl}>` directo (no `fetch`/blob)?
- [ ] ¿El `<video>` tiene `controls` y `playsInline`?
- [ ] ¿El JWT token se envía en las peticiones al backend?
- [ ] ¿La respuesta paginada se lee desde `response.data.content`?
- [ ] ¿El polling se detiene cuando status es `COMPLETED` o `FAILED`?
- [ ] ¿DevTools → Console muestra errores CORS o MediaError?
- [ ] ¿DevTools → Network muestra el request al video con status 200/206?
