# Product Requirements Document (PRD)

## Backend Social Media Platform

---

## 1. Project Overview

**Project Name:** Social Media Video Backend  
**Project Type:** REST API Backend (Spring Boot 3.5)  
**Core Functionality:** Backend que permite a usuarios autenticarse con Google, generar videos mediante IA (Google Veo), y publicar contenido en YouTube.  
**Target Users:** Creadores de contenido y desarrolladores que integran funcionalidades de video social.

---

## 2. Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.5 |
| Language | Java 21 |
| Database | PostgreSQL |
| ORM | JPA / Hibernate |
| Auth | Google OAuth2 + JWT |
| Video Generation | Google Generative AI (Veo) |
| Video Publishing | YouTube Data API v3 |
| Migration | Flyway |
| Build | Maven |

---

## 3. User Roles

| Role | Description |
|------|------------|
| **Authenticated User** | Usuario con cuenta Google que puede generar y publicar videos |
| **System** | Componentes internos del backend |

---

## 4. Functional Requirements

### 4.1 Autenticación

| ID | Requirement | Priority |
|----|-------------|----------|
| AUTH-01 | Los usuarios deben poder autenticarse con su cuenta Google | Critical |
| AUTH-02 | El sistema debe generar un JWT tras autenticación exitosa | Critical |
| AUTH-03 | El sistema debe almacenar tokens OAuth para API de Google | Critical |
| AUTH-04 | Los endpoints protegidos requieren JWT válido | Critical |

### 4.2 Generación de Videos

| ID | Requirement | Priority |
|----|-------------|----------|
| VIDEO-01 | Los usuarios pueden solicitar generación de video con prompt | Critical |
| VIDEO-02 | El sistema debe enviar la solicitud a Google Veo API | Critical |
| VIDEO-03 | El sistema debe hacer polling del estado de generación | High |
| VIDEO-04 | Los usuarios pueden listar sus videos generados | High |
| VIDEO-05 | Los usuarios pueden obtener detalles de un video | High |
| VIDEO-06 | Los usuarios pueden eliminar un video | Medium |

### 4.3 Publicación a YouTube

| ID | Requirement | Priority |
|----|-------------|----------|
| YT-01 | Los usuarios pueden publicar videos a YouTube | Critical |
| YT-02 | El sistema debe configurar visibilidad del video (public/unlisted) | High |
| YT-03 | Los usuarios pueden listar sus publicaciones | Medium |
| YT-04 | Los usuarios pueden eliminar publicaciones | Medium |

---

## 5. API Endpoints

### 5.1 Autenticación

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/oauth2/authorization/google` | Iniciar flujo OAuth2 |
| GET | `/oauth2/callback` | Callback después de autorización |
| POST | `/auth/me` | Obtener usuario actual |

### 5.2 Videos

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/videos/generate` | Generar nuevo video |
| GET | `/api/videos` | Listar videos del usuario |
| GET | `/api/videos/{id}` | Obtener detalles de video |
| DELETE | `/api/videos/{id}` | Eliminar video |

### 5.3 YouTube

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/youtube/publish` | Publicar video a YouTube |
| GET | `/api/youtube/uploads` | Listar uploads a YouTube |
| GET | `/api/youtube/status/{id}` | Obtener estado de upload |
| DELETE | `/api/youtube/{id}` | Eliminar video de YouTube |

---

## 6. Data Model

### 6.1 User

```
- id: Long (PK)
- googleId: String (unique)
- email: String
- name: String
- pictureUrl: String
- createdAt: Timestamp
- updatedAt: Timestamp
```

### 6.2 Video

```
- id: Long (PK)
- userId: Long (FK)
- googleVideoId: String
- prompt: String
- status: Enum (PENDING, PROCESSING, COMPLETED, FAILED)
- resultUri: String
- errorMessage: String (nullable)
- createdAt: Timestamp
- updatedAt: Timestamp
```

### 6.3 YouTubeUpload

```
- id: Long (PK)
- userId: Long (FK)
- videoId: Long (FK)
- youtubeVideoId: String
- youtubeVideoUrl: String
- title: String
- description: String
- visibility: Enum (PUBLIC, UNLISTED)
- status: Enum (UPLOADING, PROCESSING, COMPLETED, FAILED)
- createdAt: Timestamp
- updatedAt: Timestamp
```

### 6.4 OAuthToken

```
- id: Long (PK)
- userId: Long (FK)
- accessToken: String
- refreshToken: String
- expiresAt: Timestamp
- scope: String
```

---

## 7. Non-Functional Requirements

| Requirement | Description |
|-------------|-------------|
| Performance | El sistema debe responder en < 2s para operaciones síncronas |
| Availability | El polling de video debe ejecutarse cada 30 segundos |
| Security | Tokens OAuth nunca deben ser logged o expuestos |
| Scalability | La arquitectura permite escalamiento horizontal |

---

## 8. Configuration (Environment Variables)

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | URL de PostgreSQL |
| `DATABASE_USER` | Usuario de base de datos |
| `DATABASE_PASSWORD` | Contraseña de base de datos |
| `JWT_SECRET` | Clave secreta para JWT |
| `GOOGLE_CLIENT_ID` | Client ID de Google OAuth |
| `GOOGLE_CLIENT_SECRET` | Client Secret de Google OAuth |
| `FRONTEND_URL` | URL del frontend para CORS |
| `GOOGLE_AI_API_KEY` | API Key para Google Generative AI |

---

## 9. Acceptance Criteria

### 9.1 Autenticación
- [ ] Usuario puede autenticarse con Google desde el frontend
- [ ] JWT se genera y retorna tras login exitoso
- [ ] Endpoints de /api/* retornan 401 sin JWT válido

### 9.2 Generación de Videos
- [ ] POST /api/videos/generate retorna 201 con video en estado PENDING
- [ ] Video cambia a COMPLETED cuando Google termina la generación
- [ ] GET /api/videos lista todos los videos del usuario

### 9.3 YouTube
- [ ] POST /api/youtube/publish sube video a YouTube
- [ ] Video aparece en canal de YouTube del usuario
- [ ] Visibility se configura correctamente

---

## 10. Project Structure

```
src/main/java/com/example/backend_socialmedia/
├── auth/                    # Autenticación y usuarios
│   ├── application/
│   ├── domain/
│   └── infrastructure/
├── video/                   # Generación de videos
│   ├── application/
│   ├── domain/
│   └── infrastructure/
├── youtube/                # Publicación YouTube
│   ├── application/
│   ├── domain/
│   └── infrastructure/
├── shared/                 # Configuración y utilitarios
│   ├── config/
│   ├── exception/
│   ├── persistence/
│   ├── utils/
│   └── web/
└── BackendSocialMediaApplication.java
```

---

## 11. Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter OAuth2 Client
- Spring Boot Starter Validation
- PostgreSQL Driver
- Flyway (migrations)
- JWT (jjwt)
- Google HTTP Client
- Google Auth Library
- Google Cloud Storage
- Lombok