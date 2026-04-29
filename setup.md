# SETUP.md — Generador de videos con IA (Veo 3.1 Lite)

Guía maestra para reestructurar, configurar y desplegar el sistema desde cero.

> **Stack**: Spring Boot 3.5 + Java 21 + PostgreSQL + Vertex AI (Veo 3.1 Lite) + Google Cloud Storage en backend. React 19 + Vite + TypeScript en frontend. Despliegue: Render (backend + DB) + Vercel (frontend).

---

## Tabla de contenidos

1. [Problemas detectados en el código original](#1-problemas-detectados-en-el-código-original)
2. [Decisiones de arquitectura](#2-decisiones-de-arquitectura)
3. [Diagrama de arquitectura](#3-diagrama-de-arquitectura)
4. [Modelo de datos](#4-modelo-de-datos)
5. [Estructura del backend reestructurado](#5-estructura-del-backend-reestructurado)
6. [Estructura del frontend reestructurado](#6-estructura-del-frontend-reestructurado)
7. [Requisitos previos](#7-requisitos-previos)
8. [Configuración de Google Cloud](#8-configuración-de-google-cloud)
9. [Configuración del backend (local)](#9-configuración-del-backend-local)
10. [Configuración del frontend (local)](#10-configuración-del-frontend-local)
11. [Configuración de PostgreSQL](#11-configuración-de-postgresql-en-render)
12. [Despliegue en Render (backend)](#12-despliegue-en-render-backend)
13. [Despliegue en Vercel (frontend)](#13-despliegue-en-vercel-frontend)
14. [Flujos críticos explicados](#14-flujos-críticos-explicados)
15. [Checklist final](#15-checklist-final)

---

## 1. Problemas detectados en el código original

### 1.1 Bug raíz del login con Google (el que reportaste)

El proyecto actual mezcla **dos sistemas de autenticación incompatibles** y por eso te termina expulsando al login:

| Archivo | Problema |
|---|---|
| `SecurityConfig.java` | Activa `oauth2Login()` (flujo de **redirect server-side** con sesión HTTP) **junto con** un `JwtAuthenticationFilter`. Son dos modelos contradictorios. |
| `OAuth2SuccessHandler.java` | Tras Google Auth, escribe los JWT en **cookies httpOnly** y hace `getRedirectStrategy().sendRedirect(...)` al frontend. Cookies cross-site (Render → Vercel) requieren `SameSite=None; Secure`, y Chrome/Safari las bloquean en muchos contextos. |
| `application.yaml` | Configura `oauth2.client.registration.google` con `redirect-uri: "{baseUrl}/login/oauth2/code/google"` apuntando al backend. El frontend nunca recibe el token directamente. |
| `LoginPage.tsx` | Hace `window.location.href = ${API_URL}/oauth2/authorization/google` → arranca el flujo redirect server-side, exactamente lo que dijiste que no querías. |
| `AuthCallback.tsx` | Confía en `// El backend ya seteó cookies HttpOnly` — esa asunción se rompe cuando la cookie no se envía por SameSite/secure cross-domain. |
| `client.ts` | `withCredentials: true` → depende de cookies cross-site. |

**Síntoma**: el flujo OAuth termina, el backend pone cookies, redirige al frontend, pero el navegador no envió/aceptó la cookie cross-site → `GET /api/auth/me` falla con 401 → frontend redirige al login. Loop.

**Fix**: eliminar completamente `oauth2Login()`. El frontend usa **Google Identity Services (GIS)** que devuelve un ID Token de Google directamente al navegador. El frontend lo manda a `POST /api/auth/google` con `{ idToken }`. El backend lo verifica con `GoogleIdTokenVerifier`, hace upsert en BD, y devuelve un JWT propio que el frontend guarda y envía en `Authorization: Bearer <jwt>`. **Cero cookies cross-site, cero sesiones.**

### 1.2 Otros problemas relevantes

| # | Problema | Impacto |
|---|---|---|
| 1 | **No existe registro/login con email + password.** `UserEntity` no tiene `password_hash`, no hay `POST /api/auth/register` ni `POST /api/auth/login`. | Contradice tu regla de negocio #1. |
| 2 | **No vincula cuentas automáticamente.** `GoogleAuthUseCase.execute()` hace `findByGoogleId` y, si no encuentra, crea un usuario nuevo. Si ya existía un usuario con ese email, se rompe por la constraint `UNIQUE(email)`. | Contradice tu regla #4. |
| 3 | **No hay endpoint `POST /api/auth/google` propiamente dicho.** Hay un `GoogleTokenVerifier` pero no se usa desde un controller — se usa el flujo redirect. | Lo que pediste explícitamente. |
| 4 | **`GenerateVideoRequest` no recibe `durationSeconds`.** En `GenerateVideoUseCase` está hardcodeado `5` y aspecto `16:9`. | Contradice tu regla de 4/6/8s. |
| 5 | **No hay validación de cuota** (máx 2 videos completados). | Contradice tu regla de cuota. |
| 6 | **No genera Signed URLs.** `GoogleGenerativeAiService` guarda directamente lo que retorna Vertex (que es un `gs://` URI), y el frontend intenta reproducirlo en `<video>` — eso no funciona. | El video nunca se reproduce en el frontend. |
| 7 | **Falta `storageUri` en parameters.** Sin él, Vertex devuelve los bytes en base64 (no los sube al bucket). El código intenta leer `gcsUri` pero Vertex no devuelve ese campo si no lo pediste. | El "guardado en bucket" no ocurre como esperas. |
| 8 | **Falta `resolution: "720p"` en parameters.** | No fuerzas la calidad fija. |
| 9 | **No existe endpoint `GET /api/videos/{id}/status`.** Solo `GET /api/videos/{id}` que retorna todo. | Pediste un endpoint de status específico. |
| 10 | **Falta endpoint `GET /api/users/me` y `PUT /api/users/me`.** | Tu regla de perfil. |
| 11 | **Falta upload de avatar a GCS.** | Tu regla de perfil. |
| 12 | **Módulo `youtube/` completo** (controller, entity, use cases, scopes en OAuth) que no está en los requisitos. | Ruido, riesgo, scopes peligrosos pidiéndose al usuario. |
| 13 | **Archivo `.backup`** dentro del paquete `google/`. | Higiene. |
| 14 | **`CsrfController` y `CookieCsrfTokenRepository`** no tienen sentido en una API REST stateless con JWT. | Ruido. |
| 15 | **Comentario en `application.yaml`**: `same-site: None / secure: false` para "desarrollo" — el navegador rechaza eso. | Bug latente. |
| 16 | **`logging.level.org.springframework.security.oauth2: DEBUG`** en producción. | Ruido en logs. |
| 17 | **Tests prácticamente inexistentes** (solo `BackendSocialMediaApplicationTests` autogenerado). | Pediste tests al menos en services críticos. |
| 18 | **Frontend no tiene formulario de login email/password ni de registro.** Solo botón "Continuar con Google". | Contradice tu UX. |
| 19 | **Frontend no tiene selector de duración.** | Contradice tu UX. |
| 20 | **Frontend tiene módulo `youtube/`** (PublishModal, UploadList) que no se necesita. | Ruido. |
| 21 | **Frontend usa `withCredentials: true`** en lugar de Authorization header. | Junto al bug #1. |
| 22 | **`tokeninfo` endpoint deprecado** en `GoogleTokenVerifier`. Google recomienda `GoogleIdTokenVerifier` (verificación local con claves públicas + caché), no llamar a `tokeninfo` en cada login. | Performance + dependencia de un endpoint legacy. |

---

## 2. Decisiones de arquitectura

### 2.1 Autenticación — JWT en `Authorization` header (no cookies)

**Decisión**: El JWT viaja en `Authorization: Bearer <token>` y el frontend lo guarda en `localStorage`.

**Trade-off considerado**:

| Opción | Pro | Contra |
|---|---|---|
| `localStorage` + `Authorization` header | Simple, funciona perfecto cross-domain (Render ↔ Vercel), sin CORS de cookies. | Vulnerable a XSS — si te comprometen el JS, te roban el token. |
| Cookie `httpOnly` cross-site | Inmune a XSS para leer el token. | Requiere `SameSite=None; Secure`, problemas de cookies en Safari/iOS, complica CORS. **Es exactamente lo que está roto hoy.** |

**Justificación**: en una SPA + API REST en dominios distintos, `localStorage` + `Authorization` header es el patrón recomendado mayoritariamente. El riesgo de XSS se mitiga sanitizando inputs y usando React (que escapa por defecto). Como tu app no maneja datos financieros sensibles ni operaciones bancarias, el balance favorece la opción simple.

### 2.2 Tokens — un solo JWT de 24h

**Decisión**: Un único access token con `exp = 24h`. **Sin refresh token.**

**Justificación**: tu producto es un demo/MVP académico. Un refresh token agrega complejidad (rotación, revocación, almacenamiento) sin beneficio real para tu caso. 24h es el balance entre UX (no relogarte cada hora) y seguridad. Si en el futuro necesitas access cortos + refresh, está documentado cómo migrar.

### 2.3 Google Sign-In — Google Identity Services (GIS) en frontend

**Decisión**: el frontend usa la librería oficial `@react-oauth/google` (wrapper sobre GIS). Devuelve un **ID Token de Google** al cliente. El cliente lo manda a `POST /api/auth/google { idToken }`. El backend lo verifica con `GoogleIdTokenVerifier` (de `google-api-client`), valida `aud == GOOGLE_CLIENT_ID`, extrae `sub`/`email`/`name`/`picture` y emite un JWT propio.

**Por qué NO el flujo `oauth2Login()` server-side**: porque tu backend y frontend están en dominios distintos. Sesiones HTTP + cookies cross-site son frágiles. GIS evita todo eso: el ID Token se obtiene en el navegador, viaja en el body, no depende de cookies.

### 2.4 Vinculación automática de cuentas

**Decisión**: en el endpoint `POST /api/auth/google`, el lookup es:

1. `findByGoogleId(sub)` → si existe, login.
2. Si no, `findByEmail(email)` → si existe (registrado con password), **vincular**: setear `google_id = sub`, completar `name`/`picture` si están vacíos, y emitir JWT.
3. Si no existe ninguno, crear usuario nuevo con `password_hash = NULL` y `google_id = sub`.

Modelo de datos: `password_hash` y `google_id` ambos **nullable**, con check constraint `password_hash IS NOT NULL OR google_id IS NOT NULL` (al menos un método de auth).

### 2.5 Polling vs SSE vs WebSockets para el estado de generación

**Decisión**: **Polling cada 5 segundos** desde el frontend al endpoint `GET /api/videos/{id}/status`. **Scheduler en backend cada 15 segundos** que actualiza videos en estado `PROCESSING` consultando Vertex AI.

| Opción | Pro | Contra |
|---|---|---|
| Polling | Simple, funciona detrás de cualquier proxy/load balancer, sin estado en backend. | Tráfico HTTP repetido, latencia ~intervalo. |
| SSE (Server-Sent Events) | Push real-time, simple. | Requiere conexión abierta; problemas en Render free tier (idle timeouts). |
| WebSockets | Bidireccional. | Overkill, complica auth. |

**Justificación**: Veo 3.1 Lite tarda 30s–3min. Un polling de 5s es muy aceptable, sin estado, sin abrir conexiones largas. El scheduler del backend desacopla el polling del frontend de la llamada real a Vertex (no quemamos cuota de Vertex con un usuario que recarga cada segundo).

### 2.6 Actualización del estado de generación — scheduler en backend

**Decisión**: Un `@Scheduled` en `VideoStatusUpdateJob` cada 15s recoge videos en `PROCESSING`, llama a Vertex, y actualiza estado en BD. El endpoint `GET /api/videos/{id}/status` solo lee de BD.

**Por qué no consultar Vertex en cada llamada al status**: si el frontend hace polling cada 5s y el backend a su vez consulta Vertex en cada llamada, son 12 calls/min a Vertex por usuario. Con el scheduler, son 4/min independientemente del número de usuarios pollando, y todos leen la BD.

### 2.7 Signed URLs

**Decisión**: cuando el video pasa a `COMPLETED`, el backend genera una **Signed URL V4** firmada por la service account, válida **7 días**, y la guarda en `videos.signed_url` con `videos.signed_url_expires_at`. Si la URL ya expiró cuando alguien lista videos, el backend la regenera automáticamente al servir el `GET`.

**Por qué Signed URL V4**: es el estándar actual de GCS, soporta firma con service account, y permite acceso temporal a un objeto privado sin hacer público el bucket.

### 2.8 Cuota — derivada por COUNT, no contador desnormalizado

**Decisión**: `COUNT(*) FROM videos WHERE user_id = ? AND status = 'COMPLETED'`. Si ≥2 → `403 QUOTA_EXCEEDED`.

**Justificación**: un campo `videos_generated` en `users` se desincroniza si un job falla a mitad de camino o si borras videos. La query es trivial con un índice en `(user_id, status)` que ya tienes (lo añadimos).

### 2.9 Service Account Key — JSON inline en variable de entorno

**Decisión**: Render permite variables de entorno multilínea. Subes el JSON entero como `GCP_SA_KEY_JSON` y el backend lo lee con `GoogleCredentials.fromStream(new ByteArrayInputStream(...))`.

**Trade-off**:

| Opción | Pro | Contra |
|---|---|---|
| JSON inline en env var | Render lo soporta nativamente, sin filesystem. | El JSON aparece en la consola de Render (con permisos de owner). |
| Secret File en Render | Más seguro de leer accidentalmente. | Render Free no soporta secret files; sí lo soportan los planes pagos. |
| Workload Identity Federation | Sin keys. | Render no es GKE/GCE; no aplica. |

**Justificación**: como Render Free no soporta secret files, JSON inline es la opción práctica. **Importante**: la SA debe tener solo los roles mínimos (`Vertex AI User`, `Storage Object Admin` sobre el bucket específico, no global).

### 2.10 Migraciones — Flyway

**Decisión**: Flyway. Ya está en tu `pom.xml`.

**Por qué Flyway sobre Liquibase**: SQL puro, más sencillo, ya está configurado, y para tu tamaño de proyecto Liquibase XML es overkill.

### 2.11 Estructura del backend — capas clásicas (controller → service → repository → entity)

**Decisión**: pasar de la arquitectura hexagonal/DDD actual (`domain`/`application`/`infrastructure`) a la estructura por capas que pediste explícitamente. Es más simple, más estándar para proyectos Spring Boot, y más rápido de mantener.

```
controller/  → DTOs in/out, validación, mapeos a service
service/     → lógica de negocio
repository/  → Spring Data JPA
entity/      → JPA entities
dto/         → Request/Response DTOs
mapper/      → Mappers (manuales, suficientes para este tamaño)
config/      → Security, CORS, Beans
exception/   → GlobalExceptionHandler + excepciones custom
security/    → JwtService, JwtAuthFilter, UserPrincipal
external/    → Clientes a Vertex AI y GCS
```

---

## 3. Diagrama de arquitectura

```mermaid
flowchart TB
    subgraph Client["Browser (Vercel)"]
        FE["React + Vite SPA<br/>localStorage: jwt"]
    end

    subgraph Render["Render"]
        BE["Spring Boot 3.5<br/>Java 21"]
        DB[("PostgreSQL")]
    end

    subgraph GCP["Google Cloud"]
        GIS["Google Identity Services<br/>(devuelve ID Token al cliente)"]
        VertexAI["Vertex AI<br/>Veo 3.1 Lite"]
        GCS["Cloud Storage<br/>Bucket de videos"]
    end

    FE -->|"1. ID Token"| GIS
    FE -->|"2. POST /api/auth/google { idToken }"| BE
    BE -->|"3. Verify ID Token"| GIS
    BE -->|"4. Bearer JWT propio"| FE

    FE -->|"5. POST /api/videos/generate<br/>+ Bearer JWT"| BE
    BE -->|"6. predictLongRunning<br/>+ storageUri=gs://bucket/..."| VertexAI
    VertexAI -->|"7. mp4 directo"| GCS
    BE -->|"8. Scheduler cada 15s<br/>poll operation status"| VertexAI
    BE -->|"9. Generate Signed URL V4"| GCS
    FE -->|"10. GET /api/videos/{id}/status<br/>polling cada 5s"| BE
    FE -->|"11. <video src=signedUrl>"| GCS

    BE <--> DB
```

---

## 4. Modelo de datos

### Tabla `users`

| Columna | Tipo | Constraints | Descripción |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE | |
| `name` | `VARCHAR(150)` | NOT NULL | |
| `password_hash` | `VARCHAR(255)` | NULLABLE | BCrypt. NULL si solo se registró con Google. |
| `google_id` | `VARCHAR(255)` | UNIQUE, NULLABLE | El `sub` de Google. NULL si solo se registró con email. |
| `avatar_url` | `TEXT` | NULLABLE | URL en GCS o URL de Google profile picture. |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT NOW() | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT NOW() | |

**Check constraint**: `CHECK (password_hash IS NOT NULL OR google_id IS NOT NULL)`.

**Índices**: PK en `id`, UNIQUE en `email`, UNIQUE en `google_id`.

### Tabla `videos`

| Columna | Tipo | Constraints | Descripción |
|---|---|---|---|
| `id` | `UUID` | PK, DEFAULT `gen_random_uuid()` | El `videoId` que devuelve la API al frontend. |
| `user_id` | `BIGINT` | NOT NULL, FK → users(id) ON DELETE CASCADE | |
| `prompt` | `TEXT` | NOT NULL | |
| `duration_seconds` | `SMALLINT` | NOT NULL, CHECK `IN (4, 6, 8)` | |
| `status` | `VARCHAR(20)` | NOT NULL, CHECK `IN ('PROCESSING','COMPLETED','FAILED')` | |
| `vertex_operation_name` | `TEXT` | NULLABLE | Para hacer polling al operation. |
| `gcs_uri` | `TEXT` | NULLABLE | `gs://bucket/path/file.mp4`. Se setea cuando termina. |
| `signed_url` | `TEXT` | NULLABLE | URL firmada V4 lista para `<video src>`. |
| `signed_url_expires_at` | `TIMESTAMPTZ` | NULLABLE | Para regenerar cuando esté cerca de expirar. |
| `error_message` | `TEXT` | NULLABLE | Solo si `status = FAILED`. |
| `created_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT NOW() | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL DEFAULT NOW() | |

**Índices**:
- `idx_videos_user_id` en `(user_id)`
- `idx_videos_user_status` en `(user_id, status)` — soporta el COUNT de cuota.
- `idx_videos_status` en `(status)` — soporta el scheduler que busca PROCESSING.
- `idx_videos_created_at` en `(created_at DESC)` — historial paginado.

> **Nota**: usar `UUID` como PK del video (en lugar de `BIGSERIAL`) es deliberado — pediste devolver un `videoId` UUID al frontend tras `POST /generate`. Así no expones IDs autoincrementales (enumeration attacks) y el ID es estable desde el primer momento.

### Migraciones Flyway

```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__create_videos_table.sql
└── V3__add_check_user_auth_method.sql
```

#### `V1__create_users_table.sql`

```sql
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    password_hash   VARCHAR(255),
    google_id       VARCHAR(255) UNIQUE,
    avatar_url      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

#### `V2__create_videos_table.sql`

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE videos (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    prompt                  TEXT NOT NULL,
    duration_seconds        SMALLINT NOT NULL CHECK (duration_seconds IN (4, 6, 8)),
    status                  VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSING','COMPLETED','FAILED')),
    vertex_operation_name   TEXT,
    gcs_uri                 TEXT,
    signed_url              TEXT,
    signed_url_expires_at   TIMESTAMPTZ,
    error_message           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_videos_user_id      ON videos(user_id);
CREATE INDEX idx_videos_user_status  ON videos(user_id, status);
CREATE INDEX idx_videos_status       ON videos(status);
CREATE INDEX idx_videos_created_at   ON videos(created_at DESC);
```

#### `V3__add_check_user_auth_method.sql`

```sql
ALTER TABLE users
ADD CONSTRAINT chk_users_auth_method
CHECK (password_hash IS NOT NULL OR google_id IS NOT NULL);
```

---

## 5. Estructura del backend reestructurado

```
backend/
├── pom.xml
├── Dockerfile
├── .env.example
├── src/main/java/com/socialvideo/
│   ├── SocialVideoApplication.java
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── service/JwtService.java
│   │   ├── service/GoogleIdTokenService.java
│   │   ├── dto/RegisterRequest.java
│   │   ├── dto/LoginRequest.java
│   │   ├── dto/GoogleLoginRequest.java
│   │   └── dto/AuthResponse.java
│   ├── user/
│   │   ├── controller/UserController.java
│   │   ├── service/UserService.java
│   │   ├── repository/UserRepository.java
│   │   ├── entity/User.java
│   │   ├── dto/UserResponse.java
│   │   └── dto/UpdateProfileRequest.java
│   ├── video/
│   │   ├── controller/VideoController.java
│   │   ├── service/VideoService.java
│   │   ├── service/VideoStatusUpdateJob.java
│   │   ├── repository/VideoRepository.java
│   │   ├── entity/Video.java
│   │   ├── entity/VideoStatus.java
│   │   ├── dto/GenerateVideoRequest.java
│   │   ├── dto/VideoResponse.java
│   │   └── dto/VideoStatusResponse.java
│   ├── external/
│   │   ├── vertex/VertexAiClient.java
│   │   ├── vertex/dto/PredictLongRunningRequest.java
│   │   ├── vertex/dto/OperationResponse.java
│   │   └── gcs/GcsService.java       # Sube avatares + genera Signed URLs
│   ├── security/
│   │   ├── SecurityConfig.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── UserPrincipal.java
│   │   └── CurrentUserResolver.java
│   ├── config/
│   │   ├── CorsConfig.java
│   │   ├── GoogleCloudConfig.java     # Beans GoogleCredentials + Storage
│   │   └── AppProperties.java         # @ConfigurationProperties("app.*")
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── ApiError.java
│       ├── ResourceNotFoundException.java
│       ├── QuotaExceededException.java
│       ├── InvalidCredentialsException.java
│       └── VideoGenerationException.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/
│       ├── V1__create_users_table.sql
│       ├── V2__create_videos_table.sql
│       └── V3__add_check_user_auth_method.sql
└── src/test/java/com/socialvideo/
    ├── auth/AuthServiceTest.java
    ├── auth/JwtServiceTest.java
    └── video/VideoServiceTest.java
```

### `pom.xml` — dependencias clave a agregar/quitar

**Agregar**:

```xml
<!-- Verificación de ID Tokens de Google -->
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.7.0</version>
</dependency>
```

**Quitar**:

```xml
<!-- spring-boot-starter-oauth2-client  ← no se usa flujo redirect -->
<!-- skills-lock.json y archivos .backup -->
```

### `application.yml` (perfil base)

```yaml
spring:
  application:
    name: social-video-backend
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  datasource:
    url: ${DATABASE_URL}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        jdbc:
          time_zone: UTC

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

server:
  port: ${PORT:8080}

app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173}
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: ${JWT_EXPIRATION_MS:86400000}   # 24h
  google:
    client-id: ${GOOGLE_CLIENT_ID}
  gcp:
    project-id: ${GCP_PROJECT_ID}
    location: ${GCP_LOCATION:us-central1}
    bucket: ${GCS_BUCKET_NAME}
    sa-key-json: ${GCP_SA_KEY_JSON}
  vertex:
    veo-model-id: ${VEO_MODEL_ID:veo-3.1-lite-generate-preview}
    polling-interval-seconds: 15
    job-timeout-minutes: 10
  signed-url:
    ttl-days: 7

logging:
  level:
    com.socialvideo: INFO
    org.springframework.web: INFO
    org.hibernate.SQL: WARN
```

### `application-dev.yml`

```yaml
logging:
  level:
    com.socialvideo: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
```

### `application-prod.yml`

```yaml
logging:
  level:
    com.socialvideo: INFO
    org.springframework: WARN
```

---

## 6. Estructura del frontend reestructurado

```
frontend/
├── package.json
├── vite.config.ts
├── vercel.json
├── .env.example
├── .env.development
├── .env.production
├── index.html
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── routes/AppRouter.tsx
    ├── pages/
    │   ├── LoginPage.tsx          # email+password + botón Google (GIS)
    │   ├── RegisterPage.tsx       # nombre/email/password/confirm
    │   ├── DashboardPage.tsx      # input prompt + selector duración + historial
    │   └── ProfilePage.tsx        # nombre + avatar
    ├── components/
    │   ├── ProtectedRoute.tsx
    │   ├── Header.tsx             # avatar + dropdown logout/profile
    │   ├── VideoCard.tsx
    │   ├── VideoPlayer.tsx
    │   └── GenerateVideoForm.tsx  # incluye selector 4/6/8s
    ├── contexts/AuthContext.tsx   # user, login, register, googleLogin, logout
    ├── hooks/
    │   ├── useAuth.ts
    │   └── useVideoPolling.ts     # polling 5s del status
    ├── services/
    │   ├── apiClient.ts           # axios con interceptor JWT + 401 handler
    │   ├── authService.ts
    │   ├── userService.ts
    │   └── videoService.ts
    ├── types/
    │   ├── auth.types.ts
    │   ├── user.types.ts
    │   └── video.types.ts
    └── utils/
        └── tokenStorage.ts
```

### `package.json` — dependencias clave

**Agregar**:

```json
"@react-oauth/google": "^0.12.1"
```

**Quitar todo lo relacionado con YouTube**: `youtube.types.ts`, `youtube.api.ts`, `UploadList.tsx`, `PublishModal.tsx`.

### `services/apiClient.ts` — patrón correcto

```ts
import axios from 'axios';
import { getToken, clearToken } from '../utils/tokenStorage';

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: { 'Content-Type': 'application/json' },
  // OJO: NO usar withCredentials. Usamos Authorization header.
});

apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      clearToken();
      window.location.href = '/login';
    }
    return Promise.reject(err);
  },
);
```

### `contexts/AuthContext.tsx` — esqueleto

```tsx
import { createContext, useEffect, useState } from 'react';
import { authService } from '../services/authService';
import { userService } from '../services/userService';

export const AuthContext = createContext<AuthCtx>({} as AuthCtx);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (authService.getToken()) {
      userService.getMe().then(setUser).finally(() => setLoading(false));
    } else setLoading(false);
  }, []);

  const login = async (email: string, password: string) => {
    const { token, user } = await authService.login(email, password);
    authService.saveToken(token);
    setUser(user);
  };

  const register = async (data: RegisterRequest) => {
    const { token, user } = await authService.register(data);
    authService.saveToken(token);
    setUser(user);
  };

  const googleLogin = async (idToken: string) => {
    const { token, user } = await authService.googleLogin(idToken);
    authService.saveToken(token);
    setUser(user);
  };

  const logout = () => {
    authService.clearToken();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, googleLogin, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
```

### `pages/LoginPage.tsx` — botón con GIS

```tsx
import { GoogleLogin } from '@react-oauth/google';
import { useAuth } from '../hooks/useAuth';
// ...

<GoogleLogin
  onSuccess={(cred) => {
    if (cred.credential) googleLogin(cred.credential);
  }}
  onError={() => toast.error('Error con Google')}
/>
```

`main.tsx` envuelve la app en:

```tsx
<GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
  <App />
</GoogleOAuthProvider>
```

---

## 7. Requisitos previos

| Herramienta | Versión |
|---|---|
| Java | 21 (JDK) |
| Maven | 3.9+ (o usa el `./mvnw` incluido) |
| Node.js | 20.x LTS |
| npm | 10.x |
| Docker | (opcional, para Postgres local) |
| Cuenta de Google Cloud | con billing habilitado |
| Cuenta de Render | https://render.com |
| Cuenta de Vercel | https://vercel.com |

---

## 8. Configuración de Google Cloud

### 8.1 Proyecto y APIs

1. Ir a https://console.cloud.google.com → crear proyecto (o usar uno existente).
2. Anotar el **Project ID** (ej. `social-video-ai-prod`).
3. Habilitar billing en el proyecto (Vertex AI requiere billing activo).
4. Habilitar APIs:
    - **Vertex AI API**
    - **Cloud Storage API**
    - **Identity Toolkit API** (para verificación de ID Tokens)
    - Comando rápido (gcloud CLI):
      ```bash
      gcloud services enable aiplatform.googleapis.com \
                             storage.googleapis.com \
                             identitytoolkit.googleapis.com \
                             --project=YOUR_PROJECT_ID
      ```

### 8.2 Bucket de Cloud Storage

1. Ir a **Cloud Storage → Buckets → Create**.
2. Nombre: `social-video-ai-videos-<sufijo-único>` (los nombres son globales).
3. Region: la **misma que Vertex AI** (recomiendo `us-central1`).
4. Storage class: **Standard**.
5. Access control: **Uniform** (recomendado).
6. Public access prevention: **Enforced** (queremos privado, accedemos vía Signed URLs).
7. Crear.
8. (Opcional) Lifecycle rule para auto-eliminar videos > 30 días si quieres ahorrar costos.

### 8.3 Service Account

1. **IAM & Admin → Service Accounts → Create**.
2. Nombre: `social-video-backend-sa`.
3. Asignar **roles MÍNIMOS**:
    - `Vertex AI User` (`roles/aiplatform.user`)
    - `Storage Object Admin` (`roles/storage.objectAdmin`) **scope al bucket** (no al proyecto):
        - Ir al bucket → **Permissions → Grant access**.
        - Principal: `social-video-backend-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com`.
        - Role: `Storage Object Admin`.
    - `Service Account Token Creator` (`roles/iam.serviceAccountTokenCreator`) **a sí misma** — necesario para firmar Signed URLs:
      ```bash
      gcloud iam service-accounts add-iam-policy-binding \
        social-video-backend-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com \
        --member="serviceAccount:social-video-backend-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
        --role="roles/iam.serviceAccountTokenCreator" \
        --project=YOUR_PROJECT_ID
      ```
4. **Keys → Add Key → JSON** → descargar el archivo `service-account-key.json`. **Guárdalo seguro y no lo subas al repo.** Ya está en `.gitignore`.

### 8.4 OAuth 2.0 — Consent Screen

1. **APIs & Services → OAuth consent screen**.
2. User type: **External** (a menos que uses Workspace).
3. App name: `Social Video AI`.
4. User support email: tu email.
5. Authorized domains: `vercel.app` (suficiente si usas el dominio gratuito de Vercel) y tu dominio custom si lo tienes.
6. Scopes: dejar los **default**: `openid`, `email`, `profile`. **No agregar nada más** — no necesitamos YouTube ni Drive.
7. Test users: agregarte mientras esté en modo Testing.
8. Guardar.

### 8.5 OAuth Client ID (Web application)

1. **APIs & Services → Credentials → Create Credentials → OAuth client ID**.
2. Type: **Web application**.
3. Name: `Social Video AI - Web Client`.
4. **Authorized JavaScript origins** (todos los dominios desde donde el frontend cargará el SDK de GIS):
   ```
   http://localhost:5173
   https://your-app.vercel.app
   ```
5. **Authorized redirect URIs**: **DEJAR VACÍO**. GIS no usa redirects en este flujo (One-Tap / Sign-In Button devuelve el ID Token al JS de la página).
6. Crear → anotar el **Client ID**. (No usaremos el Client Secret — no aplica para GIS en este flujo.)

> ⚠️ Si más adelante migras a un dominio custom (ej. `app.tudominio.com`), debes agregarlo a Authorized JavaScript origins. Cambios pueden tardar hasta 5 minutos en propagarse.

---

## 9. Configuración del backend (local)

### 9.1 Archivo `.env` (NO subir al repo)

Crea `backend/.env`:

```bash
# Postgres local
DATABASE_URL=jdbc:postgresql://localhost:5432/socialvideo
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# JWT
JWT_SECRET=cambia-esto-por-32-caracteres-min-ay-pon-algo-largo-y-aleatorio
JWT_EXPIRATION_MS=86400000

# Google OAuth Client ID (Web) — el de la sección 8.5
GOOGLE_CLIENT_ID=1234567890-abcdef.apps.googleusercontent.com

# GCP — Service Account
GCP_PROJECT_ID=social-video-ai-prod
GCP_LOCATION=us-central1
GCS_BUCKET_NAME=social-video-ai-videos-xxxx
# El JSON entero de la service account, en una sola línea o con \n escapados
GCP_SA_KEY_JSON={"type":"service_account","project_id":"...","private_key":"-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n","client_email":"social-video-backend-sa@...iam.gserviceaccount.com",...}

# Vertex
VEO_MODEL_ID=veo-3.1-lite-generate-preview

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:5173

# Profile
SPRING_PROFILES_ACTIVE=dev
```

> Cómo obtener `GCP_SA_KEY_JSON` en una sola línea:
> ```bash
> jq -c . service-account-key.json | pbcopy   # macOS
> jq -c . service-account-key.json | xclip    # Linux
> ```

### 9.2 Postgres local con Docker

```bash
docker run -d --name socialvideo-pg \
  -p 5432:5432 \
  -e POSTGRES_DB=socialvideo \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:16
```

### 9.3 Correr el backend

```bash
cd backend
./mvnw spring-boot:run
```

Debería levantar en `http://localhost:8080`. Verifica:

```bash
curl http://localhost:8080/health
# {"status":"UP"}
```

### 9.4 Generar JAR para producción

```bash
./mvnw clean package -DskipTests
java -jar target/social-video-backend-*.jar
```

---

## 10. Configuración del frontend (local)

### 10.1 `.env.development`

```bash
VITE_API_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=1234567890-abcdef.apps.googleusercontent.com
```

### 10.2 `.env.production` (Vercel lo sobrescribe)

```bash
VITE_API_URL=https://your-backend.onrender.com
VITE_GOOGLE_CLIENT_ID=1234567890-abcdef.apps.googleusercontent.com
```

### 10.3 Comandos

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
npm run build    # genera dist/
npm run preview  # sirve dist/ localmente
```

---

## 11. Configuración de PostgreSQL en Render

1. Render Dashboard → **New +** → **PostgreSQL**.
2. Name: `social-video-db`.
3. Region: la más cercana a tu backend (ej. `Oregon` si tu backend está en Oregon).
4. PostgreSQL Version: **16**.
5. Plan: **Free** está bien para desarrollo.
6. Crear.
7. Cuando esté provisionado, copiar **Internal Database URL** (es la que usaremos desde el backend en Render — más rápida que la externa).
    - Formato: `postgresql://USER:PASSWORD@HOST/DBNAME`
    - Spring Boot necesita prefijo `jdbc:` → `jdbc:postgresql://USER:PASSWORD@HOST/DBNAME`
    - **Importante**: la "Internal Database URL" solo funciona desde otros servicios dentro de Render; para conectar localmente usa la "External Database URL".

> Flyway aplicará las migraciones automáticamente la primera vez que arranque el backend.

---

## 12. Despliegue en Render (backend)

### 12.1 Crear el servicio web

1. Render Dashboard → **New +** → **Web Service**.
2. Conectar tu repo de GitHub.
3. Name: `social-video-backend`.
4. Region: misma que la BD.
5. Branch: `main`.
6. Root Directory: vacío (o `backend/` si es monorepo).
7. Runtime: **Docker** (recomendado, usa el `Dockerfile` que adjunto abajo) o **Native** con Java.

#### Opción Docker (recomendada): `Dockerfile`

```dockerfile
# Stage 1 — build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# Stage 2 — runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
```

#### Opción Native: comandos

- Build Command: `./mvnw clean package -DskipTests`
- Start Command: `java -jar target/social-video-backend-*.jar`

### 12.2 Variables de entorno en Render

En la pestaña **Environment** del web service, agregar:

| Key | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | `jdbc:postgresql://...` (de la BD de Render) |
| `JWT_SECRET` | `<random 64 chars>` (generar con `openssl rand -base64 64`) |
| `JWT_EXPIRATION_MS` | `86400000` |
| `GOOGLE_CLIENT_ID` | tu Client ID de la sección 8.5 |
| `GCP_PROJECT_ID` | tu project ID |
| `GCP_LOCATION` | `us-central1` |
| `GCS_BUCKET_NAME` | tu bucket |
| `GCP_SA_KEY_JSON` | el JSON entero (Render acepta multilínea pegado tal cual) |
| `VEO_MODEL_ID` | `veo-3.1-lite-generate-preview` |
| `CORS_ALLOWED_ORIGINS` | `https://your-app.vercel.app` (sin slash final) |
| `PORT` | `8080` (Render lo setea pero por si acaso) |

### 12.3 Health check

En la sección **Health Check Path** poner `/health`. Render reiniciará el servicio si no responde.

### 12.4 Deploy

Hacer push a `main` → Render hace el deploy automáticamente. Mira los logs en tiempo real desde la UI.

---

## 13. Despliegue en Vercel (frontend)

1. Vercel Dashboard → **Add New → Project** → importar tu repo.
2. Framework Preset: **Vite** (autodetectado).
3. Root Directory: `frontend/` (si es monorepo) o vacío.
4. Build Command: `npm run build` (default).
5. Output Directory: `dist` (default).
6. Environment Variables:

| Key | Value |
|---|---|
| `VITE_API_URL` | `https://social-video-backend.onrender.com` (sin slash final) |
| `VITE_GOOGLE_CLIENT_ID` | tu Client ID de la sección 8.5 |

7. Deploy.

### 13.1 `vercel.json` (importante para SPAs con React Router)

```json
{
  "rewrites": [
    { "source": "/(.*)", "destination": "/" }
  ]
}
```

Sin esto, recargar `/dashboard` da 404.

### 13.2 Volver a Google Cloud y actualizar Authorized origins

Ahora que sabes la URL final de Vercel (ej. `https://social-video-ai.vercel.app`):

1. Volver a **APIs & Services → Credentials → tu OAuth Client ID**.
2. Agregar a **Authorized JavaScript origins**:
   ```
   https://social-video-ai.vercel.app
   ```
3. (Si tienes preview deployments en Vercel) agregarlos también: `https://social-video-ai-git-*.vercel.app` no es soportado por Google — tienes que agregar URLs concretas de previews que necesites probar, o testear solo el dominio de producción.
4. Volver a **Render → Environment → CORS_ALLOWED_ORIGINS** y poner la URL de Vercel.

---

## 14. Flujos críticos explicados

### 14.1 Flujo de registro con email + password

```mermaid
sequenceDiagram
    participant U as Usuario
    participant FE as Frontend (Vercel)
    participant BE as Backend (Render)
    participant DB as PostgreSQL

    U->>FE: Llena form (nombre, email, password)
    FE->>FE: Validación cliente (email regex, password >= 8)
    FE->>BE: POST /api/auth/register {name, email, password}
    BE->>BE: @Valid → ok
    BE->>DB: SELECT * FROM users WHERE email = ?
    alt email ya existe
        BE-->>FE: 409 EMAIL_ALREADY_REGISTERED
        FE-->>U: Mensaje de error
    else email libre
        BE->>BE: BCrypt.hashpw(password)
        BE->>DB: INSERT users (email, name, password_hash)
        BE->>BE: JwtService.generate(userId, email)
        BE-->>FE: 201 {token, user}
        FE->>FE: localStorage.setItem('jwt', token)
        FE->>FE: navigate('/dashboard')
    end
```

### 14.2 Flujo de login con email + password

Idéntico al anterior pero `POST /api/auth/login` valida con `BCrypt.checkpw()` y retorna 401 si falla.

### 14.3 Flujo de login con Google (GIS)

```mermaid
sequenceDiagram
    participant U as Usuario
    participant FE as Frontend
    participant GIS as Google Identity
    participant BE as Backend
    participant DB as PostgreSQL

    U->>FE: Click "Continuar con Google"
    FE->>GIS: GoogleLogin SDK abre popup
    U->>GIS: Selecciona cuenta y consiente
    GIS-->>FE: ID Token (JWT firmado por Google)
    FE->>BE: POST /api/auth/google {idToken}
    BE->>BE: GoogleIdTokenVerifier.verify(idToken)
    Note over BE: Valida firma con claves<br/>públicas de Google + aud + exp
    BE->>BE: Extrae sub, email, name, picture
    BE->>DB: SELECT WHERE google_id = sub
    alt encontrado
        BE->>BE: usuario existente
    else no
        BE->>DB: SELECT WHERE email = ?
        alt email existe (registrado con password)
            BE->>DB: UPDATE users SET google_id=sub, avatar_url=picture
            Note over BE,DB: Vinculación automática
        else no
            BE->>DB: INSERT users (email, name, google_id, avatar_url)
        end
    end
    BE->>BE: JwtService.generate(userId, email)
    BE-->>FE: 200 {token, user}
    FE->>FE: localStorage.setItem('jwt', token)
    FE->>FE: navigate('/dashboard')
```

### 14.4 Flujo de generación de video

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant BE as Backend
    participant DB as PostgreSQL
    participant V as Vertex AI
    participant GCS as Cloud Storage
    participant SCH as Scheduler (interno BE)

    FE->>BE: POST /api/videos/generate {prompt, durationSeconds}
    BE->>BE: @Valid: prompt no vacío, duration in [4,6,8]
    BE->>DB: SELECT COUNT(*) FROM videos WHERE user_id=? AND status='COMPLETED'
    alt count >= 2
        BE-->>FE: 403 QUOTA_EXCEEDED
    else count < 2
        BE->>DB: INSERT videos (user_id, prompt, duration, status='PROCESSING')
        BE->>V: POST predictLongRunning<br/>{instances:[{prompt}], parameters:{durationSeconds, resolution:"720p", storageUri:"gs://bucket/videos/{userId}/{videoId}/", sampleCount:1}}
        V-->>BE: 200 {name: "projects/.../operations/abc123"}
        BE->>DB: UPDATE videos SET vertex_operation_name=...
        BE-->>FE: 202 {videoId, status:"PROCESSING"}
    end

    loop cada 5 segundos
        FE->>BE: GET /api/videos/{videoId}/status<br/>+ Bearer JWT
        BE->>DB: SELECT status, signed_url FROM videos WHERE id=?
        BE-->>FE: {status, signedUrl?}
    end

    loop cada 15 segundos (independiente)
        SCH->>DB: SELECT WHERE status='PROCESSING'
        loop cada video
            SCH->>V: GET /v1/{operation_name}
            alt done=false
                Note over SCH: nada
            else done=true & success
                Note over V,GCS: Vertex ya subió<br/>el mp4 al bucket
                SCH->>SCH: Extraer gcsUri del response
                SCH->>GCS: Storage.signUrl(blob, 7d, V4)
                GCS-->>SCH: signedUrl
                SCH->>DB: UPDATE status='COMPLETED', gcs_uri, signed_url, signed_url_expires_at
            else done=true & error
                SCH->>DB: UPDATE status='FAILED', error_message
            end
        end
    end
```

### 14.5 Llamada exacta a Vertex AI (Veo 3.1 Lite)

**Endpoint**:
```
POST https://us-central1-aiplatform.googleapis.com/v1/projects/{PROJECT_ID}/locations/us-central1/publishers/google/models/veo-3.1-lite-generate-preview:predictLongRunning
```

**Headers**:
```
Authorization: Bearer <access_token de la SA>
Content-Type: application/json
```

**Body**:
```json
{
  "instances": [
    { "prompt": "<prompt del usuario>" }
  ],
  "parameters": {
    "durationSeconds": 8,
    "resolution": "720p",
    "aspectRatio": "16:9",
    "sampleCount": 1,
    "storageUri": "gs://social-video-ai-videos-xxxx/videos/123/abc-uuid/",
    "personGeneration": "allow_adult"
  }
}
```

**Response inmediata**:
```json
{
  "name": "projects/123/locations/us-central1/publishers/google/models/veo-3.1-lite-generate-preview/operations/a1b07c8e-7b5a-4aba-bb34-3e1ccb8afcc8"
}
```

**Polling**:
```
GET https://us-central1-aiplatform.googleapis.com/v1/{operation_name}
```

**Response cuando termina** (extracto):
```json
{
  "name": "projects/.../operations/...",
  "done": true,
  "response": {
    "@type": "type.googleapis.com/cloud.ai.large_models.vision.GenerateVideoResponse",
    "videos": [
      {
        "gcsUri": "gs://social-video-ai-videos-xxxx/videos/123/abc-uuid/sample_0.mp4",
        "mimeType": "video/mp4"
      }
    ]
  }
}
```

> **Importante**: el campo se llama `gcsUri` y vive bajo `response.videos[0].gcsUri` (no `response.predictions[0].gcsUri` como tu código actual asume).

### 14.6 Generación de Signed URL V4 desde Java

```java
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;

URL signedUrl = storage.signUrl(
    BlobInfo.newBuilder(bucketName, objectName).build(),
    7, TimeUnit.DAYS,
    Storage.SignUrlOption.withV4Signature(),
    Storage.SignUrlOption.httpMethod(HttpMethod.GET)
);
```

---

## 15. Checklist final

Antes de considerar el sistema "listo para probar":

### Google Cloud
- [ ] Proyecto creado con billing activo.
- [ ] APIs habilitadas: Vertex AI, Cloud Storage, Identity Toolkit.
- [ ] Bucket creado, region `us-central1`, public access prevention enforced.
- [ ] Service Account creada con roles: `Vertex AI User`, `Storage Object Admin` (scope al bucket), `Service Account Token Creator` (a sí misma).
- [ ] JSON key descargado y NUNCA comiteado.
- [ ] OAuth Consent Screen configurado.
- [ ] OAuth Client ID (Web) creado con Authorized JavaScript origins correctos (localhost + Vercel).

### Backend
- [ ] Variables de entorno completas en `.env` local.
- [ ] `./mvnw spring-boot:run` levanta sin errores.
- [ ] `GET http://localhost:8080/health` retorna 200.
- [ ] Flyway aplicó V1, V2, V3 (verificar tabla `flyway_schema_history`).
- [ ] Tests pasan: `./mvnw test`.

### Frontend
- [ ] `.env.development` con `VITE_API_URL=http://localhost:8080` y `VITE_GOOGLE_CLIENT_ID`.
- [ ] `npm run dev` carga http://localhost:5173 sin errores en consola.
- [ ] Botón Google muestra el popup de Google y devuelve un ID Token.

### Postman / E2E
- [ ] `POST /api/auth/register` con email nuevo → 201 + JWT.
- [ ] `POST /api/auth/login` con esas credenciales → 200 + JWT.
- [ ] `GET /api/users/me` con `Authorization: Bearer <jwt>` → 200 con datos del user.
- [ ] `POST /api/videos/generate` con `{prompt, durationSeconds:4}` → 202 + `videoId`.
- [ ] `GET /api/videos/{videoId}/status` cada 5s → eventualmente `COMPLETED` con `signedUrl`.
- [ ] Pegar `signedUrl` en navegador → reproduce el mp4.
- [ ] Generar 2 más → al 3er video debe responder 403 `QUOTA_EXCEEDED`.

### Producción
- [ ] BD Postgres en Render conectada.
- [ ] Backend desplegado en Render con todas las env vars.
- [ ] `https://your-backend.onrender.com/health` → 200.
- [ ] Frontend desplegado en Vercel con env vars.
- [ ] Vercel URL agregada a Authorized JavaScript origins en Google Cloud.
- [ ] Vercel URL agregada a `CORS_ALLOWED_ORIGINS` en Render.
- [ ] `vercel.json` con rewrites para SPAs.
- [ ] Flujo completo end-to-end funciona en producción.

---

## Apéndice — código clave de referencia

### `JwtService.java`

```java
@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generate(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("email", email)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expirationMs)))
            .signWith(key)
            .compact();
    }

    public Long extractUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public boolean isValid(String token) {
        try { parse(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                   .parseSignedClaims(token).getPayload();
    }
}
```

### `GoogleIdTokenService.java`

```java
@Service
public class GoogleIdTokenService {
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenService(@Value("${app.google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(clientId))
            .build();
    }

    public GooglePayload verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) throw new InvalidGoogleTokenException("Token inválido");
            Payload p = token.getPayload();
            return new GooglePayload(
                p.getSubject(),
                p.getEmail(),
                (String) p.get("name"),
                (String) p.get("picture")
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new InvalidGoogleTokenException("Error verificando token de Google", e);
        }
    }

    public record GooglePayload(String sub, String email, String name, String picture) {}
}
```

### `AuthService.googleLogin()` — vinculación automática

```java
@Transactional
public AuthResponse googleLogin(String idToken) {
    GooglePayload p = googleIdTokenService.verify(idToken);

    User user = userRepository.findByGoogleId(p.sub())
        .or(() -> userRepository.findByEmail(p.email()).map(existing -> {
            // Vincular cuentas
            existing.setGoogleId(p.sub());
            if (existing.getAvatarUrl() == null) existing.setAvatarUrl(p.picture());
            return userRepository.save(existing);
        }))
        .orElseGet(() -> userRepository.save(User.builder()
            .email(p.email())
            .name(p.name())
            .googleId(p.sub())
            .avatarUrl(p.picture())
            .build()));

    String jwt = jwtService.generate(user.getId(), user.getEmail());
    return new AuthResponse(jwt, UserMapper.toResponse(user));
}
```

### `SecurityConfig.java` — sin OAuth2 redirect

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CorsConfigurationSource corsSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors(c -> c.configurationSource(corsSource))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                .requestMatchers("/health", "/error").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(
                (req, res, ex) -> res.sendError(401, "Unauthorized")))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
```

### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = e.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a,b)->a));
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", "Datos inválidos", fields));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ApiError> handleQuota(QuotaExceededException e) {
        return ResponseEntity.status(403).body(new ApiError("QUOTA_EXCEEDED", e.getMessage(), null));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleAuth(InvalidCredentialsException e) {
        return ResponseEntity.status(401).body(new ApiError("INVALID_CREDENTIALS", e.getMessage(), null));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(404).body(new ApiError("NOT_FOUND", e.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception e) {
        log.error("Error no manejado", e);
        return ResponseEntity.status(500).body(new ApiError("INTERNAL_ERROR", "Error interno del servidor", null));
    }
}
```

---

**Fin de SETUP.md**. Ver `POSTMAN.md` para la colección de endpoints.