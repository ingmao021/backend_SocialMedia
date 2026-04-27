# Guía Técnica de Desarrollo Frontend
## TypeScript + Vite — Social Video AI

> Este documento está generado a partir del código fuente real del backend.
> Todos los endpoints, tipos y flujos descritos aquí reflejan el comportamiento
> actual del servidor en producción (`https://backend-socialmedia-ixsm.onrender.com`).

---

## Tabla de Contenidos

1. [Stack y configuración inicial](#1-stack-y-configuración-inicial)
2. [Variables de entorno](#2-variables-de-entorno)
3. [Estructura del proyecto](#3-estructura-del-proyecto)
4. [Tipos TypeScript](#4-tipos-typescript)
5. [Cliente HTTP — Axios](#5-cliente-http--axios)
6. [Módulo de Autenticación](#6-módulo-de-autenticación)
7. [Módulo de Videos](#7-módulo-de-videos)
8. [Módulo de YouTube](#8-módulo-de-youtube)
9. [Manejo de errores](#9-manejo-de-errores)
10. [Gestión de estado global](#10-gestión-de-estado-global)
11. [Rutas y navegación](#11-rutas-y-navegación)
12. [Polling de estado de video](#12-polling-de-estado-de-video)
13. [UX — Estados y feedback visual](#13-ux--estados-y-feedback-visual)
14. [Páginas requeridas](#14-páginas-requeridas)
15. [Checklist de integración](#15-checklist-de-integración)

---

## 1. Stack y Configuración Inicial

### Crear el proyecto

```bash
npm create vite@latest social-video-ai -- --template react-ts
cd social-video-ai
npm install
```

### Instalar dependencias

```bash
npm install axios react-router-dom
npm install -D @types/react @types/react-dom
```

### Dependencias opcionales recomendadas

```bash
npm install zustand          # manejo de estado global (alternativa ligera a Redux)
npm install date-fns         # formateo de fechas
npm install react-hot-toast  # notificaciones/toasts
```

---

## 2. Variables de Entorno

Crea el archivo `.env` en la raíz del proyecto frontend:

```env
VITE_API_URL=https://backend-socialmedia-ixsm.onrender.com
```

Para desarrollo local (cuando el backend corre en tu máquina):

```env
VITE_API_URL=http://localhost:8080
```

> **Importante**: Vite solo expone variables que empiecen con `VITE_`.
> Nunca expongas secretos en variables `VITE_` — son visibles en el bundle final.

---

## 3. Estructura del Proyecto

```
src/
├── api/
│   ├── client.ts          # Instancia de Axios con interceptores
│   ├── auth.api.ts        # Llamadas al módulo de autenticación
│   ├── videos.api.ts      # Llamadas al módulo de videos
│   └── youtube.api.ts     # Llamadas al módulo de YouTube
│
├── types/
│   ├── auth.types.ts
│   ├── video.types.ts
│   └── youtube.types.ts
│
├── modules/
│   ├── auth/
│   │   ├── AuthCallback.tsx      # Página /auth/callback (captura el JWT)
│   │   ├── AuthError.tsx         # Página /auth/error
│   │   └── useAuth.ts            # Hook de autenticación
│   ├── video/
│   │   ├── VideoForm.tsx         # Formulario de generación
│   │   ├── VideoCard.tsx         # Tarjeta de un video
│   │   ├── VideoList.tsx         # Historial de videos
│   │   └── useVideoPolling.ts    # Hook de polling de estado
│   └── youtube/
│       ├── PublishModal.tsx      # Modal para publicar en YouTube
│       └── UploadList.tsx        # Historial de publicaciones
│
├── pages/
│   ├── LoginPage.tsx
│   ├── DashboardPage.tsx
│   ├── GeneratePage.tsx
│   └── HistoryPage.tsx
│
├── routes/
│   └── AppRouter.tsx
│
├── store/
│   └── authStore.ts       # Estado global de autenticación (Zustand)
│
├── hooks/
│   └── useApi.ts          # Hook genérico para llamadas con loading/error
│
├── utils/
│   └── token.utils.ts     # Lectura/escritura del JWT en localStorage
│
├── App.tsx
└── main.tsx
```

---

## 4. Tipos TypeScript

Copia estos tipos exactamente — reflejan los DTOs del backend.

### `src/types/auth.types.ts`

```typescript
export interface User {
  id: number;
  name: string;
  email: string;
  picture: string;
}

export interface TokenDebugResponse {
  valid: boolean;
  userId?: number;
  email?: string;
  error?: string;
}
```

### `src/types/video.types.ts`

```typescript
export type VideoStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'ERROR';

export interface Video {
  id: number;
  userId: number;
  title: string;
  description: string | null;
  prompt: string;
  status: VideoStatus;
  videoUrl: string | null;
  googleJobId: string | null;
  errorMessage: string | null;
  createdAt: string;   // ISO-8601, ej: "2024-04-26T14:00:00"
  updatedAt: string;
}

// Body enviado al POST /api/videos/generate
export interface GenerateVideoRequest {
  title: string;        // requerido, max 200 caracteres
  description?: string; // opcional, max 500 caracteres
  prompt: string;       // requerido, entre 5 y 1000 caracteres
}

// Notas importantes:
// - La duración del video es fija: 5 segundos (definida en el backend)
// - El aspect ratio es fijo: 16:9 (definido en el backend)
// - La generación es ASÍNCRONA: el status inicial es PENDING/PROCESSING
//   y el backend lo actualiza automáticamente mediante polling a Google AI
```

### `src/types/youtube.types.ts`

```typescript
export type YouTubeUploadStatus =
  | 'PENDING'
  | 'PUBLISHING'
  | 'PUBLISHED'
  | 'FAILED'
  | 'DELETED';

export type YouTubeVisibility = 'PRIVATE' | 'UNLISTED' | 'PUBLIC';

export interface YouTubeUpload {
  id: number;
  videoId: number;
  userId: number;
  status: YouTubeUploadStatus;
  youtubeVideoId: string | null;
  youtubeUrl: string | null;
  title: string;
  description: string | null;
  visibility: YouTubeVisibility;
  publishedAt: string | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

// Body enviado al POST /api/youtube/publish
export interface YouTubePublishRequest {
  videoId: number;           // ID del video con status COMPLETED
  title: string;             // requerido
  description?: string;      // opcional
  visibility: YouTubeVisibility;
}
```

### `src/types/error.types.ts`

```typescript
// Estructura estándar de todos los errores del backend
export interface ApiErrorResponse {
  timestamp: string;   // ISO-8601
  status: number;
  error: string;       // Ej: "Bad Request"
  code: string;        // Código de error específico (ver sección 9)
  message: string;
  path: string;
}
```

---

## 5. Cliente HTTP — Axios

### `src/api/client.ts`

```typescript
import axios, { AxiosError } from 'axios';
import type { ApiErrorResponse } from '../types/error.types';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: false,
});

// Request interceptor — agrega el token en cada petición autenticada
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor — maneja expiración de sesión globalmente
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorResponse>) => {
    const code = error.response?.data?.code;

    if (code === 'TOKEN_EXPIRADO' || code === 'TOKEN_INVALIDO') {
      localStorage.removeItem('jwt_token');
      // Redirigir al login — ajusta según tu router
      window.location.href = '/login';
    }

    return Promise.reject(error);
  }
);

export default api;
```

---

## 6. Módulo de Autenticación

### Flujo OAuth2 completo

```
1. Usuario hace clic en "Iniciar sesión con Google"
2. Frontend llama GET /api/auth/google-url  →  { url: "https://...backend.../oauth2/authorization/google" }
3. Frontend redirige window.location.href = url
4. Google muestra pantalla de consentimiento
5. Google redirige al backend → backend procesa → backend redirige a:
      https://tu-frontend.vercel.app/auth/callback?token=<JWT>
   O en caso de error:
      https://tu-frontend.vercel.app/auth/error?reason=<razón>
6. Frontend captura el token de la URL y lo guarda en localStorage
```

### `src/api/auth.api.ts`

```typescript
import api from './client';
import type { User } from '../types/auth.types';

export const getGoogleLoginUrl = async (): Promise<string> => {
  const { data } = await api.get<{ url: string }>('/api/auth/google-url');
  return data.url;
};

export const getCurrentUser = async (): Promise<User> => {
  const { data } = await api.get<User>('/api/auth/me');
  return data;
};

export const checkStatus = async (): Promise<boolean> => {
  try {
    await api.get('/api/auth/status');
    return true;
  } catch {
    return false;
  }
};
```

### `src/modules/auth/AuthCallback.tsx`

Esta página debe existir en la ruta `/auth/callback`. El backend redirige aquí tras autenticarse.

```typescript
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function AuthCallback() {
  const navigate = useNavigate();

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');

    if (token) {
      localStorage.setItem('jwt_token', token);
      navigate('/dashboard', { replace: true });
    } else {
      navigate('/auth/error?reason=token_missing', { replace: true });
    }
  }, [navigate]);

  return <div>Iniciando sesión...</div>;
}
```

### `src/modules/auth/AuthError.tsx`

Ruta `/auth/error` — el backend redirige aquí si falla la autenticación.

```typescript
import { useSearchParams } from 'react-router-dom';

const ERROR_MESSAGES: Record<string, string> = {
  token_missing: 'No se recibió el token de sesión. Intenta de nuevo.',
  server_error: 'Error interno del servidor. Intenta más tarde.',
};

export default function AuthError() {
  const [params] = useSearchParams();
  const reason = params.get('reason') ?? 'unknown';
  const message = ERROR_MESSAGES[reason] ?? 'Error desconocido al iniciar sesión.';

  return (
    <div>
      <h1>Error de autenticación</h1>
      <p>{message}</p>
      <a href="/login">Volver al inicio</a>
    </div>
  );
}
```

### `src/utils/token.utils.ts`

```typescript
const TOKEN_KEY = 'jwt_token';

export const saveToken = (token: string): void =>
  localStorage.setItem(TOKEN_KEY, token);

export const getToken = (): string | null =>
  localStorage.getItem(TOKEN_KEY);

export const removeToken = (): void =>
  localStorage.removeItem(TOKEN_KEY);

export const isAuthenticated = (): boolean =>
  getToken() !== null;
```

### `src/store/authStore.ts` (con Zustand)

```typescript
import { create } from 'zustand';
import type { User } from '../types/auth.types';
import { getCurrentUser } from '../api/auth.api';
import { removeToken, isAuthenticated } from '../utils/token.utils';

interface AuthState {
  user: User | null;
  loading: boolean;
  fetchUser: () => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  loading: false,

  fetchUser: async () => {
    if (!isAuthenticated()) return;
    set({ loading: true });
    try {
      const user = await getCurrentUser();
      set({ user, loading: false });
    } catch {
      removeToken();
      set({ user: null, loading: false });
    }
  },

  logout: () => {
    removeToken();
    set({ user: null });
    window.location.href = '/login';
  },
}));
```

---

## 7. Módulo de Videos

### `src/api/videos.api.ts`

```typescript
import api from './client';
import type { Video, GenerateVideoRequest } from '../types/video.types';

export const generateVideo = async (data: GenerateVideoRequest): Promise<Video> => {
  const { data: video } = await api.post<Video>('/api/videos/generate', data);
  return video;
};

export const getVideo = async (id: number): Promise<Video> => {
  const { data } = await api.get<Video>(`/api/videos/${id}`);
  return data;
};

export const listVideos = async (): Promise<Video[]> => {
  const { data } = await api.get<Video[]>('/api/videos');
  return data;
};

export const deleteVideo = async (id: number): Promise<void> => {
  await api.delete(`/api/videos/${id}`);
};
```

### Validaciones del formulario

El backend valida estos campos y retorna 400 con código `VALIDATION_ERROR` si no se cumplen:

| Campo         | Regla                              |
|---------------|------------------------------------|
| `title`       | Requerido. Máximo 200 caracteres.  |
| `description` | Opcional. Máximo 500 caracteres.   |
| `prompt`      | Requerido. Entre 5 y 1000 chars.   |

> **Nota**: Duración (5 segundos) y aspect ratio (16:9) son fijos en el backend.
> No es necesario enviarlos desde el frontend.

### `src/modules/video/VideoForm.tsx` (estructura base)

```typescript
import { useState } from 'react';
import { generateVideo } from '../../api/videos.api';
import type { GenerateVideoRequest, Video } from '../../types/video.types';

interface Props {
  onSuccess: (video: Video) => void;
}

export default function VideoForm({ onSuccess }: Props) {
  const [form, setForm] = useState<GenerateVideoRequest>({
    title: '',
    prompt: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const video = await generateVideo(form);
      onSuccess(video);
    } catch (err: any) {
      const message = err.response?.data?.message ?? 'Error al generar el video';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        value={form.title}
        onChange={(e) => setForm({ ...form, title: e.target.value })}
        placeholder="Título del video"
        maxLength={200}
        required
        disabled={loading}
      />
      <textarea
        value={form.prompt}
        onChange={(e) => setForm({ ...form, prompt: e.target.value })}
        placeholder="Describe el video que quieres generar (5-1000 caracteres)"
        minLength={5}
        maxLength={1000}
        required
        disabled={loading}
      />
      <textarea
        value={form.description ?? ''}
        onChange={(e) => setForm({ ...form, description: e.target.value })}
        placeholder="Descripción (opcional)"
        maxLength={500}
        disabled={loading}
      />
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <button type="submit" disabled={loading}>
        {loading ? 'Generando...' : 'Generar Video'}
      </button>
    </form>
  );
}
```

---

## 8. Módulo de YouTube

### `src/api/youtube.api.ts`

```typescript
import api from './client';
import type { YouTubeUpload, YouTubePublishRequest } from '../types/youtube.types';

export const publishToYouTube = async (
  data: YouTubePublishRequest
): Promise<YouTubeUpload> => {
  const { data: upload } = await api.post<YouTubeUpload>('/api/youtube/publish', data);
  return upload;
};

export const getUploadStatus = async (uploadId: number): Promise<YouTubeUpload> => {
  const { data } = await api.get<YouTubeUpload>(`/api/youtube/uploads/${uploadId}`);
  return data;
};

export const listUploads = async (): Promise<YouTubeUpload[]> => {
  const { data } = await api.get<YouTubeUpload[]>('/api/youtube/uploads');
  return data;
};

export const deleteUpload = async (uploadId: number): Promise<void> => {
  await api.delete(`/api/youtube/uploads/${uploadId}`);
};
```

### Precondiciones para publicar en YouTube

Antes de llamar a `POST /api/youtube/publish`, el backend valida que:

1. El video tenga `status === "COMPLETED"` (no PENDING, PROCESSING ni ERROR)
2. El video tenga `videoUrl` disponible

Si estas condiciones no se cumplen, el backend responde `400 INVALID_ARGUMENT`.
**El frontend debe deshabilitar el botón de publicación si el video no está en estado COMPLETED.**

---

## 9. Manejo de Errores

### Estructura de error del backend

Todos los errores tienen este formato JSON:

```json
{
  "timestamp": "2024-04-26T14:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "prompt: El prompt es obligatorio",
  "path": "/api/videos/generate"
}
```

### Tabla de códigos de error

| Código                  | HTTP | Cuándo ocurre                                    | Acción en el frontend                     |
|-------------------------|------|--------------------------------------------------|-------------------------------------------|
| `VALIDATION_ERROR`      | 400  | Campos inválidos en el body                      | Mostrar `message` junto al campo          |
| `INVALID_ARGUMENT`      | 400  | Argumento de negocio inválido                    | Mostrar `message` como alerta             |
| `VIDEO_NOT_FOUND`       | 404  | El video no existe o no pertenece al usuario     | Redirigir al historial                    |
| `VIDEO_GENERATION_ERROR`| 500  | Fallo en la API de Vertex AI / Veo               | Mostrar error, permitir reintentar        |
| `YOUTUBE_PUBLISH_ERROR` | 502  | Fallo en la API de YouTube                       | Mostrar error de publicación              |
| `TOKEN_EXPIRADO`        | 401  | JWT expirado                                     | Redirigir al login, limpiar localStorage  |
| `TOKEN_INVALIDO`        | 401  | JWT malformado                                   | Redirigir al login, limpiar localStorage  |
| `NO_AUTENTICADO`        | 401  | Sin header Authorization                         | Redirigir al login                        |
| `ACCESO_DENEGADO`       | 403  | Sin permisos para el recurso                     | Mostrar mensaje 403                       |
| `ERROR_INTERNO`         | 500  | Error inesperado del servidor                    | Mostrar mensaje genérico                  |

### `src/hooks/useApi.ts` — Hook genérico para llamadas

```typescript
import { useState, useCallback } from 'react';
import type { ApiErrorResponse } from '../types/error.types';
import { AxiosError } from 'axios';

interface ApiState<T> {
  data: T | null;
  loading: boolean;
  error: ApiErrorResponse | null;
}

export function useApi<T>() {
  const [state, setState] = useState<ApiState<T>>({
    data: null,
    loading: false,
    error: null,
  });

  const execute = useCallback(async (fn: () => Promise<T>) => {
    setState({ data: null, loading: true, error: null });
    try {
      const data = await fn();
      setState({ data, loading: false, error: null });
      return data;
    } catch (err) {
      const axiosErr = err as AxiosError<ApiErrorResponse>;
      const error = axiosErr.response?.data ?? {
        timestamp: new Date().toISOString(),
        status: 0,
        error: 'Network Error',
        code: 'NETWORK_ERROR',
        message: 'No se pudo conectar con el servidor',
        path: '',
      };
      setState({ data: null, loading: false, error });
      throw error;
    }
  }, []);

  return { ...state, execute };
}
```

---

## 10. Gestión de Estado Global

### Inicializar el usuario al cargar la app

En `src/App.tsx`, carga el usuario al inicio si existe un token:

```typescript
import { useEffect } from 'react';
import { useAuthStore } from './store/authStore';
import AppRouter from './routes/AppRouter';

export default function App() {
  const fetchUser = useAuthStore((s) => s.fetchUser);

  useEffect(() => {
    fetchUser();
  }, [fetchUser]);

  return <AppRouter />;
}
```

---

## 11. Rutas y Navegación

### `src/routes/AppRouter.tsx`

```typescript
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import LoginPage from '../pages/LoginPage';
import DashboardPage from '../pages/DashboardPage';
import GeneratePage from '../pages/GeneratePage';
import HistoryPage from '../pages/HistoryPage';
import AuthCallback from '../modules/auth/AuthCallback';
import AuthError from '../modules/auth/AuthError';

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((s) => s.user);
  const loading = useAuthStore((s) => s.loading);

  if (loading) return <div>Cargando...</div>;
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Rutas públicas */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/auth/callback" element={<AuthCallback />} />
        <Route path="/auth/error" element={<AuthError />} />

        {/* Rutas protegidas */}
        <Route path="/dashboard" element={<PrivateRoute><DashboardPage /></PrivateRoute>} />
        <Route path="/generate" element={<PrivateRoute><GeneratePage /></PrivateRoute>} />
        <Route path="/history" element={<PrivateRoute><HistoryPage /></PrivateRoute>} />

        {/* Redirección raíz */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
```

### Rutas requeridas por el backend

El backend redirige a estas rutas específicas después del login — **deben existir**:

| Ruta                  | Cuándo ocurre                         |
|-----------------------|---------------------------------------|
| `/auth/callback?token=<JWT>` | Login exitoso con Google        |
| `/auth/error?reason=token_missing` | OAuth sin token             |
| `/auth/error?reason=server_error`  | Error interno del servidor  |

---

## 12. Polling de Estado de Video

La generación de videos es asíncrona. El backend llama a Veo y actualiza el estado
automáticamente cada 30 segundos. El frontend debe consultar periódicamente el
estado del video para mostrar el progreso.

**Ciclo de vida del video:**
```
PENDING → PROCESSING → COMPLETED (tiene videoUrl)
                     → ERROR (tiene errorMessage)
```

Los videos en estado PROCESSING se marcan como ERROR automáticamente si no se
completan en **2 horas**.

### `src/modules/video/useVideoPolling.ts`

```typescript
import { useState, useEffect, useRef } from 'react';
import { getVideo } from '../../api/videos.api';
import type { Video, VideoStatus } from '../../types/video.types';

const TERMINAL_STATES: VideoStatus[] = ['COMPLETED', 'ERROR'];
const POLL_INTERVAL_MS = 10_000; // 10 segundos (el backend hace polling cada 30s)

export function useVideoPolling(videoId: number | null) {
  const [video, setVideo] = useState<Video | null>(null);
  const [loading, setLoading] = useState(false);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const stopPolling = () => {
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  };

  const fetchStatus = async () => {
    if (!videoId) return;
    try {
      const updated = await getVideo(videoId);
      setVideo(updated);
      if (TERMINAL_STATES.includes(updated.status)) {
        stopPolling();
      }
    } catch {
      stopPolling();
    }
  };

  useEffect(() => {
    if (!videoId) return;

    setLoading(true);
    fetchStatus().then(() => setLoading(false));

    intervalRef.current = setInterval(fetchStatus, POLL_INTERVAL_MS);

    return stopPolling;
  }, [videoId]);

  return { video, loading };
}
```

### Uso del hook en un componente

```typescript
import { useVideoPolling } from './useVideoPolling';

function VideoStatus({ videoId }: { videoId: number }) {
  const { video, loading } = useVideoPolling(videoId);

  if (loading) return <p>Consultando estado...</p>;
  if (!video) return null;

  return (
    <div>
      <p>Estado: {video.status}</p>

      {video.status === 'PROCESSING' && (
        <div>
          <span className="spinner" />
          <p>Generando tu video con IA... puede tardar hasta 2 minutos.</p>
        </div>
      )}

      {video.status === 'COMPLETED' && video.videoUrl && (
        <video src={video.videoUrl} controls />
      )}

      {video.status === 'ERROR' && (
        <p style={{ color: 'red' }}>Error: {video.errorMessage}</p>
      )}
    </div>
  );
}
```

---

## 13. UX — Estados y Feedback Visual

### Estados que cada vista debe manejar

| Estado      | Qué mostrar                                               |
|-------------|-----------------------------------------------------------|
| `loading`   | Spinner / skeleton / botón deshabilitado                  |
| `success`   | Datos renderizados / mensaje de éxito                     |
| `error`     | Mensaje del campo `message` del `ApiErrorResponse`        |
| `empty`     | Mensaje vacío si el array retornado está vacío            |

### Indicadores de estado de video recomendados

| `VideoStatus` | Color    | Texto sugerido                         |
|---------------|----------|----------------------------------------|
| `PENDING`     | Gris     | En cola                                |
| `PROCESSING`  | Azul     | Generando con IA...                    |
| `COMPLETED`   | Verde    | Listo                                  |
| `ERROR`       | Rojo     | Error en la generación                 |

### Indicadores de publicación YouTube

| `YouTubeUploadStatus` | Color    | Texto sugerido       |
|-----------------------|----------|----------------------|
| `PENDING`             | Gris     | Pendiente            |
| `PUBLISHING`          | Azul     | Subiendo a YouTube...  |
| `PUBLISHED`           | Verde    | Publicado            |
| `FAILED`              | Rojo     | Error al publicar    |
| `DELETED`             | Naranja  | Eliminado            |

---

## 14. Páginas Requeridas

### `/login` — LoginPage

- Botón "Iniciar sesión con Google"
- Al hacer clic: llama `getGoogleLoginUrl()` → redirige `window.location.href = url`
- No tiene formulario de email/password (solo OAuth2)

```typescript
import { getGoogleLoginUrl } from '../api/auth.api';

export default function LoginPage() {
  const handleLogin = async () => {
    const url = await getGoogleLoginUrl();
    window.location.href = url;
  };

  return (
    <div>
      <h1>Social Video AI</h1>
      <button onClick={handleLogin}>Iniciar sesión con Google</button>
    </div>
  );
}
```

### `/dashboard` — DashboardPage

- Muestra nombre y foto del usuario (`GET /api/auth/me`)
- Acceso rápido a "Generar video" e "Historial"
- Botón de cierre de sesión (elimina token de localStorage)

### `/generate` — GeneratePage

- Formulario con campos: `title`, `prompt`, `description`
- Al enviar: llama `generateVideo()` → muestra el estado del video creado
- Usa `useVideoPolling` para mostrar el progreso en tiempo real
- Cuando el video esté `COMPLETED`, muestra el player y botón para publicar en YouTube

### `/history` — HistoryPage

- Llama `listVideos()` al montar
- Muestra lista de videos con: título, estado, fecha de creación, botón de ver
- Para videos `COMPLETED`: botón de reproducir y botón "Publicar en YouTube"
- Para videos en `PROCESSING`: badge animado

---

## 15. Checklist de Integración

Antes de considerar el frontend como completo, verifica:

### Autenticación
- [ ] Ruta `/auth/callback` existe y captura el `?token=` de la URL
- [ ] Ruta `/auth/error` existe y muestra el `?reason=`
- [ ] Token se guarda en `localStorage` con la clave `jwt_token`
- [ ] Interceptor de Axios agrega `Authorization: Bearer <token>` en cada request
- [ ] Interceptor de Axios redirige a `/login` cuando el código es `TOKEN_EXPIRADO` o `TOKEN_INVALIDO`
- [ ] Logout elimina el token y redirige al login

### Videos
- [ ] Formulario valida `title` (max 200) y `prompt` (5-1000) antes de enviar
- [ ] Después de crear un video, el componente muestra el estado inicial (`PENDING` o `PROCESSING`)
- [ ] Polling activo mientras el video esté en `PENDING` o `PROCESSING`
- [ ] Polling se detiene cuando el estado es `COMPLETED` o `ERROR`
- [ ] Video `COMPLETED` muestra el player de video con `videoUrl`
- [ ] Video `ERROR` muestra `errorMessage`

### YouTube
- [ ] Botón "Publicar en YouTube" solo habilitado si `status === 'COMPLETED'`
- [ ] Modal de publicación incluye `title`, `description` y selector de visibilidad
- [ ] Visibilidad tiene tres opciones: `PRIVATE`, `UNLISTED`, `PUBLIC`

### Manejo de errores
- [ ] Errores `400 VALIDATION_ERROR` muestran el campo específico con el mensaje
- [ ] Errores de red muestran un mensaje genérico (no exponer detalles técnicos)
- [ ] Todos los botones de submit se deshabilitan durante `loading`

### CORS
- [ ] La URL del frontend en Vercel (`https://social-video-ai.vercel.app`) está configurada
  en la variable de entorno `FRONTEND_URL` del backend en Render
- [ ] En desarrollo local, cambiar `VITE_API_URL` a `http://localhost:8080`
  y agregar `http://localhost:5173` al `FRONTEND_URL` del backend (o usar un proxy de Vite)

### Proxy de Vite para desarrollo local (evita CORS en dev)

En `vite.config.ts`:

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/login': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

Con el proxy activo, usa `VITE_API_URL=` (vacío) para que Axios apunte al mismo origen.

---

## Referencia Rápida de la API

### Endpoints públicos (sin token)

| Método | Ruta                       | Descripción                            |
|--------|----------------------------|----------------------------------------|
| GET    | `/api/auth/status`         | Health check del backend               |
| GET    | `/api/auth/google-url`     | URL para iniciar login con Google      |
| GET    | `/oauth2/authorization/google` | Inicia el flujo OAuth2 con Google  |

### Endpoints protegidos (requieren `Authorization: Bearer <token>`)

| Método | Ruta                         | Body / Params          | Respuesta              |
|--------|------------------------------|------------------------|------------------------|
| GET    | `/api/auth/me`               | —                      | `User`                 |
| POST   | `/api/videos/generate`       | `GenerateVideoRequest` | `Video` (201)          |
| GET    | `/api/videos`                | —                      | `Video[]`              |
| GET    | `/api/videos/{id}`           | path: `id`             | `Video`                |
| DELETE | `/api/videos/{id}`           | path: `id`             | 204 No Content         |
| POST   | `/api/youtube/publish`       | `YouTubePublishRequest`| `YouTubeUpload` (201)  |
| GET    | `/api/youtube/uploads`       | —                      | `YouTubeUpload[]`      |
| GET    | `/api/youtube/uploads/{id}`  | path: `id`             | `YouTubeUpload`        |
| DELETE | `/api/youtube/uploads/{id}`  | path: `id`             | 204 No Content         |
