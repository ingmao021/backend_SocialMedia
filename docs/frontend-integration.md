# Frontend Integration Guide — Social Video AI

Este documento describe el contrato exacto del backend para que el frontend verifique
que cada pieza está implementada correctamente. Todo está derivado del código fuente
real; no hay suposiciones.

---

## Configuración base

```
VITE_API_URL=https://<tu-backend>.onrender.com
```

Todas las rutas privadas requieren el header:
```
Authorization: Bearer <token>
```

El token se obtiene en `/api/auth/login`, `/api/auth/register` o `/api/auth/google`
y debe guardarse en `localStorage`.

---

## Errores — formato universal

Todos los errores del backend tienen esta forma:

```ts
interface ApiError {
  code: string;      // ver tabla abajo
  message: string;   // mensaje legible para mostrar al usuario
  fields?: Record<string, string>; // solo presente en VALIDATION_ERROR
}
```

| `code` | HTTP | Cuándo ocurre |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Campos del formulario inválidos — `fields` tiene el detalle por campo |
| `EMAIL_ALREADY_REGISTERED` | 409 | Registro con email ya existente |
| `INVALID_CREDENTIALS` | 401 | Email o contraseña incorrectos |
| `INVALID_GOOGLE_TOKEN` | 401 | Token de Google inválido o expirado |
| `QUOTA_EXCEEDED` | 403 | Usuario alcanzó el límite de videos completados |
| `NOT_FOUND` | 404 | Recurso no encontrado (video, usuario) |
| `INVALID_FILE_TYPE` | 400 | Avatar con tipo de archivo no permitido |
| `PAYLOAD_TOO_LARGE` | 413 | Archivo supera 2 MB |
| `VIDEO_GENERATION_ERROR` | 500 | Error al comunicarse con Vertex AI |
| `INTERNAL_ERROR` | 500 | Error inesperado del servidor |

**Sin token o token expirado:** HTTP 401 con body `"Unauthorized"` (string plano, sin JSON).

---

## Auth

### Registro

```
POST /api/auth/register
Content-Type: application/json
```

```ts
// Request
{
  name: string;      // 2–150 caracteres, obligatorio
  email: string;     // formato email válido, obligatorio
  password: string;  // mínimo 8 caracteres, máximo 100, obligatorio
}

// Response 201
{
  token: string;
  user: UserResponse;  // ver sección Perfil
}
```

Errores esperados: `VALIDATION_ERROR` (400), `EMAIL_ALREADY_REGISTERED` (409).

---

### Login con email/contraseña

```
POST /api/auth/login
Content-Type: application/json
```

```ts
// Request
{
  email: string;
  password: string;
}

// Response 200
{
  token: string;
  user: UserResponse;
}
```

Errores esperados: `VALIDATION_ERROR` (400), `INVALID_CREDENTIALS` (401).

---

### Login con Google

```
POST /api/auth/google
Content-Type: application/json
```

```ts
// Request
{
  idToken: string;  // JWT obtenido desde Google Sign-In
}

// Response 200
{
  token: string;
  user: UserResponse;
}
```

Errores esperados: `INVALID_GOOGLE_TOKEN` (401).

---

## Perfil de usuario

### UserResponse — forma completa

```ts
interface UserResponse {
  id: number;
  email: string;
  name: string;
  avatarUrl: string | null;   // URL https:// lista para usar en <img src>
                              // null si no tiene avatar
  hasPassword: boolean;       // true si se registró con email/contraseña
  hasGoogle: boolean;         // true si tiene cuenta Google vinculada
  videosGenerated: number;    // total de videos en estado COMPLETED
  videosLimit: number;        // límite máximo (actualmente 9999)
  createdAt: string;          // ISO 8601
}
```

> `avatarUrl` ya viene resuelta como URL firmada de GCS o como URL de Google
> (`https://lh3.googleusercontent.com/...`). El frontend puede usarla directamente.

---

### Obtener perfil propio

```
GET /api/users/me
Authorization: Bearer <token>

// Response 200 → UserResponse
```

---

### Actualizar perfil

```
PUT /api/users/me
Authorization: Bearer <token>
Content-Type: application/json
```

```ts
// Request — solo name es editable
{
  name: string;  // 2–150 caracteres
}

// Response 200 → UserResponse actualizado
```

Errores esperados: `VALIDATION_ERROR` (400).

---

### Subir avatar

```
POST /api/users/me/avatar
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

```
Campo del form: "file"
Tipos permitidos: image/png, image/jpeg
Tamaño máximo: 2 MB
```

```ts
// Response 200 → UserResponse con avatarUrl actualizado
```

Errores esperados: `INVALID_FILE_TYPE` (400), `PAYLOAD_TOO_LARGE` (413).

> **Nota:** WebP no está permitido en el backend aunque el PRD lo mencione.
> Solo `image/png` y `image/jpeg` son aceptados.

---

## Videos — flujo principal

### VideoResponse — forma completa

```ts
interface VideoResponse {
  id: string;                      // UUID
  prompt: string;
  durationSeconds: number;         // 4, 6 u 8
  status: "PROCESSING" | "COMPLETED" | "FAILED";
  signedUrl: string | null;        // URL https:// para reproducir el video
                                   // solo presente cuando status === "COMPLETED"
  signedUrlExpiresAt: string | null; // ISO 8601, TTL 7 días
  errorMessage: string | null;     // motivo del fallo, solo cuando status === "FAILED"
  createdAt: string;               // ISO 8601
  updatedAt: string;               // ISO 8601
}
```

---

### Generar video

```
POST /api/videos/generate
Authorization: Bearer <token>
Content-Type: application/json
```

```ts
// Request
{
  prompt: string;          // máximo 2000 caracteres, obligatorio
  durationSeconds: number; // debe ser exactamente 4, 6 u 8 — cualquier otro valor → 400
}

// Response 202 Accepted → VideoResponse
// status siempre será "PROCESSING" en esta respuesta
```

Errores esperados: `VALIDATION_ERROR` (400), `QUOTA_EXCEEDED` (403),
`VIDEO_GENERATION_ERROR` (500).

---

### Listar videos del usuario

```
GET /api/videos?page=0&size=6
Authorization: Bearer <token>
```

```ts
// Response 200 — Page de Spring Data
{
  content: VideoResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;          // página actual (base 0)
  first: boolean;
  last: boolean;
}
```

> Ordenado por `createdAt` descendente (más reciente primero).
> Page size máximo del backend: 50. El frontend debería pedir 6.

---

### Obtener video individual

```
GET /api/videos/{id}
Authorization: Bearer <token>

// Response 200 → VideoResponse completo
```

Errores esperados: `NOT_FOUND` (404).

---

### Eliminar video

```
DELETE /api/videos/{id}
Authorization: Bearer <token>

// Response 204 No Content
```

Errores esperados: `NOT_FOUND` (404).

---

## Polling de estado — flujo crítico

Este es el flujo que hace que el video aparezca en tiempo real.

### Endpoint de polling

```
GET /api/videos/{id}/status
Authorization: Bearer <token>
```

```ts
// Response 200
interface VideoStatusResponse {
  status: "PROCESSING" | "COMPLETED" | "FAILED";
  signedUrl: string | null;    // presente solo cuando COMPLETED
  errorMessage: string | null; // presente solo cuando FAILED
}
```

---

### Algoritmo de polling correcto

```ts
const POLL_INTERVAL_MS = 5000; // cada 5 segundos

async function pollVideo(videoId: string): Promise<void> {
  const interval = setInterval(async () => {
    try {
      const res = await api.get<VideoStatusResponse>(`/api/videos/${videoId}/status`);

      if (res.data.status === "COMPLETED") {
        clearInterval(interval);
        // res.data.signedUrl está disponible → mostrar <video src={res.data.signedUrl}>
        updateVideoInList(videoId, {
          status: "COMPLETED",
          signedUrl: res.data.signedUrl,
        });
      }

      if (res.data.status === "FAILED") {
        clearInterval(interval);
        // res.data.errorMessage explica el motivo
        updateVideoInList(videoId, {
          status: "FAILED",
          errorMessage: res.data.errorMessage,
        });
      }

      // si sigue en PROCESSING → no hacer nada, el interval sigue corriendo

    } catch (err) {
      // NO detener el polling ante errores de red transitorios
      // Solo detener si el error es 404 (video eliminado) o 401 (sesión expirada)
      if (isHttpError(err, 404) || isHttpError(err, 401)) {
        clearInterval(interval);
      }
    }
  }, POLL_INTERVAL_MS);
}
```

**Puntos críticos a verificar:**

- [ ] El polling **no se inicia** si el video ya viene con `status === "COMPLETED"` o `"FAILED"` en la carga inicial de la lista.
- [ ] El polling **se detiene** al desmontar el componente (`clearInterval` en el cleanup de `useEffect`).
- [ ] No hacer polling de múltiples videos con el mismo `setInterval` global; cada video tiene su propio intervalo independiente.
- [ ] El `signedUrl` que llega en el polling response se usa directamente, no se hace otra llamada a `GET /api/videos/{id}`.

---

### Ciclo de vida del video en el backend

```
Usuario hace POST /generate
        │
        ▼
  status: PROCESSING
  vertexOperationName guardado en BD
        │
        ▼ (cada 15s el job de backend llama a Vertex AI)
        │
  ┌─────┴──────┐
  │            │
  ▼            ▼
COMPLETED    FAILED
signedUrl    errorMessage
en BD        en BD
        │
        ▼
Próximo poll del frontend recibe
el estado final y signedUrl/errorMessage
```

> El backend tarda entre 1 y 5 minutos en procesar un video.
> El timeout del backend es de **20 minutos** — si pasa ese tiempo, el video pasa a
> `FAILED` con el mensaje `"La generación excedió el tiempo máximo de 20 minutos"`.

---

## Reproducción del video

El `signedUrl` es una URL firmada de Google Cloud Storage válida por **7 días**.

```tsx
// Uso correcto en React
<video
  src={video.signedUrl}
  controls
  playsInline
  style={{ width: "100%" }}
/>
```

**Puntos a verificar:**

- [ ] El `<video>` se renderiza solo cuando `status === "COMPLETED"` y `signedUrl !== null`.
- [ ] Si `signedUrl` expira (después de 7 días), basta con llamar a `GET /api/videos/{id}` o `GET /api/videos/{id}/status` — el backend genera uno nuevo automáticamente y lo devuelve.
- [ ] El elemento `<video>` no debe usar `crossOrigin="anonymous"` — el bucket GCS ya tiene CORS configurado para `*` en GET/HEAD.

---

## Cuota de videos

El campo `videosGenerated` en `UserResponse` es el contador de videos `COMPLETED`.
El campo `videosLimit` es el máximo permitido.

```ts
const hasReachedLimit = user.videosGenerated >= user.videosLimit;
```

Cuando se alcanza el límite, `POST /api/videos/generate` devuelve:
```json
{ "code": "QUOTA_EXCEEDED", "message": "Has alcanzado el límite de N videos completados" }
```

El frontend debe refrescar `GET /api/users/me` después de cada video completado para
mantener el contador actualizado.

---

## Checklist de implementación

### Auth
- [ ] Token JWT guardado en `localStorage` y enviado en cada request como `Authorization: Bearer <token>`
- [ ] Al recibir 401, limpiar `localStorage` y redirigir a `/login`
- [ ] Rehidratación de sesión al iniciar la app (leer token de `localStorage`)
- [ ] Login con Google: obtener `idToken` de la librería de Google y enviarlo a `POST /api/auth/google`
- [ ] Manejo de `VALIDATION_ERROR` mostrando los errores por campo (`fields`)

### Dashboard de videos
- [ ] Lista paginada con `GET /api/videos?page=0&size=6`
- [ ] Al generar, agregar el nuevo video con `status: PROCESSING` a la lista sin recargar
- [ ] Iniciar polling individual por cada video en `PROCESSING`
- [ ] Detener polling cuando el componente se desmonta
- [ ] Al recibir `COMPLETED`, actualizar el item en la lista con `signedUrl` y mostrar `<video>`
- [ ] Al recibir `FAILED`, mostrar `errorMessage` en el item
- [ ] Botón de borrado con confirmación que llama a `DELETE /api/videos/{id}` y elimina el item de la lista
- [ ] Mostrar `QUOTA_EXCEEDED` como toast o mensaje específico (no como error genérico)

### Perfil
- [ ] Avatar: solo aceptar PNG y JPEG (no WebP) — el backend rechazará WebP con 400
- [ ] Después de subir avatar, actualizar `avatarUrl` desde la respuesta (no forzar recarga)
- [ ] `hasPassword` y `hasGoogle` para mostrar qué métodos de login tiene el usuario

### Reproducción
- [ ] `<video src={signedUrl}>` solo cuando `status === "COMPLETED"` y `signedUrl !== null`
- [ ] No añadir `crossOrigin` al elemento video
- [ ] Manejar el caso en que el video está en `PROCESSING` (spinner) y en `FAILED` (mensaje de error con `errorMessage`)
