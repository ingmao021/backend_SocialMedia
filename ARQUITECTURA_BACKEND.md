# ARQUITECTURA BACKEND — Social Video AI
### Documentación Técnica de Arquitectura y Desarrollo

> **Versión**: 1.0  
> **Fecha**: Mayo 2026  
> **Stack**: Java 21 + Spring Boot 3.5 + PostgreSQL + Vertex AI (Veo 3.1 Lite) + Google Cloud Storage  
> **Autor del análisis**: Basado en análisis completo del código fuente real

---

## Tabla de Contenidos

1. [Introducción del Proyecto](#1-introducción-del-proyecto)
2. [Arquitectura del Proyecto](#2-arquitectura-del-proyecto)
3. [Explicación Completa de Carpetas y Archivos](#3-explicación-completa-de-carpetas-y-archivos)
4. [Flujo Completo del Backend](#4-flujo-completo-del-backend)
5. [Flujo de Autenticación](#5-flujo-de-autenticación)
6. [Flujo de Creación y Publicación de Videos](#6-flujo-de-creación-y-publicación-de-videos)
7. [Comunicación con el Frontend](#7-comunicación-con-el-frontend)
8. [Patrones de Diseño Utilizados](#8-patrones-de-diseño-utilizados)
9. [Programación Orientada a Objetos](#9-programación-orientada-a-objetos)
10. [Seguridad del Sistema](#10-seguridad-del-sistema)
11. [Manejo de Datos y Persistencia](#11-manejo-de-datos-y-persistencia)
12. [APIs Externas y Servicios Integrados](#12-apis-externas-y-servicios-integrados)
13. [Flujo Interno entre Módulos](#13-flujo-interno-entre-módulos)
14. [Configuración y Despliegue](#14-configuración-y-despliegue)
15. [Buenas Prácticas Implementadas](#15-buenas-prácticas-implementadas)
16. [Conclusión Técnica](#16-conclusión-técnica)

---

## 1. Introducción del Proyecto

### 1.1 Propósito del Backend

**Social Video AI** es un sistema backend diseñado para permitir a los usuarios generar videos cortos de forma automática utilizando inteligencia artificial. El usuario proporciona un prompt de texto, el sistema llama al modelo **Veo 3.1 Lite** de Google Vertex AI, el modelo genera el video y lo almacena en **Google Cloud Storage**, y finalmente el backend entrega al usuario una URL firmada y segura para reproducir el video directamente en el navegador.

El backend actúa como el núcleo central del sistema: gestiona la autenticación, controla el ciclo de vida de los videos, se comunica con servicios externos de IA y almacenamiento, y expone una API REST consumida por el frontend.

### 1.2 Problema que Resuelve

La generación de video con IA mediante Vertex AI es un proceso asíncrono de larga duración (entre 30 segundos y 5 minutos). El sistema resuelve los siguientes problemas técnicos complejos:

- **Asincronía**: Lanzar una operación larga en Vertex AI sin bloquear al cliente.
- **Polling inteligente**: Monitorear el estado de la operación sin sobrecargar la API de Vertex AI.
- **Seguridad de archivos**: Almacenar videos en un bucket privado de GCS y generar URLs temporales firmadas para acceso controlado.
- **Autenticación dual**: Soporte simultáneo de email/contraseña y Google OAuth (con vinculación automática de cuentas).
- **Cuota por usuario**: Limitar el número de videos que cada usuario puede generar.
- **Resiliencia**: Reparar automáticamente videos que quedaron en estado inconsistente al reiniciar el servidor.

### 1.3 Funcionalidades Principales

| Funcionalidad | Descripción |
|---|---|
| Registro de usuarios | Con email + contraseña (BCrypt) |
| Login con email/contraseña | Autenticación clásica con JWT |
| Login con Google OAuth | Verificación de ID Token de Google, emisión de JWT propio |
| Vinculación automática | Si un email existe con contraseña, se vincula con Google automáticamente |
| Gestión de perfil | Obtener/actualizar nombre, subir avatar a GCS |
| Generación de video con IA | Llamada a Veo 3.1 Lite via Vertex AI predictLongRunning |
| Polling de estado del video | Job programado que consulta Vertex AI cada 15 segundos |
| URLs firmadas | Signed URLs V4 de GCS con TTL de 7 días para reproducción segura |
| Cuota de videos | Límite por usuario de videos completados |
| Reparación de videos | Job al arrancar que rescata videos con archivos ya existentes en GCS |
| Health check | Endpoint de verificación del estado del servidor |

### 1.4 Stack Tecnológico Completo

| Capa | Tecnología | Versión | Rol |
|---|---|---|---|
| Lenguaje | Java | 21 (LTS) | Lenguaje principal del backend |
| Framework | Spring Boot | 3.5.12 | Framework de aplicación empresarial |
| Seguridad | Spring Security | 6.x | Autenticación y autorización |
| Persistencia ORM | Spring Data JPA + Hibernate | 6.x | Mapeo objeto-relacional |
| Base de datos | PostgreSQL | 16 | Base de datos relacional principal |
| Migraciones | Flyway | 11.8.2 | Control de versiones del schema SQL |
| JWT | JJWT (jjwt-api) | 0.12.6 | Generación y validación de tokens JWT |
| Auth Google | google-api-client | 2.7.0 | Verificación de ID Tokens de Google |
| Google Cloud Auth | google-auth-library-oauth2-http | 1.14.0 | Service Account credentials |
| Almacenamiento | Google Cloud Storage | 2.24.0 | Almacenamiento de videos y avatares |
| IA de video | Google Vertex AI (Veo 3.1 Lite) | - | Generación de video con IA |
| Build tool | Maven | 3.9+ | Gestión de dependencias y build |
| Reducción boilerplate | Lombok | 1.18.30 | Generación automática de getters/setters/builders |
| Variables de entorno | spring-dotenv | 4.0.0 | Carga de archivos .env en desarrollo local |
| Contenedores | Docker + Eclipse Temurin 21 Alpine | - | Despliegue en contenedores |
| Despliegue | Render (backend + BD) + Vercel (frontend) | - | Plataformas cloud |
| Testing | JUnit 5 + Mockito + Spring Security Test | - | Pruebas unitarias |
| Testing DB | H2 (en memoria) | - | Base de datos de pruebas |
| HTTP Client | Java 11+ HttpClient | 21 | Cliente HTTP nativo para Vertex AI |

---

## 2. Arquitectura del Proyecto

### 2.1 Tipo de Arquitectura Implementada

El backend implementa una **Arquitectura en Capas Modular por Dominio** (también conocida como *Package by Feature + Layered Architecture*). No es Clean Architecture ni Hexagonal estrictas, pero combina lo mejor de ambas con la practicidad de la arquitectura en capas clásica de Spring Boot.

La decisión fue explícita: pasar de una arquitectura hexagonal/DDD compleja (`domain/application/infrastructure`) a una estructura por capas agrupadas por módulo funcional. Esta elección favorece la **legibilidad**, la **velocidad de desarrollo** y la **mantenibilidad** para un proyecto académico/MVP.

### 2.2 Principio de Organización

En lugar de organizar por tipo técnico (todos los controllers juntos, todos los services juntos), el código se organiza **por dominio funcional**:

- `auth/` — Todo lo relacionado con autenticación
- `user/` — Todo lo relacionado con usuarios
- `video/` — Todo lo relacionado con videos
- `external/` — Clientes a servicios externos (Vertex AI, GCS)
- `security/` — Infraestructura de seguridad (filtros, anotaciones)
- `config/` — Configuración de la aplicación
- `exception/` — Manejo centralizado de errores

Dentro de cada módulo funcional se aplican las capas clásicas:

```
controller/ → service/ → repository/ → entity/
                ↑
               dto/
```

### 2.3 Diagrama de Arquitectura General

```text
┌─────────────────────────────────────────────────────────────────┐
│                    FRONTEND (React + Vite / Vercel)              │
│                  Authorization: Bearer <JWT>                     │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP REST
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│              SPRING SECURITY FILTER CHAIN                        │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  CORS Filter → JwtAuthenticationFilter → Authorization     │ │
│  └────────────────────────────────────────────────────────────┘ │
└───────────────────────────┬─────────────────────────────────────┘
                            │ peticiones autenticadas
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                     CAPA DE CONTROLLERS                          │
│  ┌──────────────┐  ┌───────────────┐  ┌───────────────────┐    │
│  │AuthController│  │UserController │  │ VideoController   │    │
│  └──────┬───────┘  └───────┬───────┘  └─────────┬─────────┘    │
└─────────┼────────────────── ┼───────────────────┼──────────────┘
          │                   │                   │
          ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                      CAPA DE SERVICIOS                           │
│  ┌──────────────┐  ┌───────────────┐  ┌───────────────────┐    │
│  │  AuthService │  │  UserService  │  │   VideoService    │    │
│  │  JwtService  │  │               │  │VideoStatusUpdate  │    │
│  │GoogleIdToken │  │               │  │   Job (Scheduler) │    │
│  │   Service    │  │               │  │VideoStatusPersist.│    │
│  └──────┬───────┘  └───────┬───────┘  └─────────┬─────────┘    │
└─────────┼────────────────── ┼───────────────────┼──────────────┘
          │                   │                   │
          ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                     CAPA DE REPOSITORIOS                         │
│       ┌────────────────┐       ┌────────────────────┐           │
│       │ UserRepository │       │  VideoRepository   │           │
│       └────────┬───────┘       └──────────┬─────────┘           │
└────────────────┼──────────────────────────┼─────────────────────┘
                 │                          │
                 ▼                          ▼
┌─────────────────────────────────────────────────────────────────┐
│               BASE DE DATOS — PostgreSQL (Render)                │
│          Tabla: users          Tabla: videos                     │
└─────────────────────────────────────────────────────────────────┘

                  Servicios externos (integrados en la capa Service)
                            │
          ┌─────────────────┼─────────────────┐
          ▼                 ▼                 ▼
  ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐
  │VertexAiClient│ │  GcsService  │ │GoogleIdTokenSvc  │
  │(Veo 3.1 Lite)│ │(Cloud Storage│ │(Google OAuth)    │
  └──────────────┘ └──────────────┘ └──────────────────┘
```

### 2.4 Ventajas de la Arquitectura Elegida

| Ventaja | Detalle |
|---|---|
| Localidad de código | Todo lo de `video/` está junto: controller, service, repository, entity, dto. No hay que buscar en múltiples carpetas. |
| Baja curva de aprendizaje | Cualquier desarrollador de Spring Boot reconoce la estructura inmediatamente. |
| Separación de responsabilidades | Cada capa tiene una responsabilidad única y bien definida. |
| Testabilidad | Los servicios son clases simples con dependencias inyectadas, fáciles de mockear. |
| Escalabilidad modular | Si el proyecto crece, cada módulo puede extraerse a un microservicio sin romper el resto. |
| Sin overhead de DDD | No hay interfaces de puerto/adaptador, no hay `use cases` por separado. El service es suficiente para la complejidad actual. |

---

## 3. Explicación Completa de Carpetas y Archivos

### 3.1 Estructura Raíz del Proyecto

```
backend_SocialMedia/
├── pom.xml                    ← Definición del proyecto Maven + dependencias
├── Dockerfile                 ← Build multi-etapa para despliegue en contenedor
├── setup.md                   ← Guía completa de configuración y despliegue
├── README.md                  ← Documentación de inicio rápido
├── PRD_frontend.md            ← Product Requirements para el frontend
├── docs/
│   ├── frontend-integration.md ← Contrato API para el equipo de frontend
│   └── PRD.md                 ← Product Requirements del proyecto
├── src/
│   ├── main/
│   │   ├── java/com/socialvideo/ ← Código fuente principal
│   │   └── resources/            ← Configuración y migraciones SQL
│   └── test/
│       ├── java/com/socialvideo/ ← Tests unitarios
│       └── resources/            ← application-test.yml
└── .mvn/wrapper/              ← Maven Wrapper (mvnw/mvnw.cmd)
```

### 3.2 Módulo Raíz — `com.socialvideo`

#### `SocialVideoApplication.java`

**Qué hace**: Punto de entrada de la aplicación Spring Boot. Contiene el método `main()` que arranca el servidor embebido Tomcat.

**Detalles clave**:
- `@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })`: Desactiva el auto-configuración del `UserDetailsService` porque el sistema implementa su propio mecanismo JWT sin usar el sistema de usuarios de Spring Security directamente.
- `@EnableScheduling`: Activa el soporte de tareas programadas (`@Scheduled`), necesario para el `VideoStatusUpdateJob` que hace polling a Vertex AI cada 15 segundos.
- `@EventListener(ApplicationReadyEvent.class)` en el método `onReady()`: Imprime en el log que la aplicación está lista, con el puerto configurado.

**Relaciones**: Es el origen de todo el contexto de Spring. Todos los beans son detectados automáticamente desde este paquete hacia abajo.

---

### 3.3 Módulo `auth/` — Autenticación

Responsable de todo el ciclo de autenticación: registro, login con email/contraseña, y login con Google OAuth.

#### `auth/controller/AuthController.java`

**Qué hace**: Exposición HTTP de los endpoints de autenticación. Es el punto de entrada para el cliente externo (frontend).

**Endpoints que expone**:
| Método | Path | HTTP Status de éxito |
|---|---|---|
| `POST` | `/api/auth/register` | `201 Created` |
| `POST` | `/api/auth/login` | `200 OK` |
| `POST` | `/api/auth/google` | `200 OK` |

**Cómo funciona**: Recibe los DTOs de request, los valida con `@Valid` (que activa las anotaciones de Bean Validation como `@NotBlank`, `@Email`, `@Size`), y delega toda la lógica a `AuthService`. No contiene ninguna lógica de negocio.

**Dependencias**: `AuthService`

#### `auth/service/AuthService.java`

**Qué hace**: Implementa la lógica de negocio de autenticación. Es el servicio más crítico del módulo.

**Método `register(RegisterRequest)`**:
1. Verifica si el email ya está registrado consultando `UserRepository.existsByEmail()`. Si existe, lanza `EmailAlreadyRegisteredException`.
2. Construye un objeto `User` con el patrón Builder de Lombok, codificando la contraseña con `BCrypt` (`PasswordEncoder.encode()`).
3. Persiste el usuario con `UserRepository.save()`.
4. Genera y retorna un `AuthResponse` con JWT + datos del usuario.

**Método `login(LoginRequest)`**:
1. Busca el usuario por email con `UserRepository.findByEmail()`.
2. Valida la contraseña usando `PasswordEncoder.matches()` contra el hash almacenado.
3. Si `passwordHash` es null (usuario registrado solo con Google), lanza `InvalidCredentialsException`.
4. Retorna `AuthResponse` con JWT.

**Método `googleLogin(String idToken)`**:
1. Llama a `GoogleIdTokenService.verify(idToken)` para validar el token de Google.
2. Implementa lógica de **vinculación automática de cuentas** en tres pasos:
   - Primero busca por `googleId`. Si existe → login directo.
   - Si no, busca por email. Si existe (usuario con contraseña) → vincula Google al usuario existente.
   - Si no existe → crea nuevo usuario con `googleId` y sin contraseña.

**Dependencias**: `UserRepository`, `PasswordEncoder`, `JwtService`, `GoogleIdTokenService`, `UserService`

#### `auth/service/JwtService.java`

**Qué hace**: Encapsula toda la lógica de JWT: generación, validación y extracción de datos. Es el servicio más técnico del módulo.

**Implementación**:
- Utiliza la librería **JJWT 0.12.6** (la más moderna).
- La clave HMAC-SHA256 se deriva de un `secret` configurado en variables de entorno con `Keys.hmacShaKeyFor()`.
- El token JWT contiene:
  - `subject`: el `userId` (Long convertido a String)
  - `claim("email")`: el email del usuario
  - `issuedAt`: timestamp de emisión
  - `expiration`: timestamp de expiración (configurable, por defecto 24 horas)

**Métodos públicos**:
- `generate(Long userId, String email)`: Crea un JWT firmado.
- `extractUserId(String token)`: Extrae el userId del subject del JWT.
- `extractEmail(String token)`: Extrae el email del claim.
- `isValid(String token)`: Valida la firma y la expiración. Retorna `false` en lugar de lanzar excepción.

**Por qué existe**: Centraliza toda la lógica JWT en un solo servicio reutilizable, siguiendo el principio DRY.

#### `auth/service/GoogleIdTokenService.java`

**Qué hace**: Verifica criptográficamente los ID Tokens de Google usando la librería oficial `google-api-client`.

**Cómo funciona**:
- Inicializa un `GoogleIdTokenVerifier.Builder` con el `clientId` de la aplicación (obtenido de las variables de entorno).
- El verificador valida: firma digital del token (contra las claves públicas de Google), la audiencia (`aud` debe ser el `clientId`), y la expiración.
- Extrae el `sub` (identificador único de Google), `email`, `name` y `picture` del payload.
- Retorna un record inmutable `GooglePayload` con esos datos.

**Ventaja vs tokeninfo**: Verificación local con caché de claves públicas, sin llamada a red adicional por cada login, más eficiente y sin dependencia del endpoint `tokeninfo` (deprecado).

#### DTOs del módulo `auth/`

| Clase | Tipo | Campos | Validaciones |
|---|---|---|---|
| `RegisterRequest` | Java Record | `name`, `email`, `password` | `@NotBlank`, `@Email`, `@Size(min=8)` |
| `LoginRequest` | Java Record | `email`, `password` | `@NotBlank`, `@Email` |
| `GoogleLoginRequest` | Java Record | `idToken` | `@NotBlank` |
| `AuthResponse` | Java Record | `token`, `user` (UserResponse) | — |

Los Records de Java (introducidos en Java 16) son perfectos para DTOs: inmutables, sin boilerplate, con `equals/hashCode/toString` automáticos.

---

### 3.4 Módulo `user/` — Gestión de Usuarios

Responsable de las operaciones sobre el perfil del usuario autenticado.

#### `user/controller/UserController.java`

**Qué hace**: Expone los endpoints de gestión de perfil. Todas las rutas requieren autenticación JWT.

**Endpoints**:
| Método | Path | Descripción |
|---|---|---|
| `GET` | `/api/users/me` | Obtiene el perfil del usuario autenticado |
| `PUT` | `/api/users/me` | Actualiza el nombre del usuario |
| `POST` | `/api/users/me/avatar` | Sube un avatar (multipart/form-data) |

**Anotación `@CurrentUser`**: Parámetro especial que inyecta automáticamente el `userId` del token JWT. El controller nunca toca el `SecurityContext` directamente: usa esta abstracción limpia.

#### `user/service/UserService.java`

**Qué hace**: Implementa la lógica de negocio sobre usuarios.

**Método `getMe(Long userId)`**: Busca el usuario en BD y lo mapea a `UserResponse`. El mapping incluye resolver la URL del avatar (puede ser `gs://` URI o URL de Google).

**Método `updateProfile(Long userId, UpdateProfileRequest)`**: Actualiza solo el campo `name`. Otros campos (email, contraseña) no son editables por esta vía.

**Método `uploadAvatar(Long userId, MultipartFile file)`**:
1. Valida el tipo MIME del archivo: solo `image/png` e `image/jpeg`.
2. Sube el archivo a GCS en la ruta `avatars/{userId}/avatar.{ext}` usando `GcsService.uploadAvatar()`.
3. Almacena el URI `gs://...` en el campo `avatar_url` del usuario (no la URL firmada).
4. En la lectura, el método `resolveAvatarUrl()` convierte el `gs://` a URL firmada temporalmente.

**Método `toUserResponse(User user)`** (público):
- Usado también por `AuthService` para no duplicar el mapeo.
- Calcula `videosGenerated` con un `COUNT` de videos en estado `COMPLETED`.
- Incluye `videosLimit` (configurado en `AppProperties`).
- Resuelve el avatar URL: si empieza con `gs://`, genera una Signed URL de 30 días; si es `https://` (foto de Google), la retorna directamente.

#### `user/entity/User.java`

**Qué hace**: Entidad JPA que mapea la tabla `users` de PostgreSQL.

**Campos relevantes**:
- `id` (Long, BIGSERIAL): Clave primaria autoincrementable. Los IDs de usuario no se exponen directamente en URLs públicas.
- `email` (String, UNIQUE): El identificador principal del usuario.
- `passwordHash` (String, NULLABLE): Puede ser `null` para usuarios que solo se autenticaron con Google.
- `googleId` (String, UNIQUE, NULLABLE): El `sub` de Google. `null` para usuarios sin cuenta Google.
- `avatarUrl` (TEXT): Almacena el `gs://` URI (para avatares subidos) o la URL de foto de perfil de Google.

**Anotaciones Lombok**: `@Builder`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` generan automáticamente todos los métodos necesarios.

**Timestamps automáticos**: `@CreationTimestamp` y `@UpdateTimestamp` de Hibernate gestionan `createdAt` y `updatedAt` automáticamente.

#### `user/repository/UserRepository.java`

Interfaz que extiende `JpaRepository<User, Long>`. Spring Data JPA genera la implementación automáticamente en tiempo de arranque.

**Métodos personalizados**:
- `findByEmail(String email)`: Utilizado en login y vinculación de cuentas Google.
- `findByGoogleId(String googleId)`: Utilizado en login con Google.
- `existsByEmail(String email)`: Verificación eficiente de duplicado antes del registro.

---

### 3.5 Módulo `video/` — Gestión de Videos

El módulo más complejo del sistema. Maneja el ciclo de vida completo de los videos generados con IA.

#### `video/controller/VideoController.java`

**Qué hace**: Expone los endpoints REST del ciclo de vida de videos.

**Endpoints**:
| Método | Path | HTTP | Descripción |
|---|---|---|---|
| `POST` | `/api/videos/generate` | `202 Accepted` | Inicia la generación de un video |
| `GET` | `/api/videos` | `200 OK` | Lista los videos del usuario (paginada) |
| `GET` | `/api/videos/{videoId}` | `200 OK` | Obtiene los datos completos de un video |
| `GET` | `/api/videos/{videoId}/status` | `200 OK` | Consulta solo el estado (para polling) |
| `DELETE` | `/api/videos/{videoId}` | `204 No Content` | Elimina un video |

**Por qué `202 Accepted` en generate**: El estándar HTTP para operaciones asíncronas es 202, no 201. Indica que la petición fue aceptada pero el procesamiento no ha terminado aún.

**`@CurrentUser Long userId`**: Inyecta el userId del JWT. Garantiza que un usuario solo puede ver/eliminar sus propios videos (la capa de service lo verifica).

#### `video/service/VideoService.java`

**Qué hace**: Orquesta el ciclo de vida de los videos desde la perspectiva del usuario.

**Método `generate(Long userId, GenerateVideoRequest)`**:
1. **Verifica cuota**: `videoRepository.countByUserIdAndStatus(userId, COMPLETED)` contra `AppProperties.getVideo().getMaxCompleted()`. Si se excede, lanza `QuotaExceededException`.
2. **Crea el registro en BD** con estado `PROCESSING` antes de llamar a Vertex AI. Esto garantiza que el video existe en BD incluso si la llamada a Vertex falla en el step siguiente.
3. **Llama a Vertex AI**: `vertexAiClient.predictLongRunning()`. Vertex devuelve inmediatamente un `operationName` (ID de la operación larga).
4. **Guarda el `operationName`** en el campo `vertexOperationName` del video. Este es el identificador necesario para hacer polling.
5. Retorna `VideoResponse` con estado `PROCESSING`.

**Método `getStatus(Long userId, UUID videoId)`**:
- Busca el video verificando que pertenece al usuario autenticado.
- Llama a `refreshSignedUrlIfNeeded()`: si la URL firmada está vencida o vence en menos de 1 hora, la regenera automáticamente.
- Retorna solo: `status`, `signedUrl`, `errorMessage`.

**Método `listByUser(Long userId, int page, int size)`**:
- Paginación con cap de 50 elementos por página para evitar consultas masivas.
- Regenera URLs firmadas expiradas al listar.

**Método `refreshSignedUrlIfNeeded(Video video)`** (privado):
- Solo aplica a videos en estado `COMPLETED` con `gcsUri`.
- Genera una nueva URL firmada si falta o vence en menos de 1 hora.
- Usa `GcsService.generateSignedUrl()` con TTL de 7 días.

#### `video/service/VideoStatusUpdateJob.java`

**Qué hace**: Tarea programada que corre en background cada 15 segundos y actualiza el estado de todos los videos en `PROCESSING`.

**Patrón de funcionamiento**:
```text
Cada 15 segundos:
  1. Busca todos los videos con status=PROCESSING en BD
  2. Para cada video:
     a. Verifica timeout (20 minutos) → si expiró, marca FAILED
     b. Llama a VertexAiClient.getOperation(operationName)
     c. Si done=false → sigue esperando
     d. Si done=true y error → delega a VideoStatusPersister.markAsFailed()
     e. Si done=true y success → delega a VideoStatusPersister.markAsCompleted()
```

**Diseño importante**: El job **no tiene transacción** (`@Transactional`). La llamada HTTP a Vertex AI se hace fuera de cualquier transacción. Las escrituras a BD se delegan a `VideoStatusPersister`, que sí tiene transacciones. Esto evita tener conexiones de BD abiertas durante las llamadas HTTP de larga duración.

**Detección de timeout**: `video.getCreatedAt().plus(timeoutMinutes, ChronoUnit.MINUTES).isBefore(Instant.now())`.

#### `video/service/VideoStatusPersister.java`

**Qué hace**: Persiste los cambios de estado de los videos en la base de datos con transacciones.

**Por qué existe como clase separada**: El `VideoStatusUpdateJob` es un `@Component` (no un `@Service` transaccional). Para poder usar `@Transactional` en los métodos de escritura, Spring requiere un bean separado donde se aplique la transacción correctamente mediante AOP.

**Método `markAsCompleted(Video, OperationResponse)`**:
1. Re-lee la entidad dentro de la transacción (evita problemas de estado detached de JPA).
2. Intenta extraer el `gcsUri` del campo `response.videos[0].uri` o `response.videos[0].gcsUri`.
3. Si el response no contiene URI (Vertex no siempre lo incluye), **busca el archivo en GCS** usando el prefijo `videos/{userId}/{videoId}/` y el método `GcsService.findVideoFile()`.
4. Normaliza el URI: si termina sin `.mp4`, añade `/sample_0.mp4`.
5. Genera la Signed URL V4 con `GcsService.generateSignedUrl()`.
6. **Solo marca como `COMPLETED` si tiene una Signed URL válida**. Si falla la generación, no cambia el estado.

**Método `markAsFailed(Video, String errorMessage)`**: Re-lee la entidad, setea `status=FAILED` y guarda el mensaje de error.

#### `video/entity/Video.java`

**Qué hace**: Entidad JPA que mapea la tabla `videos`.

**Campos clave**:
- `id` (UUID): Clave primaria UUID. Previene ataques de enumeración de IDs y es opaco para el cliente.
- `user` (ManyToOne LAZY): Relación con el usuario propietario. `FetchType.LAZY` para no cargar el usuario siempre que se cargue un video.
- `status` (enum VideoStatus): Estado almacenado como String en BD con `@Enumerated(EnumType.STRING)`.
- `vertexOperationName` (TEXT): Guardado para el polling de estado.
- `gcsUri` (TEXT): URI `gs://bucket/path/file.mp4` — la ubicación real en GCS.
- `signedUrl` (TEXT): URL firmada temporal para reproducción.
- `signedUrlExpiresAt` (Instant): Cuándo vence la URL firmada.

#### `video/entity/VideoStatus.java`

Enum con tres estados del ciclo de vida:
- `PROCESSING`: Video solicitado a Vertex AI, en generación.
- `COMPLETED`: Video generado y disponible para reproducción.
- `FAILED`: Error en la generación (timeout, error de Vertex AI, o archivo no encontrado).

#### `video/dto/GenerateVideoRequest.java`

Record inmutable con:
- `prompt`: String de hasta 2000 caracteres.
- `durationSeconds`: Short validado con la anotación custom `@AllowedDuration`.

#### `video/dto/AllowedDuration.java` y `AllowedDurationValidator.java`

**Qué hacen**: Una anotación de validación personalizada (Custom Bean Validation Constraint) que acepta solo los valores `4`, `6` u `8` para la duración del video.

- `@AllowedDuration`: Anotación meta-anotada con `@Constraint(validatedBy = AllowedDurationValidator.class)`.
- `AllowedDurationValidator`: Implementa `ConstraintValidator<AllowedDuration, Short>`. Define el `Set<Short> ALLOWED = {4, 6, 8}`.

**Por qué existe**: Vertex AI Veo 3.1 Lite solo soporta esas duraciones específicas. Si el usuario envía cualquier otro valor, el backend rechaza la petición con error 400 antes de llamar a Vertex AI.

#### `video/repository/VideoRepository.java`

Extiende `JpaRepository<Video, UUID>`. Métodos derivados por convención de nombres:

| Método | SQL generado |
|---|---|
| `countByUserIdAndStatus(Long, VideoStatus)` | `SELECT COUNT(*) FROM videos WHERE user_id=? AND status=?` |
| `findByStatus(VideoStatus)` | `SELECT * FROM videos WHERE status=?` |
| `findByIdAndUserId(UUID, Long)` | `SELECT * FROM videos WHERE id=? AND user_id=?` |
| `findByUserIdOrderByCreatedAtDesc(Long, Pageable)` | `SELECT * FROM videos WHERE user_id=? ORDER BY created_at DESC` |

El método `findByIdAndUserId` es crítico para la seguridad: garantiza que un usuario no pueda ver videos de otro usuario.

---

### 3.6 Módulo `external/` — Clientes a Servicios Externos

Encapsula la comunicación con servicios de terceros. Separa claramente las dependencias externas del resto del sistema.

#### `external/vertex/VertexAiClient.java`

**Qué hace**: Cliente HTTP que se comunica con la API de Google Vertex AI para generar videos con el modelo Veo 3.1 Lite.

**Tecnología de HTTP**: Utiliza `java.net.http.HttpClient` (cliente HTTP nativo de Java 11+), no RestTemplate ni WebClient. Esto elimina dependencias adicionales.

**Método `predictLongRunning(String prompt, int durationSeconds, Long userId, UUID videoId)`**:
- Refresca las credenciales de Google si están por expirar (`googleCredentials.refreshIfExpired()`).
- Construye la URL del endpoint: `https://{location}-aiplatform.googleapis.com/v1/projects/{projectId}/locations/{location}/publishers/google/models/{modelId}:predictLongRunning`.
- Construye el body JSON con `parameters`: `durationSeconds`, `resolution: "720p"`, `aspectRatio: "16:9"`, `sampleCount: 1`, `storageUri: "gs://bucket/videos/{userId}/{videoId}/"`, `personGeneration: "allow_adult"`.
- La `storageUri` le indica a Vertex AI dónde guardar el video directamente en GCS.
- Retorna el `operationName` (nombre completo de la operación larga).

**Método `getOperation(String operationName)`**:
- Detecta automáticamente el tipo de operación: si contiene `/publishers/google/models/`, es una operación de publisher model y usa `fetchPredictOperation` (POST). Si no, usa el endpoint estándar GET.
- Esta distinción fue necesaria porque Veo 3.1 Lite usa IDs UUID para sus operaciones, y el endpoint estándar solo acepta IDs numéricos.

**Método `fetchPredictOperation`** (publisher model): 
- `POST https://{location}-aiplatform.googleapis.com/v1/{modelPath}:fetchPredictOperation`
- Body: `{"operationName": "{operationName}"}`

**Método `getStandardOperation`** (LRO estándar):
- `GET https://{location}-aiplatform.googleapis.com/v1/projects/{p}/locations/{l}/operations/{id}`

#### `external/gcs/GcsService.java`

**Qué hace**: Abstrae todas las operaciones sobre Google Cloud Storage.

**Métodos**:

| Método | Descripción |
|---|---|
| `configureBucketCors()` | Configura CORS en el bucket para permitir que browsers carguen videos directamente |
| `findVideoFile(String prefix)` | Busca el primer archivo `.mp4` bajo un prefijo dado. Usado para reparar videos |
| `generateSignedUrl(String gcsUri)` | Genera Signed URL V4 para videos (TTL: 7 días) |
| `generateAvatarSignedUrl(String gcsUri)` | Genera Signed URL V4 para avatares (TTL: 30 días) |
| `uploadAvatar(Long userId, byte[], contentType, extension)` | Sube avatar a `avatars/{userId}/avatar.{ext}` |

**CORS en GCS**: El bucket usa CORS con origen `*` para GET/HEAD. Las Signed URLs ya son el mecanismo de autenticación, por lo que el wildcard es seguro.

**Signed URL V4**: Firmada con la Service Account, permite acceso temporal a un objeto privado del bucket. La librería `google-cloud-storage` hace la firma automáticamente usando las credenciales de la SA.

#### `external/vertex/dto/OperationResponse.java`

Record que mapea la respuesta de Vertex AI al consultar el estado de una operación:
- `name`: Nombre completo de la operación.
- `done`: Boolean que indica si terminó.
- `response`: `JsonNode` genérico (Jackson) que contiene el resultado cuando `done=true`.
- `error`: Record anidado `OperationError(int code, String message)` si falló.

El uso de `JsonNode` para `response` permite flexibilidad: la estructura del response de Vertex AI puede variar y se procesa dinámicamente en `VideoStatusPersister`.

---

### 3.7 Módulo `security/` — Infraestructura de Seguridad

Contiene la infraestructura de seguridad que atraviesa todo el sistema.

#### `security/SecurityConfig.java`

**Qué hace**: Configura el filtro de seguridad HTTP de Spring Security.

**Configuración aplicada**:
- `CORS`: Usa el bean `CorsConfigurationSource` definido en `CorsConfig`.
- `CSRF`: **Deshabilitado** — correcto para APIs REST stateless con JWT (no hay sesiones de browser que proteger con CSRF).
- `SessionManagement`: `STATELESS` — Spring Security no crea ni usa sesiones HTTP. Cada petición se autentica con el JWT.
- **Rutas públicas**: `POST /api/auth/**`, `/health`, `/error`, `OPTIONS /**` (preflight CORS).
- **Resto**: Requiere autenticación.
- `JwtAuthenticationFilter` se inserta **antes** del `UsernamePasswordAuthenticationFilter`.
- Si no hay JWT válido, retorna `401 Unauthorized`.
- Bean `PasswordEncoder`: `BCryptPasswordEncoder` con factor de coste por defecto (10 rondas).

#### `security/JwtAuthenticationFilter.java`

**Qué hace**: Filtro que intercepta cada petición HTTP, extrae el JWT del header `Authorization: Bearer`, lo valida y, si es válido, crea el objeto de autenticación en el `SecurityContext`.

**Flujo**:
1. Extrae el token del header `Authorization`.
2. Si hay token y es válido (`jwtService.isValid(token)`): crea un `UserPrincipal` con `userId` y `email`, y lo establece en `SecurityContextHolder`.
3. Llama a `filterChain.doFilter()` en todos los casos (con o sin token). Spring Security decide si permitir el acceso según las reglas de `SecurityConfig`.

Hereda de `OncePerRequestFilter`: garantiza que se ejecuta exactamente una vez por petición.

#### `security/UserPrincipal.java`

**Qué hace**: Implementación de `AbstractAuthenticationToken` que representa al usuario autenticado en el `SecurityContext`.

Contiene:
- `userId` (Long): El ID del usuario en BD.
- `email` (String): El email del usuario.
- Se marca como autenticado (`setAuthenticated(true)`) al crearse.

Extiende `AbstractAuthenticationToken` para ser compatible con el ecosistema de Spring Security.

#### `security/CurrentUser.java` y `CurrentUserResolver.java`

**`@CurrentUser`**: Anotación personalizada (`@Target(ElementType.PARAMETER)`) que se coloca en parámetros de métodos de controladores.

**`CurrentUserResolver`**: Implementa `HandlerMethodArgumentResolver` y `WebMvcConfigurer`. 
- `supportsParameter()`: Solo maneja parámetros de tipo `Long` anotados con `@CurrentUser`.
- `resolveArgument()`: Extrae el `UserPrincipal` del `SecurityContextHolder` y retorna su `userId`.
- `addArgumentResolvers()`: Se registra a sí mismo en el MVC framework.

**Por qué existe esta abstracción**: Los controllers nunca llaman directamente a `SecurityContextHolder.getContext().getAuthentication()`. Esto hace el código más limpio, más testeable, y desacoplado del mecanismo de seguridad.

---

### 3.8 Módulo `config/` — Configuración

#### `config/AppProperties.java`

**Qué hace**: Clase de configuración tipada que mapea las propiedades del `application.yml` bajo el prefijo `app.*`.

Usa `@ConfigurationProperties(prefix = "app")` de Spring Boot, que vincula automáticamente las propiedades YAML a los campos de la clase.

**Clases internas** (grupos de configuración):

| Clase | Prefijo YAML | Campos |
|---|---|---|
| `Cors` | `app.cors` | `allowedOrigins` |
| `Jwt` | `app.jwt` | `secret`, `expirationMs` |
| `Google` | `app.google` | `clientId` |
| `Gcp` | `app.gcp` | `projectId`, `location`, `bucket`, `saKeyJson` |
| `Vertex` | `app.vertex` | `veoModelId`, `pollingIntervalSeconds`, `jobTimeoutMinutes` |
| `SignedUrl` | `app.signed-url` | `ttlDays`, `avatarTtlDays` |
| `Video` | `app.video` | `maxCompleted` |

**Ventaja**: En lugar de usar `@Value("${app.gcp.project-id}")` disperso en el código, se inyecta una sola instancia de `AppProperties` y se accede como `appProperties.getGcp().getProjectId()`. Centralizado, tipado y fácil de testear.

#### `config/CorsConfig.java`

**Qué hace**: Define el bean `CorsConfigurationSource` usado por Spring Security.

**Configuración**:
- `allowedOrigins`: Lista de orígenes permitidos (del env `CORS_ALLOWED_ORIGINS`). Siempre incluye `localhost:5173` para desarrollo.
- `allowedMethods`: GET, POST, PUT, DELETE, OPTIONS.
- `allowedHeaders`: Authorization, Cache-Control, Content-Type.
- `allowCredentials: true`: Necesario para que el navegador envíe el header `Authorization`.
- `maxAge: 3600s`: Los preflight CORS se cachean por 1 hora.

#### `config/GoogleCloudConfig.java`

**Qué hace**: Define los beans de Google Cloud: `GoogleCredentials` y `Storage`.

- **`googleCredentials`**: Lee el JSON de la Service Account del env `GCP_SA_KEY_JSON` y crea las credenciales con scope `cloud-platform`.
- **`storage`**: Cliente oficial de GCS, configurado con las credenciales de la SA.
- **`@Lazy`**: Ambos beans se cargan solo cuando se necesitan (no al arrancar). Esto permite que la aplicación arranque incluso sin credenciales de GCS (útil para pruebas locales o desarrollo).

#### `config/GcsBucketInitializer.java`

**Qué hace**: Configura el CORS del bucket de GCS al arrancar la aplicación.

- Se ejecuta al evento `ApplicationReadyEvent` (cuando Spring está completamente levantado).
- Llama a `GcsService.configureBucketCors()`.
- Si falla (p.ej. sin credenciales en desarrollo local), solo emite un warning y no falla la aplicación.
- Es **idempotente**: puede correr en cada arranque sin efectos secundarios.

#### `config/VideoRepairJob.java`

**Qué hace**: Job de reparación que corre al arrancar la aplicación. Busca videos en estado `PROCESSING` o `FAILED` que ya tienen el archivo `.mp4` en GCS y los marca como `COMPLETED`.

**Escenario que resuelve**: Si el backend se reinicia mientras hay videos en `PROCESSING`, el scheduler puede perder el contexto. Este job recupera esos videos buscando los archivos en GCS por prefijo (`videos/{userId}/{videoId}/`).

**Flujo**:
1. Obtiene todos los videos en `PROCESSING` + `FAILED`.
2. Para cada uno, busca un archivo `.mp4` en GCS con el prefijo esperado.
3. Si lo encuentra, genera Signed URL y marca como `COMPLETED`.
4. Si no encuentra, lo deja en su estado actual.

#### `config/HealthController.java`

**Qué hace**: Endpoint `GET /health` que retorna `{"status":"UP"}`. Usado por Render para health checks y para verificar que el servidor está levantado.

---

### 3.9 Módulo `exception/` — Manejo de Errores

#### `exception/GlobalExceptionHandler.java`

**Qué hace**: Intercepta todas las excepciones lanzadas por los controllers y las convierte en respuestas HTTP estructuradas con el formato `ApiError`.

Usa `@RestControllerAdvice`, que combina `@ControllerAdvice` + `@ResponseBody`. Aplica a todos los controllers de la aplicación.

**Excepciones manejadas**:

| Excepción | HTTP Status | `code` |
|---|---|---|
| `MethodArgumentNotValidException` | `400 Bad Request` | `VALIDATION_ERROR` |
| `QuotaExceededException` | `403 Forbidden` | `QUOTA_EXCEEDED` |
| `InvalidCredentialsException` | `401 Unauthorized` | `INVALID_CREDENTIALS` |
| `EmailAlreadyRegisteredException` | `409 Conflict` | `EMAIL_ALREADY_REGISTERED` |
| `InvalidGoogleTokenException` | `401 Unauthorized` | `INVALID_GOOGLE_TOKEN` |
| `InvalidFileTypeException` | `400 Bad Request` | `INVALID_FILE_TYPE` |
| `ResourceNotFoundException` | `404 Not Found` | `NOT_FOUND` |
| `IllegalArgumentException` | `400 Bad Request` | `BAD_REQUEST` |
| `MaxUploadSizeExceededException` | `413 Payload Too Large` | `PAYLOAD_TOO_LARGE` |
| `VideoGenerationException` | `500 Internal Server Error` | `VIDEO_GENERATION_ERROR` |
| `Throwable` (fallback) | `500 Internal Server Error` | `INTERNAL_ERROR` |

El `@ExceptionHandler(MethodArgumentNotValidException.class)` recolecta todos los errores de validación por campo y los incluye en el mapa `fields` del `ApiError`.

#### `exception/ApiError.java`

Record que define el formato universal de errores de la API:
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Datos inválidos",
  "fields": {
    "email": "El email debe ser válido",
    "password": "La contraseña debe tener al menos 8 caracteres"
  }
}
```

`fields` es `null` excepto para `VALIDATION_ERROR`.

#### Excepciones Personalizadas

| Clase | Extiende | Propósito |
|---|---|---|
| `EmailAlreadyRegisteredException` | `RuntimeException` | Email ya registrado |
| `InvalidCredentialsException` | `RuntimeException` | Login fallido |
| `InvalidFileTypeException` | `RuntimeException` | Tipo de archivo no permitido |
| `InvalidGoogleTokenException` | `RuntimeException` | Token de Google inválido |
| `QuotaExceededException` | `RuntimeException` | Límite de videos alcanzado |
| `ResourceNotFoundException` | `RuntimeException` | Recurso no encontrado |
| `VideoGenerationException` | `RuntimeException` | Error con Vertex AI |

Todas extienden `RuntimeException` (unchecked), lo que permite lanzarlas sin `throws` en las firmas de métodos, manteniendo el código limpio.

---

### 3.10 Recursos y Configuración

#### `src/main/resources/application.yml`

Configuración base del sistema (aplica a todos los perfiles):

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}              # Conexión a PostgreSQL
    hikari:
      maximum-pool-size: 10           # Pool de conexiones
  jpa:
    hibernate.ddl-auto: validate      # Hibernate valida el schema pero no lo modifica
    open-in-view: false               # Evita antipatrón de sesiones JPA abiertas en la vista
  flyway:
    enabled: true                     # Flyway gestiona el schema
  servlet.multipart:
    max-file-size: 2MB                # Límite de avatares

server:
  port: ${PORT:8080}

app:
  jwt.expiration-ms: 86400000         # JWT de 24 horas
  vertex.polling-interval-seconds: 15  # Polling a Vertex AI
  vertex.job-timeout-minutes: 20       # Timeout de generación de video
  signed-url.ttl-days: 7              # TTL de URLs firmadas de video
  signed-url.avatar-ttl-days: 30      # TTL de URLs firmadas de avatar
  video.max-completed: 9999           # Cuota (desactivada temporalmente)
```

**`ddl-auto: validate`**: Hibernate verifica que el schema de BD coincide con las entidades, pero **no crea ni modifica tablas**. Flyway es el único responsable de los cambios de schema.

**`open-in-view: false`**: Desactiva el antipatrón de Spring que mantiene sesiones JPA abiertas durante el rendering. Correcto para APIs REST.

#### `application-dev.yml`

Solo añade más verbose en logs para desarrollo: `DEBUG` en el paquete propio y `TRACE` para los parámetros SQL de Hibernate.

#### `application-prod.yml`

Reduce el logging al mínimo necesario. Cambia `ddl-auto` a `none` (más eficiente en producción, ya que Flyway garantiza la consistencia).

#### `src/main/resources/db/migration/`

Migraciones versionadas de Flyway:

| Archivo | Descripción |
|---|---|
| `V1__create_users_table.sql` | Crea la tabla `users` |
| `V2__create_videos_table.sql` | Crea la tabla `videos` con todos los índices |
| `V3__add_check_user_auth_method.sql` | Añade constraint: al menos un método de auth |

---

## 4. Flujo Completo del Backend

### 4.1 Flujo de una Petición HTTP

```text
[Cliente HTTP]
    │
    │  Authorization: Bearer <JWT>
    │  POST /api/videos/generate
    │
    ▼
[Tomcat — Servidor Embebido]
    │
    ▼
[Spring Security Filter Chain]
    │
    ├── CorsFilter ──────────────────────────► Verifica origen CORS
    │                                          Maneja preflight OPTIONS
    │
    ├── JwtAuthenticationFilter ──────────────► 1. Extrae "Bearer <token>" del header
    │                                           2. JwtService.isValid(token)
    │                                           3. Si válido: crea UserPrincipal
    │                                              y lo setea en SecurityContextHolder
    │
    ├── AuthorizationFilter ──────────────────► Verifica reglas de SecurityConfig
    │                                           (¿la ruta requiere auth?)
    │
    ▼
[DispatcherServlet — Spring MVC]
    │
    ├── HandlerMapping ───────────────────────► Encuentra VideoController.generate()
    │
    ├── ArgumentResolver ─────────────────────► @CurrentUser Long userId
    │                                            CurrentUserResolver.resolveArgument()
    │                                            → extrae userId del UserPrincipal
    │
    ├── @Valid ───────────────────────────────► Bean Validation: valida GenerateVideoRequest
    │                                           prompt @NotBlank @Size(max=2000)
    │                                           durationSeconds @AllowedDuration
    │
    ▼
[VideoController.generate()]
    │
    ▼
[VideoService.generate()]
    │
    ├── countByUserIdAndStatus() ─────────────► Verifica cuota
    ├── userRepository.findById() ────────────► Carga entidad User
    ├── videoRepository.save() ───────────────► Persiste video con status=PROCESSING
    ├── vertexAiClient.predictLongRunning() ──► Llama a Vertex AI
    └── videoRepository.save() ───────────────► Guarda operationName
    │
    ▼
[VideoController] ────────────────────────────► ResponseEntity 202 Accepted
    │
    ▼
[GlobalExceptionHandler] (solo si hay excepción)
    │
    ▼
[Jackson] ───────────────────────────────────► Serializa VideoResponse a JSON
    │
    ▼
[Respuesta HTTP]
```

---

## 5. Flujo de Autenticación

### 5.1 Registro con Email y Contraseña

```text
Frontend                    Backend (Spring Boot)              Base de Datos
   │                               │                               │
   │  POST /api/auth/register      │                               │
   │  { name, email, password }    │                               │
   │──────────────────────────────►│                               │
   │                               │                               │
   │                        JwtAuthFilter                          │
   │                        (sin token → continúa)                 │
   │                               │                               │
   │                        SecurityConfig                         │
   │                        (/api/auth/** → público)               │
   │                               │                               │
   │                        @Valid valida campos                    │
   │                               │                               │
   │                        AuthService.register()                 │
   │                               │                               │
   │                               │  existsByEmail(email)         │
   │                               │──────────────────────────────►│
   │                               │◄──────────────────────────────│
   │                               │  [false → email disponible]   │
   │                               │                               │
   │                        BCrypt.hash(password)                  │
   │                               │                               │
   │                               │  INSERT users(...)            │
   │                               │──────────────────────────────►│
   │                               │◄──────────────────────────────│
   │                               │  [User con id generado]       │
   │                               │                               │
   │                        JwtService.generate(userId, email)     │
   │                               │                               │
   │  201 { token, user }          │                               │
   │◄──────────────────────────────│                               │
```

### 5.2 Login con Google OAuth (GIS)

```text
Usuario    Frontend           Google Identity    Backend              PostgreSQL
   │           │                   │                │                     │
   │  Clic     │                   │                │                     │
   │──────────►│                   │                │                     │
   │           │  Abre popup GIS   │                │                     │
   │           │──────────────────►│                │                     │
   │           │                   │                │                     │
   │  Selecciona cuenta y autoriza  │                │                     │
   │──────────────────────────────►│                │                     │
   │           │                   │                │                     │
   │           │  ID Token (JWT firmado por Google) │                     │
   │           │◄──────────────────│                │                     │
   │           │                   │                │                     │
   │           │  POST /api/auth/google             │                     │
   │           │  { idToken: "eyJhbGci..." }        │                     │
   │           │───────────────────────────────────►│                     │
   │           │                   │                │                     │
   │           │                   │   GoogleIdTokenVerifier.verify()    │
   │           │                   │   (verifica firma + aud + exp)      │
   │           │                   │                │                     │
   │           │                   │                │  findByGoogleId(sub)│
   │           │                   │                │────────────────────►│
   │           │                   │                │◄────────────────────│
   │           │                   │  [No encontrado]│                     │
   │           │                   │                │  findByEmail(email)  │
   │           │                   │                │────────────────────►│
   │           │                   │                │◄────────────────────│
   │           │                   │  [No encontrado]│                     │
   │           │                   │                │  INSERT users        │
   │           │                   │                │  (sin password_hash, │
   │           │                   │                │   con google_id)     │
   │           │                   │                │────────────────────►│
   │           │                   │                │◄────────────────────│
   │           │                   │                │                     │
   │           │                   │   JwtService.generate(userId, email) │
   │           │                   │                │                     │
   │           │  200 { token, user }               │                     │
   │           │◄───────────────────────────────────│                     │
```

### 5.3 Validación JWT en Peticiones Protegidas

```text
Request: GET /api/users/me
Authorization: Bearer eyJhbGc...

1. JwtAuthenticationFilter.doFilterInternal()
   │
   ├── extractToken() → extrae "eyJhbGc..."
   │
   ├── JwtService.isValid(token)
   │   └── Jwts.parser().verifyWith(key).build()
   │       .parseSignedClaims(token)
   │       → verifica firma HMAC-SHA256
   │       → verifica expiración
   │
   ├── JwtService.extractUserId(token) → 42L
   ├── JwtService.extractEmail(token) → "user@example.com"
   │
   └── SecurityContextHolder.setContext(
           new UserPrincipal(42L, "user@example.com")
       )

2. AuthorizationFilter verifica que la ruta requiere auth → ✓

3. DispatcherServlet → UserController.getMe()
   @CurrentUser Long userId
   ↓
   CurrentUserResolver.resolveArgument()
   ↓
   SecurityContextHolder.getContext().getAuthentication()
   → UserPrincipal.getUserId() → 42L
```

---

## 6. Flujo de Creación y Publicación de Videos

### 6.1 Flujo Completo de Generación de Video

```text
FASE 1 — SOLICITUD (síncrona, dura < 1 segundo)
═══════════════════════════════════════════════

Frontend ──POST /api/videos/generate──► VideoController
         { prompt, durationSeconds: 8 }       │
                                               ▼
                                         VideoService.generate()
                                               │
                                    ┌── ¿videos completados >= maxCompleted?
                                    │   → SÍ: 403 QUOTA_EXCEEDED
                                    │   → NO: continúa
                                    │
                                    ├── INSERT videos (status=PROCESSING)
                                    │   → videoId = UUID generado
                                    │
                                    ├── VertexAiClient.predictLongRunning()
                                    │   POST https://us-central1-aiplatform.googleapis.com/v1/
                                    │        projects/{p}/locations/{l}/publishers/google/
                                    │        models/veo-3.1-lite-generate-preview:predictLongRunning
                                    │   Body:
                                    │   {
                                    │     instances: [{ prompt: "..." }],
                                    │     parameters: {
                                    │       durationSeconds: 8,
                                    │       resolution: "720p",
                                    │       aspectRatio: "16:9",
                                    │       sampleCount: 1,
                                    │       storageUri: "gs://bucket/videos/{userId}/{videoId}/",
                                    │       personGeneration: "allow_adult"
                                    │     }
                                    │   }
                                    │   ← Vertex responde en ~1s con operationName
                                    │
                                    ├── UPDATE videos SET vertex_operation_name = operationName
                                    │
                                    └── RETURN 202 { id, status: "PROCESSING", ... }

Frontend ◄── 202 VideoResponse ─────────────────────────────────────────────────

FASE 2 — POLLING DEL FRONTEND (cada 5 segundos)
═════════════════════════════════════════════════

Frontend ──GET /api/videos/{id}/status──► VideoController
                                               │
                                               ▼
                                         VideoService.getStatus()
                                               │
                                    ├── findByIdAndUserId(videoId, userId)
                                    ├── refreshSignedUrlIfNeeded() [si COMPLETED]
                                    └── RETURN { status, signedUrl, errorMessage }
                                               │
Frontend ◄── { status: "PROCESSING" } ─────────┘
         (repite cada 5 segundos hasta COMPLETED o FAILED)

FASE 3 — JOB EN BACKGROUND (cada 15 segundos en el servidor)
═════════════════════════════════════════════════════════════

VideoStatusUpdateJob.pollProcessingVideos() [SCHEDULED]
    │
    ├── findByStatus(PROCESSING) → lista de videos pendientes
    │
    ├── Para cada video:
    │   │
    │   ├── ¿Creado hace más de 20 min? → markAsFailed("timeout")
    │   │
    │   └── VertexAiClient.getOperation(operationName)
    │       POST https://us-central1-aiplatform.googleapis.com/v1/
    │            {modelPath}:fetchPredictOperation
    │       Body: { operationName: "..." }
    │       │
    │       ├── done=false → esperar siguiente tick
    │       ├── done=true, error → VideoStatusPersister.markAsFailed()
    │       └── done=true, success → VideoStatusPersister.markAsCompleted()
    │                                    │
    │                        1. Extrae gcsUri del response JSON
    │                           (busca "uri" o "gcsUri")
    │                        2. Si no hay URI → GcsService.findVideoFile(prefix)
    │                        3. Normaliza: añade /sample_0.mp4 si necesario
    │                        4. GcsService.generateSignedUrl(gcsUri)
    │                           → URL firmada V4, TTL 7 días
    │                        5. UPDATE videos SET
    │                           status=COMPLETED,
    │                           gcs_uri=...,
    │                           signed_url=...,
    │                           signed_url_expires_at=...

FASE 4 — RECEPCIÓN EN EL FRONTEND
══════════════════════════════════

Frontend: GET /api/videos/{id}/status
   ← { status: "COMPLETED", signedUrl: "https://storage.googleapis.com/...?X-Goog-Signature=..." }

Frontend: <video src={signedUrl} controls />
   ──── HTTP GET directamente a GCS ────► Google Cloud Storage
                                         (autenticado por la Signed URL)
```

### 6.2 Gestión de URLs Firmadas (Signed URLs V4)

```text
GCS Bucket (privado)
        │
        │   Video: gs://bucket/videos/1/uuid/sample_0.mp4
        │
        ▼
GcsService.generateSignedUrl(gcsUri)
        │
        │   storage.signUrl(
        │       BlobInfo.newBuilder(bucket, objectPath).build(),
        │       7, TimeUnit.DAYS,
        │       Storage.SignUrlOption.withV4Signature(),
        │       Storage.SignUrlOption.httpMethod(HttpMethod.GET)
        │   )
        │
        ▼
URL resultante:
https://storage.googleapis.com/bucket/videos/1/uuid/sample_0.mp4
?X-Goog-Algorithm=GOOG4-RSA-SHA256
&X-Goog-Credential=sa@project.iam.gserviceaccount.com%2F...
&X-Goog-Date=20260515T120000Z
&X-Goog-Expires=604800    ← 7 días en segundos
&X-Goog-SignedHeaders=host
&X-Goog-Signature=abc123...

El browser puede hacer GET directamente a GCS sin credenciales.
La URL misma es el token de acceso temporal.
```

---

## 7. Comunicación con el Frontend

### 7.1 Contrato API — Todos los Endpoints

| Método | Endpoint | Auth | Body Request | Response |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | No | `RegisterRequest` | `201 AuthResponse` |
| `POST` | `/api/auth/login` | No | `LoginRequest` | `200 AuthResponse` |
| `POST` | `/api/auth/google` | No | `GoogleLoginRequest` | `200 AuthResponse` |
| `GET` | `/api/users/me` | JWT | — | `200 UserResponse` |
| `PUT` | `/api/users/me` | JWT | `UpdateProfileRequest` | `200 UserResponse` |
| `POST` | `/api/users/me/avatar` | JWT | `multipart/form-data` | `200 UserResponse` |
| `POST` | `/api/videos/generate` | JWT | `GenerateVideoRequest` | `202 VideoResponse` |
| `GET` | `/api/videos` | JWT | `?page=0&size=10` | `200 Page<VideoResponse>` |
| `GET` | `/api/videos/{id}` | JWT | — | `200 VideoResponse` |
| `GET` | `/api/videos/{id}/status` | JWT | — | `200 VideoStatusResponse` |
| `DELETE` | `/api/videos/{id}` | JWT | — | `204 No Content` |
| `GET` | `/health` | No | — | `200 {"status":"UP"}` |

### 7.2 Estructura de Respuestas de Datos

**`AuthResponse`**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": { ...UserResponse... }
}
```

**`UserResponse`**:
```json
{
  "id": 42,
  "email": "user@example.com",
  "name": "Juan Pérez",
  "avatarUrl": "https://storage.googleapis.com/...",
  "hasPassword": true,
  "hasGoogle": false,
  "videosGenerated": 3,
  "videosLimit": 9999,
  "createdAt": "2026-01-15T10:30:00Z"
}
```

**`VideoResponse`**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "prompt": "A cat playing piano in a jazz bar",
  "durationSeconds": 8,
  "status": "COMPLETED",
  "signedUrl": "https://storage.googleapis.com/...",
  "signedUrlExpiresAt": "2026-05-22T10:30:00Z",
  "errorMessage": null,
  "createdAt": "2026-05-15T10:30:00Z",
  "updatedAt": "2026-05-15T10:33:15Z"
}
```

**`VideoStatusResponse`** (respuesta ligera para polling):
```json
{
  "status": "COMPLETED",
  "signedUrl": "https://storage.googleapis.com/...",
  "errorMessage": null
}
```

**`ApiError`** (formato universal de error):
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Datos inválidos",
  "fields": {
    "email": "El email debe ser válido",
    "password": "La contraseña debe tener al menos 8 caracteres"
  }
}
```

### 7.3 Mecanismo de Autenticación Frontend-Backend

```text
1. Login/Register:
   POST /api/auth/login { email, password }
   ← 200 { token: "eyJ...", user: {...} }
   
   Frontend: localStorage.setItem("jwt", token)

2. Peticiones posteriores:
   GET /api/users/me
   Authorization: Bearer eyJ...
   
   Backend: JwtAuthFilter extrae y valida el token

3. Expiración (401):
   ← 401 "Unauthorized" (texto plano)
   
   Frontend: localStorage.removeItem("jwt")
              redirect → /login
```

### 7.4 CORS — Comunicación Cross-Origin

El frontend (Vercel: `https://app.vercel.app`) y el backend (Render: `https://backend.onrender.com`) están en dominios distintos. Spring Security maneja CORS:

- `CORS_ALLOWED_ORIGINS` (env var): lista de orígenes permitidos separada por comas.
- Siempre incluye `http://localhost:5173` para desarrollo.
- El header `Authorization` está en `allowedHeaders` → el browser puede enviarlo en peticiones cross-origin.
- `allowCredentials: true` → necesario para que el browser envíe headers de Authorization.

---

## 8. Patrones de Diseño Utilizados

### 8.1 Tabla de Patrones Detectados

| Patrón | Archivos donde Aparece | Explicación |
|---|---|---|
| **Dependency Injection (IoC)** | Todos los `@Service`, `@Component`, `@Controller` | Spring inyecta dependencias automáticamente. Los constructores anotados con `@RequiredArgsConstructor` reciben las dependencias como parámetros (constructor injection). |
| **Repository Pattern** | `UserRepository`, `VideoRepository` | Abstracción de la capa de persistencia. Los services no conocen JPA ni SQL directamente, solo interactúan con el Repository. |
| **Service Layer Pattern** | `AuthService`, `UserService`, `VideoService`, `JwtService` | La lógica de negocio está encapsulada en Services. Los Controllers solo orquestan y los Repositories solo persisten. |
| **DTO (Data Transfer Object)** | Todos los `*Request.java` y `*Response.java` | Desacopla la representación de la API de las entidades internas. Las entidades nunca se exponen directamente. |
| **Builder** | `User.java`, `Video.java` (Lombok `@Builder`) | Permite construir objetos complejos paso a paso. `User.builder().email(...).name(...).build()`. |
| **Factory Method** | `JwtService.generate()`, `GoogleCloudConfig` (beans) | Crea objetos concretos (`SecretKey`, `GoogleCredentials`, `Storage`) sin exponer la lógica de construcción. |
| **Template Method** | `JwtAuthenticationFilter` (extiende `OncePerRequestFilter`) | Define el esqueleto del algoritmo (filtrado HTTP). La superclase gestiona el `doFilter()`, la subclase implementa `doFilterInternal()`. |
| **Observer / Event Listener** | `GcsBucketInitializer.configureBucketCors()`, `VideoRepairJob.repairStuckVideos()`, `SocialVideoApplication.onReady()` | Se suscribe a `ApplicationReadyEvent`. El framework notifica cuando la aplicación está lista. |
| **Strategy** | `VertexAiClient.getOperation()` decide entre `fetchPredictOperation()` o `getStandardOperation()` | Selección dinámica del algoritmo de polling según el tipo de operación. |
| **Adapter** | `UserPrincipal` adapta la identidad del usuario al sistema de Spring Security (`AbstractAuthenticationToken`) | Permite que un objeto propio (`UserPrincipal`) sea usado donde Spring Security espera un `Authentication`. |
| **Custom Annotation** | `@CurrentUser`, `@AllowedDuration` | Anotaciones propias que extienden el framework sin modificar su código. |
| **Argument Resolver** | `CurrentUserResolver` implementa `HandlerMethodArgumentResolver` | Resuelve automáticamente el parámetro `@CurrentUser Long userId` en los métodos de los controllers. |
| **Scheduled Job** | `VideoStatusUpdateJob` con `@Scheduled` | Patrón de tarea periódica (Cron/Scheduler) para el polling a Vertex AI. |
| **Facade** | `GcsService` agrupa todas las operaciones de GCS | Una sola clase simplifica la interfaz compleja del SDK de Google Cloud Storage. |
| **Record (DTO inmutable)** | Todos los DTOs de Request/Response | Los Java Records son inmutables por diseño, ideales para DTOs. |
| **Singleton** | Todos los `@Bean` de Spring | Los beans de Spring son Singleton por defecto: una sola instancia en el contexto. |

### 8.2 Detalle de Patrones Clave

#### Dependency Injection con Constructor Injection

```java
// VideoService.java — todas las dependencias declaradas en el constructor
@Service
@RequiredArgsConstructor  // Lombok genera el constructor con todos los @final
public class VideoService {
    private final VideoRepository videoRepository;    // inyectado
    private final UserRepository userRepository;      // inyectado
    private final VertexAiClient vertexAiClient;      // inyectado
    private final GcsService gcsService;              // inyectado
    private final AppProperties appProperties;        // inyectado
}
```

Ventaja: las dependencias son explícitas, inmutables (final), y el objeto no puede existir sin ellas.

#### Repository Pattern

```java
// Los services no conocen JPA
public Page<VideoResponse> listByUser(Long userId, int page, int size) {
    Page<Video> videos = videoRepository
        .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    return videos.map(this::toVideoResponse);
}
// videoRepository oculta completamente la implementación SQL
```

#### Strategy Pattern en VertexAiClient

```java
public OperationResponse getOperation(String operationName) {
    // Selección dinámica de estrategia según el tipo de operación
    if (operationName.contains("/publishers/google/models/")) {
        return fetchPredictOperation(operationName);  // estrategia POST
    }
    return getStandardOperation(operationName);       // estrategia GET
}
```

---

## 9. Programación Orientada a Objetos

### 9.1 Encapsulamiento

Todos los campos de las entidades y servicios son privados (`private`). El acceso se hace a través de métodos:
- **Entidades JPA**: `@Getter` y `@Setter` de Lombok generan los accesores sin escribirlos manualmente.
- **DTOs**: Los Java Records son inherentemente encapsulados: los campos son privados finales y solo se accede con los métodos de acceso autogenerados (`record.email()`, `record.token()`).
- **Servicios**: Los métodos `private` como `refreshSignedUrlIfNeeded()`, `resolveAvatarUrl()`, `findUserOrThrow()` encapsulan lógica auxiliar.

### 9.2 Abstracción

Las abstracciones clave del sistema:

| Abstracción | Implementación | Propósito |
|---|---|---|
| `JpaRepository<T, ID>` | `UserRepository`, `VideoRepository` | Abstrae la persistencia. El code de negocio no sabe si es Postgres o H2. |
| `AbstractAuthenticationToken` | `UserPrincipal` | Abstrae la identidad del usuario. Spring Security no conoce los detalles de autenticación. |
| `OncePerRequestFilter` | `JwtAuthenticationFilter` | Abstrae el ciclo de vida de los filtros HTTP. |
| `HandlerMethodArgumentResolver` | `CurrentUserResolver` | Abstrae la resolución de parámetros en controllers. |
| `ConstraintValidator` | `AllowedDurationValidator` | Abstrae la lógica de validación de restricciones. |

### 9.3 Herencia

| Clase Hija | Clase/Interfaz Padre | Por qué |
|---|---|---|
| `UserPrincipal` | `AbstractAuthenticationToken` | Integración con Spring Security |
| `JwtAuthenticationFilter` | `OncePerRequestFilter` | Garantizar ejecución única por request |
| `CurrentUserResolver` | `HandlerMethodArgumentResolver`, `WebMvcConfigurer` | Resolución de argumentos + auto-registro |
| `AllowedDurationValidator` | `ConstraintValidator<AllowedDuration, Short>` | Integración con Bean Validation |
| Todas las excepciones | `RuntimeException` | Unchecked exceptions para código limpio |

### 9.4 Polimorfismo

- **En repositorios**: El código usa `UserRepository` (interfaz), Spring genera dinámicamente una implementación concreta con `JdkDynamicAopProxy`.
- **En filtros**: Spring llama a `doFilterInternal()` del `JwtAuthenticationFilter` sin saber su implementación concreta.
- **En `@ExceptionHandler`**: `GlobalExceptionHandler` tiene múltiples métodos con el mismo patrón pero para diferentes tipos de excepción. Spring selecciona el más específico automáticamente.

### 9.5 Interfaces Funcionales y Records

El proyecto usa características modernas de Java 21:
- **Java Records**: `AuthResponse`, `LoginRequest`, `RegisterRequest`, `VideoResponse`, `ApiError`, `OperationResponse`, `GooglePayload`. Inmutables, concisos.
- **Pattern Matching** (`instanceof`): `CurrentUserResolver` usa `if (auth instanceof UserPrincipal principal)`.
- **Optional con encadenamiento**: `AuthService.googleLogin()` usa `.or(() -> ...)` y `.orElseGet(() -> ...)` para la lógica de vinculación de cuentas.

---

## 10. Seguridad del Sistema

### 10.1 Mecanismo de Autenticación

El sistema implementa autenticación **stateless** con JWT. No hay sesiones HTTP en el servidor.

```text
Solicitud con token:
  Header: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MiIsImVtYWlsIjoidUBleGFtcGxlLmNvbSIsImlhdCI6MTcxNTc2NjQwMCwiZXhwIjoxNzE1ODUyODAwfQ.signature

Decodificado:
  Header: { "alg": "HS256" }
  Payload: {
    "sub": "42",                           ← userId
    "email": "user@example.com",           ← email
    "iat": 1715766400,                     ← issued at
    "exp": 1715852800                      ← expira en 24h
  }
  Firma: HMAC-SHA256(base64(header) + "." + base64(payload), secretKey)
```

### 10.2 Algoritmo de Cifrado de Contraseñas

**BCrypt** con factor de coste 10 (configurable). BCrypt es un algoritmo diseñado específicamente para contraseñas:
- Incluye salt aleatorio en el hash.
- Es lento por diseño (resistente a ataques de fuerza bruta).
- El hash almacenado incluye el factor de coste, permitiendo migrar sin perder contraseñas existentes.

### 10.3 Protección de Rutas

```
Reglas de SecurityConfig (evaluadas en orden):
  1. OPTIONS /**     → PERMITIDO (preflight CORS)
  2. POST /api/auth/** → PERMITIDO (login/registro)
  3. /health, /error  → PERMITIDO (monitoring)
  4. Cualquier otra  → REQUIERE AUTENTICACIÓN JWT
```

### 10.4 Validación de Datos de Entrada

Todas las entradas del cliente son validadas antes de procesarse:

| Validación | Mecanismo | Ejemplo |
|---|---|---|
| Formato y presencia | Bean Validation (`@Valid`) | `@NotBlank`, `@Email`, `@Size` |
| Duración de video | Custom Constraint (`@AllowedDuration`) | Solo 4, 6 u 8 segundos |
| Tipo de archivo | Verificación en UserService | Solo `image/png`, `image/jpeg` |
| Tamaño de archivo | Spring Multipart config | Max 2MB |
| Propiedad de recursos | Consulta BD con userId | `findByIdAndUserId(videoId, userId)` |

### 10.5 Protección contra Ataques Comunes

| Ataque | Protección |
|---|---|
| **SQL Injection** | JPA/Hibernate usa queries parametrizadas. Nunca se concatenan strings SQL. |
| **XSS** | El backend es una API JSON pura. No renderiza HTML. |
| **CSRF** | Deshabilitado correctamente (API stateless con JWT en header, no en cookies). |
| **Enumeración de IDs** | Los videos usan UUID como PK. Los usuarios usan Long pero no se exponen en URLs de videos. |
| **Acceso a recursos de otros usuarios** | `findByIdAndUserId()` verifica ownership en cada operación sobre videos. |
| **Tokens Google falsos** | `GoogleIdTokenVerifier` valida la firma criptográfica contra las claves públicas de Google. |
| **Tokens JWT manipulados** | JJWT valida la firma HMAC-SHA256 y la expiración. |
| **Flooding de Vertex AI** | El scheduler corre cada 15s independientemente del número de usuarios. Los endpoints de status leen de BD, no llaman a Vertex. |

### 10.6 Seguridad de Archivos GCS

- El bucket es **privado**: Public Access Prevention enforced.
- El acceso a videos se hace únicamente a través de **Signed URLs V4** temporales.
- Las URLs firmadas expiran en 7 días y son renovadas automáticamente por el backend.
- Los avatares tienen Signed URLs con TTL de 30 días.
- La Service Account tiene permisos mínimos: solo Vertex AI User + Storage Object Admin.

---

## 11. Manejo de Datos y Persistencia

### 11.1 Base de Datos

**PostgreSQL 16** gestionado en Render. Configuración via `spring.datasource` con pool Hikari:
- `maximum-pool-size: 10`
- `minimum-idle: 2`

### 11.2 Diagrama Entidad-Relación

```
┌────────────────────────────────────────────────────────────────┐
│                         TABLA: users                           │
├─────────────────────────┬──────────────┬───────────────────────┤
│ Columna                 │ Tipo         │ Constraints           │
├─────────────────────────┼──────────────┼───────────────────────┤
│ id (PK)                 │ BIGSERIAL    │ PRIMARY KEY           │
│ email                   │ VARCHAR(255) │ NOT NULL, UNIQUE       │
│ name                    │ VARCHAR(150) │ NOT NULL               │
│ password_hash           │ VARCHAR(255) │ NULL (Google-only)    │
│ google_id               │ VARCHAR(255) │ UNIQUE, NULL          │
│ avatar_url              │ TEXT         │ NULL                  │
│ created_at              │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW() │
│ updated_at              │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW() │
└─────────────────────────┴──────────────┴───────────────────────┘
        │  CHECK: password_hash IS NOT NULL OR google_id IS NOT NULL
        │
        │  1:N (ON DELETE CASCADE)
        │
        ▼
┌────────────────────────────────────────────────────────────────┐
│                         TABLA: videos                          │
├─────────────────────────┬──────────────┬───────────────────────┤
│ Columna                 │ Tipo         │ Constraints           │
├─────────────────────────┼──────────────┼───────────────────────┤
│ id (PK)                 │ UUID         │ DEFAULT gen_random_uuid() │
│ user_id (FK)            │ BIGINT       │ NOT NULL → users(id)  │
│ prompt                  │ TEXT         │ NOT NULL               │
│ duration_seconds        │ SMALLINT     │ NOT NULL, CHECK IN (4,6,8) │
│ status                  │ VARCHAR(20)  │ NOT NULL, CHECK IN (...) │
│ vertex_operation_name   │ TEXT         │ NULL                  │
│ gcs_uri                 │ TEXT         │ NULL                  │
│ signed_url              │ TEXT         │ NULL                  │
│ signed_url_expires_at   │ TIMESTAMPTZ  │ NULL                  │
│ error_message           │ TEXT         │ NULL                  │
│ created_at              │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW() │
│ updated_at              │ TIMESTAMPTZ  │ NOT NULL DEFAULT NOW() │
└─────────────────────────┴──────────────┴───────────────────────┘

ÍNDICES en videos:
  idx_videos_user_id      → (user_id)           — lista de videos por usuario
  idx_videos_user_status  → (user_id, status)   — COUNT para cuota
  idx_videos_status       → (status)            — scheduler busca PROCESSING
  idx_videos_created_at   → (created_at DESC)   — historial paginado reciente-primero
```

### 11.3 Migraciones con Flyway

**Flyway** gestiona el versionado del schema SQL:
- Archivos en `src/main/resources/db/migration/` con naming `V{version}__{description}.sql`.
- `baseline-on-migrate: true`: permite arrancar Flyway en una BD que ya tiene tablas.
- En `application.yml`: `ddl-auto: validate` → Hibernate verifica, Flyway gestiona.

Historial de migraciones aplicadas:
1. `V1`: Tabla `users` con autenticación dual.
2. `V2`: Tabla `videos` con todos los campos del ciclo de vida + extensión `pgcrypto` para UUIDs.
3. `V3`: Constraint de integridad que garantiza que cada usuario tiene al menos un método de autenticación.

### 11.4 Transacciones

| Método | Transacción | Por qué |
|---|---|---|
| `AuthService.register()` | `@Transactional` | INSERT + SELECT en una unidad atómica |
| `AuthService.login()` | `@Transactional(readOnly = true)` | Solo lectura, optimización de Hibernate |
| `AuthService.googleLogin()` | `@Transactional` | INSERT o UPDATE según vinculación |
| `VideoService.generate()` | `@Transactional` | INSERT video + UPDATE con operationName |
| `VideoService.listByUser()` | `@Transactional` | Puede actualizar signed URLs durante la lectura |
| `VideoStatusPersister.markAsCompleted()` | `@Transactional` | UPDATE atómico de status + gcsUri + signedUrl |
| `VideoStatusUpdateJob.pollProcessingVideos()` | Sin `@Transactional` | La llamada HTTP no debe estar dentro de una transacción |

### 11.5 Relación JPA

La entidad `Video` tiene una relación `@ManyToOne(fetch = FetchType.LAZY)` con `User`:
- `FetchType.LAZY`: El usuario no se carga de BD automáticamente al cargar un Video. Solo se carga cuando se accede al campo `video.getUser()`.
- `@JoinColumn(name = "user_id")`: Define la columna de clave foránea.
- `ON DELETE CASCADE` (en SQL, no en JPA): Si se elimina un usuario, sus videos se eliminan automáticamente en BD.

---

## 12. APIs Externas y Servicios Integrados

### 12.1 Google Vertex AI — Veo 3.1 Lite

| Aspecto | Detalle |
|---|---|
| **Tipo** | REST API de Google Cloud |
| **Autenticación** | OAuth2 Service Account (`Bearer` token de Google) |
| **Endpoint principal** | `POST {location}-aiplatform.googleapis.com/v1/...predictLongRunning` |
| **Modelo** | `veo-3.1-lite-generate-preview` |
| **Naturaleza** | Operación larga asíncrona (LRO - Long Running Operation) |
| **Polling** | `POST {modelPath}:fetchPredictOperation` |
| **Parámetros de generación** | `durationSeconds`, `resolution: "720p"`, `aspectRatio: "16:9"`, `sampleCount: 1`, `storageUri`, `personGeneration` |
| **Output** | Archivo `.mp4` subido directamente a GCS |
| **Timeout configurado** | 20 minutos |
| **Polling del scheduler** | Cada 15 segundos |

**Flujo de credenciales con Vertex AI**:
```text
Service Account JSON (env var GCP_SA_KEY_JSON)
        ↓
GoogleCredentials.fromStream(json).createScoped("cloud-platform")
        ↓
googleCredentials.refreshIfExpired()  ← refresca token OAuth2 automáticamente
        ↓
accessToken = googleCredentials.getAccessToken().getTokenValue()
        ↓
Header: Authorization: Bearer {accessToken}  ← se envía a Vertex AI
```

### 12.2 Google Cloud Storage

| Aspecto | Detalle |
|---|---|
| **SDK** | `google-cloud-storage` 2.24.0 |
| **Autenticación** | Service Account (mismo bean de `GoogleCredentials`) |
| **Operaciones** | Upload (avatares), list (búsqueda de videos), signUrl (Signed URLs) |
| **Firma de URLs** | V4 con HMAC-SHA256 |
| **Estructura del bucket** | `videos/{userId}/{videoId}/sample_0.mp4`, `avatars/{userId}/avatar.{ext}` |
| **CORS del bucket** | Configurado en startup para `*` en GET/HEAD |

### 12.3 Google Identity Services (OAuth)

| Aspecto | Detalle |
|---|---|
| **SDK** | `google-api-client` 2.7.0 |
| **Clase principal** | `GoogleIdTokenVerifier` |
| **Validaciones** | Firma digital, audiencia (`aud`), expiración (`exp`) |
| **Claves públicas** | Se cachean localmente, se refrescan automáticamente |
| **Datos extraídos** | `sub` (identificador único), `email`, `name`, `picture` |

### 12.4 spring-dotenv

La librería `me.paulschwarz:spring-dotenv:4.0.0` permite cargar automáticamente un archivo `.env` en desarrollo local. Las variables se inyectan como properties de Spring Boot, sin necesidad de export manual.

---

## 13. Flujo Interno entre Módulos

### 13.1 Mapa de Dependencias entre Módulos

```text
                    ┌──────────────┐
                    │   security   │
                    │  (filtros,   │
                    │   JwtService)│
                    └──────┬───────┘
                           │ usa JwtService
                           ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│     auth     │───►│     user     │◄───│    video     │
│  controller  │    │  controller  │    │  controller  │
│  service     │    │  service     │    │  service     │
│  JwtService  │    │  repository  │    │  repository  │
│  GoogleId    │    │  entity      │    │  entity      │
│  TokenSvc    │    │              │    │              │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       │   usa             │   usa             │   usa
       ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────┐
│                    external                          │
│         ┌─────────────────┐  ┌──────────────┐       │
│         │  VertexAiClient │  │  GcsService  │       │
│         └─────────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────┘
       │                   │
       ▼                   ▼
┌──────────────┐    ┌──────────────┐
│  Google      │    │  Google      │
│  Vertex AI   │    │  Cloud       │
│  (Veo 3.1)   │    │  Storage     │
└──────────────┘    └──────────────┘

Todos los módulos usan:
  - config/AppProperties (configuración centralizada)
  - exception/GlobalExceptionHandler (errores)
```

### 13.2 Dependencias Detalladas por Módulo

**`auth` depende de**:
- `user/repository/UserRepository` — para buscar/crear usuarios
- `user/service/UserService` — para el mapeo User→UserResponse
- `auth/service/JwtService` — para generar tokens
- `auth/service/GoogleIdTokenService` — para verificar tokens de Google
- `security/PasswordEncoder` (bean) — para cifrado de contraseñas

**`user` depende de**:
- `user/repository/UserRepository` — CRUD de usuarios
- `video/repository/VideoRepository` — para contar videos en `toUserResponse()`
- `external/gcs/GcsService` — para upload de avatares y resolución de URLs

**`video` depende de**:
- `video/repository/VideoRepository` — CRUD de videos
- `user/repository/UserRepository` — para cargar el usuario propietario
- `external/vertex/VertexAiClient` — para llamar a Vertex AI
- `external/gcs/GcsService` — para Signed URLs y búsqueda de archivos
- `config/AppProperties` — para configuración de cuota, TTL, etc.

**`external` depende de**:
- `config/AppProperties` — para URLs, projectId, modelId, etc.
- Google SDKs (`GoogleCredentials`, `Storage`) — inyectados desde `config/GoogleCloudConfig`

---

## 14. Configuración y Despliegue

### 14.1 Variables de Entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DATABASE_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://host/db` |
| `SPRING_DATASOURCE_USERNAME` | Usuario BD | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña BD | `password` |
| `JWT_SECRET` | Secreto HMAC (mín. 32 chars) | `random-64-char-string` |
| `JWT_EXPIRATION_MS` | TTL del JWT en ms | `86400000` (24h) |
| `GOOGLE_CLIENT_ID` | OAuth Client ID de Google | `123.apps.googleusercontent.com` |
| `GCP_PROJECT_ID` | ID del proyecto GCP | `social-video-ai-prod` |
| `GCP_LOCATION` | Región de GCP | `us-central1` |
| `GCS_BUCKET_NAME` | Nombre del bucket | `social-video-ai-videos` |
| `GCP_SA_KEY_JSON` | JSON completo de la Service Account | `{"type":"service_account"...}` |
| `VEO_MODEL_ID` | ID del modelo de Vertex AI | `veo-3.1-lite-generate-preview` |
| `CORS_ALLOWED_ORIGINS` | Orígenes CORS permitidos | `https://app.vercel.app` |
| `SPRING_PROFILES_ACTIVE` | Perfil activo | `dev` o `prod` |
| `PORT` | Puerto HTTP | `8080` |

### 14.2 Dockerfile — Build Multi-Etapa

```text
Stage 1 — Build (maven:3.9-eclipse-temurin-21)
  │
  ├── COPY pom.xml → mvn dependency:go-offline
  │   (Las dependencias Maven se cachean en esta capa si pom.xml no cambia)
  │
  ├── COPY src/ → mvn clean package -DskipTests
  │
  └── target/app.jar

Stage 2 — Runtime (eclipse-temurin:21-jre-alpine)
  │
  ├── Imagen Alpine (~50MB vs ~400MB de full JDK)
  │
  ├── Crea usuario non-root: appuser:appgroup
  │   (principio de menor privilegio)
  │
  ├── COPY --from=build app.jar
  │
  └── ENTRYPOINT:
      java
        -XX:+UseContainerSupport      ← detecta límites de memoria del contenedor
        -XX:MaxRAMPercentage=75.0     ← usa el 75% de la RAM del contenedor para el heap
        -Djava.security.egd=file:/dev/./urandom  ← acelera inicialización JVM en Linux
        -jar app.jar
```

**Por qué multi-stage**: La imagen final no contiene Maven, código fuente, ni las herramientas de compilación. Solo el JRE mínimo y el JAR ejecutable. Esto reduce el tamaño de la imagen de ~800MB a ~200MB y la superficie de ataque.

### 14.3 Perfiles de Spring Boot

| Perfil | Activación | Configuración adicional |
|---|---|---|
| `dev` | Por defecto (`SPRING_PROFILES_ACTIVE=dev`) | Logs DEBUG, SQL en consola |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | Logs INFO, `ddl-auto: none` |
| `test` | Automático en tests con `@SpringBootTest` | H2 en memoria, sin GCS |

### 14.4 Construcción con Maven

```bash
# Desarrollo local
./mvnw spring-boot:run

# Compilar JAR
./mvnw clean package -DskipTests

# Ejecutar tests
./mvnw test

# Docker
docker build -t social-video-backend .
docker run -p 8080:8080 --env-file .env social-video-backend
```

### 14.5 Infraestructura de Despliegue

```text
┌─────────────────────────────────────────────────────┐
│                  GOOGLE CLOUD                        │
│  ┌────────────────────┐  ┌────────────────────────┐ │
│  │    Vertex AI       │  │   Cloud Storage         │ │
│  │   (Veo 3.1 Lite)   │  │   Bucket privado        │ │
│  └────────────────────┘  └────────────────────────┘ │
│  ┌────────────────────┐                              │
│  │  Google Identity   │  ← Verificación OAuth        │
│  │  Services (GIS)    │                              │
│  └────────────────────┘                              │
└─────────────────────────────────────────────────────┘
            │ HTTPS                    │ Signed URLs
            ▼                          ▼
┌────────────────────────┐   ┌─────────────────────────┐
│       RENDER           │   │        VERCEL           │
│  ┌──────────────────┐  │   │  ┌─────────────────────┐│
│  │  Spring Boot App │  │   │  │  React + Vite SPA   ││
│  │  Docker Container│  │   │  │  TypeScript         ││
│  └────────┬─────────┘  │   │  └─────────────────────┘│
│           │            │   └─────────────────────────┘
│  ┌────────▼─────────┐  │
│  │   PostgreSQL 16  │  │
│  └──────────────────┘  │
└────────────────────────┘
```

---

## 15. Buenas Prácticas Implementadas

### 15.1 Principios SOLID

| Principio | Aplicación en el Código |
|---|---|
| **S** — Single Responsibility | Cada clase tiene una responsabilidad. `JwtService` solo gestiona JWT. `GcsService` solo gestiona GCS. `VideoStatusPersister` solo persiste cambios de estado. |
| **O** — Open/Closed | El sistema de excepciones es extensible: agregar un nuevo tipo de error requiere solo una nueva clase de excepción + un método en `GlobalExceptionHandler`. |
| **L** — Liskov Substitution | `UserPrincipal` puede usarse donde Spring Security espera `AbstractAuthenticationToken`. `JwtAuthenticationFilter` puede usarse donde se espera `OncePerRequestFilter`. |
| **I** — Interface Segregation | `CurrentUserResolver` implementa `HandlerMethodArgumentResolver` (solo los métodos necesarios) y `WebMvcConfigurer` (solo `addArgumentResolvers`). No implementa interfaces innecesariamente grandes. |
| **D** — Dependency Inversion | Los módulos de alto nivel (`VideoService`) dependen de abstracciones (`VideoRepository`, `GcsService`) no de implementaciones concretas. Spring inyecta las implementaciones. |

### 15.2 Clean Code

- **Nombres expresivos**: `refreshSignedUrlIfNeeded()`, `findUserOrThrow()`, `buildAuthResponse()`, `resolveAvatarUrl()`. Se lee como prosa.
- **Métodos pequeños y enfocados**: Ningún método supera las 50 líneas. Los métodos privados extraen lógica auxiliar.
- **Sin magic numbers**: Las constantes están en configuración (`AppProperties`) o como constantes de clase (`Set<String> ALLOWED_IMAGE_TYPES`).
- **Sin comentarios obvios**: Los comentarios existentes explican el **por qué**, no el qué: `// Re-read entity inside this transaction to avoid stale/detached state`, `// NO marcar COMPLETED si no tenemos signedUrl válido`.

### 15.3 Separación de Responsabilidades

El patrón de separación más notable es la división entre `VideoStatusUpdateJob` y `VideoStatusPersister`:
- `VideoStatusUpdateJob`: **Coordinación** — itera videos, llama a Vertex AI, decide qué hacer.
- `VideoStatusPersister`: **Persistencia** — escribe en BD con transacción correcta.

Esta separación es necesaria porque JPA no permite tener una transacción abierta durante una llamada HTTP externa (Vertex AI puede tardar varios segundos).

### 15.4 Modularidad y Desacoplamiento

- El módulo `external/` puede reemplazarse completamente (cambiar Vertex AI por otro proveedor) sin tocar `video/service/VideoService`.
- El módulo `auth/` puede extenderse para soportar más proveedores OAuth sin modificar los demás módulos.
- Los DTOs desacoplan la representación de la API de las entidades JPA. Si cambia el schema de BD, los DTOs pueden absorber el cambio.

### 15.5 Manejo Centralizado de Errores

El `GlobalExceptionHandler` centraliza todos los errores en un solo lugar. Los services lanzan excepciones semánticas (`ResourceNotFoundException`, `QuotaExceededException`) sin preocuparse por los códigos HTTP. El handler hace la traducción.

### 15.6 Idempotencia

- `GcsBucketInitializer.configureBucketCors()` puede correr en cada arranque sin efectos secundarios.
- `VideoRepairJob.repairStuckVideos()` es idempotente: si el video ya está `COMPLETED`, no hace nada.
- Las migraciones de Flyway son idempotentes por naturaleza.

### 15.7 Tests Unitarios

El proyecto incluye tests unitarios para los servicios más críticos:

| Test | Qué prueba | Técnica |
|---|---|---|
| `JwtServiceTest` | Generación, extracción y validación de JWT | Unit test puro, sin Spring |
| `AuthServiceTest` | Registro, login, casos de error | Mockito para dependencias |
| `VideoServiceTest` | Generación de video, cuota | Mockito para dependencias |
| `SocialVideoApplicationTest` | Arranque del contexto de Spring | `@SpringBootTest` con H2 |

Los tests usan **Mockito** para simular las dependencias, lo que permite probar la lógica de negocio en aislamiento sin necesidad de BD real ni servicios externos.

### 15.8 Áreas de Mejora Detectadas

| Área | Problema | Mejora Sugerida |
|---|---|---|
| **Tests de integración** | Solo hay tests unitarios básicos. No hay tests de controllers ni de integración con H2. | Agregar `@WebMvcTest` para controllers y `@DataJpaTest` para repositories. |
| **Logging estructurado** | Los logs usan strings interpolados. | Migrar a logging estructurado (JSON) para facilitar análisis en Render/Grafana. |
| **Métricas** | No hay Actuator ni métricas de rendimiento. | Agregar `spring-boot-starter-actuator` para health checks detallados, métricas de JVM y pool de conexiones. |
| **Caché** | Las Signed URLs se regeneran en cada acceso si están cerca de expirar. | Agregar caché en memoria (Spring Cache + Caffeine) para Signed URLs frecuentemente accedidas. |
| **Refresh tokens** | Un solo JWT de 24h. | En producción real, implementar refresh tokens con rotación para mayor seguridad. |
| **Rate limiting** | No hay límite de peticiones por IP. | Agregar rate limiting en el endpoint de generación de video. |
| **Documentación API** | No hay Swagger/OpenAPI. | Integrar `springdoc-openapi` para documentación automática de la API. |

---

## 16. Conclusión Técnica

### 16.1 Calidad Arquitectónica

El backend de Social Video AI demuestra una arquitectura sólida y coherente para un sistema de generación de contenido multimedia con IA. Las decisiones de diseño son técnicamente fundamentadas y justificadas:

- La elección de **arquitectura en capas modular por dominio** equilibra la simplicidad de Spring Boot clásico con la organización por contextos acotados de DDD, sin el overhead de la arquitectura hexagonal completa.
- La separación entre `VideoStatusUpdateJob` (coordinador) y `VideoStatusPersister` (persistidor) resuelve el problema clásico de las transacciones con operaciones externas de larga duración.
- El sistema de **Signed URLs con renovación automática** es una solución elegante para dar acceso temporal a archivos privados sin complejidad adicional.
- La **vinculación automática de cuentas** Google-email es una decisión de UX correctamente implementada a nivel de base de datos (con check constraints que garantizan integridad).

### 16.2 Escalabilidad

El sistema está diseñado para escalar horizontalmente:
- **Stateless**: No hay estado de sesión en el servidor. Múltiples instancias pueden correr en paralelo.
- **Pool de conexiones Hikari**: Gestiona eficientemente las conexiones a BD.
- **Scheduler independiente del tráfico**: El polling a Vertex AI es independiente del número de usuarios.
- **Base de datos con índices correctos**: Los índices en `videos` soportan los patrones de consulta más frecuentes.

### 16.3 Mantenibilidad

- **Estructura predecible**: Cualquier nuevo desarrollador encontrará inmediatamente el código relevante buscando por módulo funcional.
- **Bajo acoplamiento**: Los módulos se comunican a través de interfaces y DTOs, no directamente con las entidades del otro módulo.
- **Configuración centralizada**: Todas las configuraciones están en `AppProperties`, no dispersas en `@Value` por el código.
- **Migraciones versionadas**: Flyway garantiza que el schema de BD siempre está sincronizado con el código.

### 16.4 Fortalezas del Sistema

1. **Manejo robusto de videos perdidos**: El `VideoRepairJob` rescata videos que quedaron en estado inconsistente después de reinicios del servidor.
2. **Compatibilidad con dos endpoints de Vertex AI**: La detección automática entre `fetchPredictOperation` y el endpoint estándar resuelve una inconsistencia real de la API de Google.
3. **Seguridad sin sesiones**: El modelo JWT stateless es el adecuado para una SPA con backend en dominios distintos.
4. **DTOs como contratos**: Los Java Records como DTOs son inmutables, concisos y forman un contrato claro entre el frontend y el backend.
5. **Validaciones ricas**: Tanto las anotaciones estándar de Bean Validation como las personalizadas (`@AllowedDuration`) garantizan que los datos llegan correctos a la capa de negocio.

### 16.5 Resumen del Sistema

```text
Social Video AI Backend — Mapa mental

                    USUARIO
                      │
              ┌───────┴────────┐
              │                │
          EMAIL+PASS        GOOGLE OAuth
              │                │
              └───────┬────────┘
                      │  JWT (24h)
                      │
              ┌───────▼────────┐
              │   PERFIL       │
              │  (avatar GCS)  │
              └───────┬────────┘
                      │
              ┌───────▼────────────────────────────────────────┐
              │         GENERACIÓN DE VIDEO                     │
              │                                                  │
              │  1. Validar cuota                               │
              │  2. Insertar en BD (PROCESSING)                 │
              │  3. Llamar a Vertex AI Veo 3.1 Lite             │
              │  4. Guardar operationName                       │
              │  5. Retornar 202 Accepted                       │
              │                                                  │
              │  ┌────────────── Scheduler 15s ───────────────┐ │
              │  │ Polling Vertex AI → GCS URI → Signed URL   │ │
              │  │ Timeout 20min → FAILED                     │ │
              │  └────────────────────────────────────────────┘ │
              │                                                  │
              │  Frontend polling 5s → BD → signedUrl           │
              │  Browser → GCS Signed URL V4 → <video>         │
              └─────────────────────────────────────────────────┘
```

El proyecto representa un ejemplo bien construido de integración de servicios de IA en una aplicación web moderna, con una arquitectura clara, decisiones de diseño justificadas y un manejo cuidadoso de la asincronía inherente a la generación de contenido multimedia con modelos de inteligencia artificial.

---

*Documento generado mediante análisis completo del código fuente real del proyecto. Todos los nombres de clases, métodos, archivos y configuraciones corresponden al código existente.*
