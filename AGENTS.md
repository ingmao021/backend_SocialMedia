# AGENTS.md - Guía para Agentes de IA en Backend SocialMedia

## Arquitectura General
Este es un backend Spring Boot para una aplicación de redes sociales que genera videos con IA y los publica en YouTube. La arquitectura sigue Clean Architecture con capas: domain, application, infrastructure.

- **Autenticación**: OAuth2 con Google, JWT propios para sesiones.
- **Generación de Videos**: Usa Google Generative AI para crear videos basados en prompts.
- **Publicación en YouTube**: OAuth2 para subir videos generados.
- **Base de Datos**: PostgreSQL con Flyway para migraciones.
- **Despliegue**: Render.

## Componentes Principales
- `auth/`: Gestión de usuarios y autenticación OAuth2/JWT.
- `video/`: Generación y gestión de videos (prompt → IA → polling → completado).
- `youtube/`: Publicación de videos en YouTube via API.
- `shared/`: Utilidades comunes (config, persistence, utils).

## Flujos Críticos
- **Login**: OAuth2 Google → JWT generado → almacenado en DB.
- **Generar Video**: Prompt validado → API Google → polling asíncrono → videoUrl actualizado.
- **Publicar en YouTube**: Verificar video COMPLETED → obtener URL → OAuth token → subir.

## Patrones Específicos
- **UserDetails Custom**: `CustomUserDetails` incluye `userId` para acceso directo desde SecurityContext.
- **Token Store**: `OAuthTokenStore` usa `Long` para userId (no String).
- **Validación**: Prompts no vacíos, longitud máxima 1000 chars.
- **Polling**: Servicio `@Scheduled` para actualizar estado de videos.
- **Errores**: Excepciones específicas, logging detallado.

## Configuraciones
- `application.yaml`: Desarrollo.
- `application-prod.yaml`: Producción (variables env requeridas: DB_*, GOOGLE_*, JWT_*, etc.).

## Comandos Comunes
- Compilar: `./mvnw compile`
- Ejecutar: `./mvnw spring-boot:run`
- Tests: `./mvnw test`
- Migraciones: Flyway automático en startup.

## Consideraciones de Seguridad
- JWT en headers Authorization.
- OAuth2 scopes específicos para YouTube.
- Tokens refresh no implementados (TODO).
- CORS configurado para frontend.

## Endpoints Principales
- `POST /api/videos/generate`: Generar video.
- `GET /api/videos`: Listar videos del usuario.
- `POST /api/youtube/publish`: Publicar video en YouTube.
- `GET /oauth2/authorization/google`: Iniciar OAuth.

## Errores Comunes
- Videos no COMPLETED fallan en publish.
- Tokens expirados requieren re-login.
- Prompts inválidos rechazados.

## Mejoras Pendientes
- Refresh tokens OAuth.
- Manejo de rate limits YouTube.
- Tests unitarios/integración.
