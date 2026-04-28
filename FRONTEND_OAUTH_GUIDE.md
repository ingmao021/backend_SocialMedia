# 🔐 **Flujo de Autenticación OAuth2 - Guía para Frontend**

## 📋 **Nuevo Flujo Seguro (Recomendado)**

### **Paso 1: Iniciar Login**
```javascript
// Obtener URL de login de Google
const response = await fetch('https://backend-socialmedia-ixsm.onrender.com/api/auth/google-url');
const { url } = await response.json();

// Redirigir al usuario a Google
window.location.href = url;
```

### **Paso 2: Google redirige al backend**
- Google redirige a: `https://backend-socialmedia-ixsm.onrender.com/login/oauth2/code/google`
- Backend procesa tokens y redirige a: `https://backend-social-media-ai.vercel.app/auth/callback`

### **Paso 3: Frontend recibe callback**
```javascript
// En tu componente /auth/callback
useEffect(() => {
  const fetchToken = async () => {
    try {
      const response = await fetch('https://backend-socialmedia-ixsm.onrender.com/api/auth/callback/token', {
        credentials: 'include'  // Importante: enviar cookies de sesión
      });

      if (response.ok) {
        const data = await response.json();
        if (data.success) {
          // Guardar token y datos de usuario
          localStorage.setItem('token', data.token);
          localStorage.setItem('user', JSON.stringify(data.user));

          // Redirigir a dashboard
          navigate('/dashboard');
        }
      } else {
        console.error('Error obteniendo token');
        navigate('/login?error=auth_failed');
      }
    } catch (error) {
      console.error('Error de red:', error);
      navigate('/login?error=network_error');
    }
  };

  fetchToken();
}, []);
```

## 🔍 **Respuesta del Endpoint `/api/auth/callback/token`**

### **Éxito (200):**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "user": {
    "id": 3,
    "email": "andersonnn12345@gmail.com",
    "name": "Anderson",
    "picture": "https://lh3.googleusercontent.com/..."
  }
}
```

### **Error (401/500):**
```json
{
  "success": false,
  "error": "No autenticado. Debe completar OAuth2 primero."
}
```

## ⚠️ **Consideraciones Importantes**

1. **CORS**: Asegurarse que `https://backend-social-media-ai.vercel.app` esté en `allowedOrigins`
2. **Cookies**: El endpoint requiere cookies de sesión (`credentials: 'include'`)
3. **Sesión**: La sesión OAuth2 debe estar activa (usuario acaba de ser redirigido)
4. **Timeout**: Llamar inmediatamente después del redirect, antes que expire la sesión

## 🐛 **Debugging**

### **Verificar estado de autenticación:**
```javascript
// Endpoint de debug
const response = await fetch('https://backend-socialmedia-ixsm.onrender.com/api/auth/debug/token', {
  headers: { 'Authorization': `Bearer ${token}` }
});
const debug = await response.json();
console.log(debug);
```

### **Verificar conectividad:**
```javascript
const response = await fetch('https://backend-socialmedia-ixsm.onrender.com/api/auth/status');
console.log(await response.json()); // { "status": "ok", "message": "..." }
```

## 📝 **URLs de Producción**

- **Backend**: `https://backend-socialmedia-ixsm.onrender.com`
- **Frontend**: `https://backend-social-media-ai.vercel.app`
- **Google OAuth Redirect**: `https://backend-socialmedia-ixsm.onrender.com/login/oauth2/code/google`</content>
<parameter name="filePath">C:\Users\Mao\Desktop\backend_SocialMedia\FRONTEND_OAUTH_GUIDE.md
