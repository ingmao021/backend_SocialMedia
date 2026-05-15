# PRD — Social Video AI (Frontend)

## Resumen
Aplicación web para generar videos con IA a partir de un prompt, consultar el estado de procesamiento y reproducir el video cuando esté listo. Incluye autenticación por email/contraseña y Google, gestión de perfil y control de cuota.

## Objetivos
1. Permitir a usuarios autenticados generar videos con IA de forma simple y rápida.
2. Mostrar el estado del video en tiempo real y reproducirlo automáticamente al completarse.
3. Ofrecer una experiencia completa de cuenta: registro, login, perfil, avatar y cuota.

## Usuarios objetivo
- Creadores de contenido y marketers que necesitan clips cortos generados por IA.
- Usuarios que desean experimentar con generación de video desde texto.

## Alcance (MVP)
| Área | Incluye |
| --- | --- |
| Autenticación | Registro, login, Google OAuth, cierre de sesión |
| Dashboard | Generación, listado paginado, polling de estado, reproducción, borrado |
| Perfil | Editar nombre, email solo lectura, avatar (JPG/PNG/WebP) |
| Cuota | Visualización de límite y aviso cuando se excede |

## Requisitos funcionales
1. Registro con nombre, email y contraseña (mín. 8 caracteres), validaciones de formato.
2. Login con email/contraseña y con Google OAuth.
3. Persistencia de sesión con JWT en `localStorage` y rehidratación al iniciar.
4. Rutas protegidas para dashboard y perfil.
5. Generación de video con prompt (máx 2000 caracteres) y duración (4/6/8s).
6. Listado paginado de videos (page size 6) con botón de recarga manual.
7. Polling por video en `PROCESSING` cada 5s vía `GET /api/videos/{id}/status`.
8. Actualización de estado en la lista cuando llega `COMPLETED` + `signedUrl` o `FAILED`.
9. Reproducción del video cuando hay `signedUrl`.
10. Borrado con confirmación.
11. Indicador visual de auto-actualización mientras haya videos en procesamiento.
12. Perfil con edición de nombre y subida de avatar (JPG/PNG/WebP, máx 2 MB).

## Requisitos no funcionales
1. **Performance:** polling eficiente solo mientras existan videos en `PROCESSING`.
2. **Seguridad:** token JWT en header `Authorization: Bearer`.
3. **UX:** feedback claro con spinners, estados vacíos y toasts de error.
4. **Compatibilidad:** SPA React + Vite, rutas con React Router.
5. **Configuración:** `VITE_API_URL` define el backend.

## Endpoints consumidos
| Método | Endpoint | Uso |
| --- | --- | --- |
| POST | `/api/auth/register` | Registro |
| POST | `/api/auth/login` | Login |
| POST | `/api/auth/google` | Login Google |
| GET | `/api/users/me` | Perfil actual |
| PUT | `/api/users/me` | Actualizar perfil |
| POST | `/api/users/me/avatar` | Subir avatar |
| POST | `/api/videos/generate` | Generar video |
| GET | `/api/videos` | Listar videos |
| GET | `/api/videos/{id}` | Video completo |
| GET | `/api/videos/{id}/status` | Estado polling |
| DELETE | `/api/videos/{id}` | Eliminar |

## Métricas de éxito
1. Tiempo promedio de `PROCESSING` a reproducción visible.
2. Tasa de éxito de generación (COMPLETED vs FAILED).
3. Conversión de registro a primer video generado.
4. Retención: sesiones con al menos 1 video generado.

## Fuera de alcance
1. Suscripciones/pagos.
2. WebSockets/SSE (se usa polling).
3. Compartir videos públicamente o exportarlos.
4. Moderación avanzada de contenido.

## Riesgos y consideraciones
- Si el backend no retorna `signedUrl` al completar, el video no podrá reproducirse.
- Muchos videos simultáneos podrían incrementar carga por polling; considerar batch endpoint a futuro.
