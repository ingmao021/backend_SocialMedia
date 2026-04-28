# 🚀 **Variables de Entorno - Render Production**

## 📋 **Variables Requeridas**

Configura estas variables en tu servicio de Render:

### **Base de Datos PostgreSQL**
```
DB_HOST=tu-host-postgresql-render
DB_PORT=5432
DB_NAME=tu-base-datos
DB_USERNAME=tu-usuario
DB_PASSWORD=tu-password
```

### **URLs de Aplicación**
```
FRONTEND_URL=https://backend-social-media-ai.vercel.app
BACKEND_URL=https://backend-socialmedia-ixsm.onrender.com
```

### **JWT Security**
```
JWT_SECRET=tu-jwt-secret-muy-seguro-min-256-bits
JWT_EXPIRATION=86400000  # 24 horas en ms
```

### **Google OAuth2**
```
GOOGLE_CLIENT_ID=tu-google-client-id
GOOGLE_CLIENT_SECRET=tu-google-client-secret
```

### **Google Cloud (Vertex AI)**
```
GOOGLE_CLOUD_PROJECT_ID=tu-project-id
GOOGLE_CLOUD_CLIENT_EMAIL=tu-service-account@project.iam.gserviceaccount.com
GOOGLE_CLOUD_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC...\n-----END PRIVATE KEY-----\n"
GOOGLE_CLOUD_PRIVATE_KEY_ID=tu-private-key-id
GOOGLE_CLOUD_CLIENT_ID=tu-client-id
```

### **Google Generative AI**
```
GOOGLE_GENERATIVE_AI_API_KEY=tu-api-key
GOOGLE_VIDEO_API_TIMEOUT=300
```

### **Configuración de APIs**
```
VIDEO_POLLING_INTERVAL=30
VIDEO_MAX_PROCESSING_HOURS=2
YOUTUBE_API_TIMEOUT=300
YOUTUBE_MAX_RETRIES=3
YOUTUBE_RATE_LIMIT_RPM=100
YOUTUBE_QUOTA_UNITS_DAY=10000
```

## 🔧 **Configuración en Render**

### **Build & Deploy**
- **Environment**: Docker
- **Dockerfile Path**: `./Dockerfile`
- **Branch**: `main` (o tu rama de producción)

### **Environment Variables**
Copia todas las variables de arriba en la sección "Environment" de Render.

### **Health Check**
- **Health Check Path**: `/api/auth/status`
- **Health Check Timeout**: `30` segundos

### **Instance Type**
- **Instance Type**: `Starter` (para comenzar)
- **Region**: `Oregon (us-west-2)` o la más cercana a tus usuarios

## ⚠️ **Consideraciones de Seguridad**

1. **JWT_SECRET**: Genera uno seguro (256+ bits)
2. **Private Keys**: Nunca commits a repositorio
3. **Passwords**: Usa Render's secret management
4. **CORS**: Solo permite tu dominio de frontend

## 🧪 **Verificación Post-Deploy**

Después del deploy, verifica:

```bash
# Health check
curl https://backend-socialmedia-ixsm.onrender.com/api/auth/status

# Google OAuth URL
curl https://backend-socialmedia-ixsm.onrender.com/api/auth/google-url

# Logs en Render para verificar perfil 'prod'
```

## 📊 **Monitoreo**

- **Logs**: Revisa logs en Render dashboard
- **Metrics**: Usa `/actuator/health` y `/actuator/metrics`
- **Performance**: Monitorea tiempo de respuesta y uso de CPU/memoria</content>
<parameter name="filePath">C:\Users\Mao\Desktop\backend_SocialMedia\RENDER_DEPLOYMENT.md
