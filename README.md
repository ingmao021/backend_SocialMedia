# Backend Social Media - Generación de Videos con Google AI

## Descripción del Proyecto

Backend para generación de videos utilizando Google Generative AI (Gemini Video API). Permite a los usuarios crear videos automáticamente mediante prompts de texto.

## Requisitos Previos

- Java 17 o superior
- Maven 3.6+
- PostgreSQL 12+
- Google Cloud Account con API Key para Generative AI

## Instalación y Setup

### 1. Clonar el Repositorio

```bash
git clone <repo-url>
cd backend_SocialMedia
```

### 2. Crear una Nueva Rama

```bash
git checkout -b feature/video-generation-ai
```

### 3. Configurar Variables de Entorno

Crear un archivo `.env` en la raíz del proyecto o configurar las variables en el sistema:

```env
# Base de Datos
DB_HOST=localhost
DB_PORT=5432
DB_NAME=social_media_db
DB_USERNAME=postgres
DB_PASSWORD=tu_password

# Google OAuth2
GOOGLE_CLIENT_ID=tu_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=tu_client_secret

# Google Generative AI
GOOGLE_GENERATIVE_AI_API_KEY=tu_api_key_aqui

# JWT
JWT_SECRET=tu_secreto_seguro_aqui
JWT_EXPIRATION=86400000

# URLs
FRONTEND_URL=http://localhost:3000
BACKEND_URL=http://localhost:8080

# Video Generation
GOOGLE_VIDEO_API_TIMEOUT=300
VIDEO_POLLING_INTERVAL=30
```

### 4. Crear Base de Datos

```sql
CREATE DATABASE social_media_db;
```

### 5. Compilar el Proyecto

```bash
mvn clean install
```

### 6. Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

La aplicación estará disponible en `http://localhost:8080`

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/example/backend_socialmedia/
│   │   ├── auth/               # Autenticación OAuth2 y JWT
│   │   ├── video/              # 🆕 Generación de videos
│   │   │   ├── application/    # Use cases
│   │   │   ├── domain/         # Modelos de dominio
│   │   │   └── infrastructure/ # Persistencia y Google AI
│   │   └── shared/             # Configuración y excepciones
│   └── resources/
│       ├── application.yaml
│       └── db/migration/       # Flyway migrations
└── test/
    └── java/                   # Tests
```

## Endpoints Principales

### Autenticación
- `POST /login/oauth2/code/google` - Login con Google
- `GET /api/auth/me` - Obtener usuario actual

### Generación de Videos (Nuevo)
- `POST /api/videos/generate` - Generar nuevo video
- `GET /api/videos` - Listar videos del usuario
- `GET /api/videos/{id}` - Obtener detalles de un video
- `DELETE /api/videos/{id}` - Eliminar un video

Ver [VIDEO_API_DOCUMENTATION.md](./VIDEO_API_DOCUMENTATION.md) para detalles completos.

## Configuración de Google AI Studio

1. Acceder a [Google AI Studio](https://aistudio.google.com/)
2. Crear un nuevo proyecto
3. Habilitar Gemini Video API
4. Generar API Key
5. Configurar en variable de entorno `GOOGLE_GENERATIVE_AI_API_KEY`

## Migraciones de Base de Datos

Las migraciones se ejecutan automáticamente con Flyway:

- `V1__create_users_table.sql` - Tabla de usuarios
- `V2__create_oauth_tokens_table.sql` - Tabla de tokens OAuth
- `V3__create_generated_videos_table.sql` - 🆕 Tabla de videos generados

Para crear una nueva migración:
1. Crear archivo `V{N}__description.sql` en `src/main/resources/db/migration/`
2. Las migraciones se ejecutan automáticamente al iniciar la aplicación

## Monitoreo de Videos

El sistema incluye un servicio de polling automático que:
- Consulta el estado de videos en generación cada 30 segundos
- Actualiza automáticamente cuando se completan
- Maneja errores y tiempos de espera

Configuración:
```yaml
video-generation:
  polling-interval-seconds: 30
```

## Testing

```bash
# Ejecutar todos los tests
mvn test

# Ejecutar test de una clase específica
mvn test -Dtest=VideosControllerTest
```

## Deployment

### Docker

```dockerfile
# El Dockerfile ya está incluido
docker build -t backend-socialmedia .
docker run -p 8080:8080 \
  -e DB_HOST=postgres \
  -e GOOGLE_GENERATIVE_AI_API_KEY=tu_api_key \
  backend-socialmedia
```

### Render/Heroku

1. Configurar variables de entorno en la plataforma
2. Deploy usando Git
3. Ejecutar migraciones automáticamente

## Troubleshooting

### Error: "API key no configurado"
- Verificar que `GOOGLE_GENERATIVE_AI_API_KEY` está configurado
- Validar que la API Key es válida en Google AI Studio

### Error: "Base de datos no responde"
- Verificar que PostgreSQL está corriendo
- Validar credenciales en `application.yaml`

### Videos no se actualizan
- Verificar logs: `tail -f logs/app.log`
- Confirmar que `@EnableScheduling` está en `BackendSocialMediaApplication`

## Contribution

1. Crear rama: `git checkout -b feature/tu-feature`
2. Commit cambios: `git commit -am 'Agregar feature'`
3. Push a rama: `git push origin feature/tu-feature`
4. Abrir Pull Request

## Licencia

Este proyecto es privado.

## Contacto

Para preguntas o soporte, contactar al equipo de desarrollo.
