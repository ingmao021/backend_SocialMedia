# Frontend Instructions: Reproducción de Videos desde GCS

> Backend: Spring Boot 3.x en Render. Videos en Google Cloud Storage (bucket privado).

## Arquitectura del Flujo

```
Frontend (Vercel) → Backend (Render) → Vertex AI → GCS (bucket privado)
     ↑                                                    |
     └──────── Signed URL (acceso directo al .mp4) ───────┘
```

El backend genera **Signed URLs V4** (válidas 7 días) que permiten al navegador
descargar el video directamente desde GCS sin autenticación adicional.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/videos/generate` | Crear video. Body: `{ "prompt": string, "durationSeconds": 4\|6\|8 }` |
| GET | `/api/videos/{id}/status` | Polling: `{ status, signedUrl }` |
| GET | `/api/videos?page=0&size=10` | Listar videos (paginado, campo `content`) |
| GET | `/api/videos/{id}` | Video individual |
| DELETE | `/api/videos/{id}` | Eliminar video |

Todas requieren header: `Authorization: Bearer <JWT>`

## Reproducción del Video

```tsx
// ✅ CORRECTO — URL directa en el src
<video
  key={signedUrl}
  src={signedUrl}
  controls
  playsInline
  preload="metadata"
/>

// ❌ INCORRECTO — NO usar fetch/blob
const blob = await fetch(signedUrl).then(r => r.blob());
```

**Reglas:**
- Usar `signedUrl` directamente como `src` del `<video>` — NO hacer `fetch()`
- Usar `key={signedUrl}` para forzar re-mount cuando cambia la URL
- NO agregar `crossOrigin="anonymous"` salvo que necesites acceso programático
- NO modificar la URL — la firma criptográfica se invalida

## Polling (Status)

```typescript
// Polling cada 10s, timeout 12 min
const interval = setInterval(async () => {
  const { data } = await api.get(`/api/videos/${videoId}/status`);
  if (data.status === 'COMPLETED') { setSignedUrl(data.signedUrl); clearInterval(interval); }
  if (data.status === 'FAILED') { clearInterval(interval); }
}, 10_000);
```

## Respuesta Paginada

```typescript
// ⚠️ Los videos están en response.data.content, NO en response.data
const { data } = await api.get('/api/videos?page=0&size=10');
const videos = data.content; // Array de VideoResponse
```

## Manejo de Errores del `<video>`

```tsx
<video
  onError={(e) => {
    const code = (e.target as HTMLVideoElement).error?.code;
    // 2 = NETWORK (URL expirada) → refetch desde backend
    // 3 = DECODE (codec incompatible)
    // 4 = SRC_NOT_SUPPORTED (formato no soportado)
  }}
/>
```

## CORS

El backend configura automáticamente CORS en el bucket GCS al iniciar.
Si aún hay problemas CORS:
1. Verificar que el backend se haya reiniciado tras el deploy
2. En Render, la variable `CORS_ALLOWED_ORIGINS` debe incluir el dominio del frontend

## Checklist de Debug

- [ ] ¿`signedUrl` no es `null`?
- [ ] ¿La URL empieza con `https://storage.googleapis.com/`?
- [ ] ¿La URL funciona pegándola en el navegador?
- [ ] ¿Se usa `src={signedUrl}` directo (no fetch)?
- [ ] ¿El JWT se envía en las peticiones al backend?
- [ ] ¿La respuesta paginada se lee desde `data.content`?
