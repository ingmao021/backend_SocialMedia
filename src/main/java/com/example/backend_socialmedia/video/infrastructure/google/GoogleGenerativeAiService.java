package com.example.backend_socialmedia.video.infrastructure.google;

import com.example.backend_socialmedia.shared.config.GoogleAiProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoogleGenerativeAiService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleGenerativeAiService.class);

    // Configuración Veo 3 Fast
    private static final String VEO3_MODEL_ID = "veo-3.0-fast-generate-001";
    private static final String VEO3_LOCATION = "us-central1";
    private static final String VEO3_API_VERSION = "v1";
    
    // Valores por defecto para Veo 3
    private static final int DEFAULT_DURATION_SECONDS = 4;
    private static final String DEFAULT_ASPECT_RATIO = "16:9";
    private static final String DEFAULT_RESOLUTION = "720p";
    private static final int DEFAULT_SAMPLE_COUNT = 1;

    @Value("${GOOGLE_CLOUD_PROJECT_ID}")
    private String projectId;

    @Value("${GOOGLE_CLOUD_CLIENT_EMAIL}")
    private String clientEmail;

    @Value("${GOOGLE_CLOUD_PRIVATE_KEY}")
    private String privateKey;

    @Value("${GOOGLE_CLOUD_PRIVATE_KEY_ID}")
    private String privateKeyId;

    @Value("${GOOGLE_CLOUD_CLIENT_ID}")
    private String clientId;

    private final GoogleAiProperties googleAiProperties;
    private final RestTemplate restTemplate;

    public GoogleGenerativeAiService(GoogleAiProperties googleAiProperties) {
        this.googleAiProperties = googleAiProperties;
        this.restTemplate = new RestTemplate();
    }

    private String getAccessToken() throws Exception {
        String json = String.format("""
            {
              "type": "service_account",
              "project_id": "%s",
              "private_key_id": "%s",
              "client_email": "%s",
              "client_id": "%s",
              "private_key": "%s",
              "token_uri": "https://oauth2.googleapis.com/token"
            }
            """, projectId, privateKeyId, clientEmail, clientId, privateKey.replace("\\n", "\n"));

        GoogleCredentials credentials = ServiceAccountCredentials
                .fromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));

        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

    /**
     * Genera un video usando Veo 3 Fast con parámetros por defecto
     */
    public GoogleVideoGenerationResponse generateVideo(String prompt) {
        return generateVideo(prompt, DEFAULT_DURATION_SECONDS, DEFAULT_ASPECT_RATIO, DEFAULT_RESOLUTION);
    }

    /**
     * Genera un video usando Veo 3 Fast con parámetros personalizados
     */
    public GoogleVideoGenerationResponse generateVideo(String prompt, int durationSeconds, 
                                                        String aspectRatio, String resolution) {
        try {
            logger.info("Generando video con Veo 3 Fast. Prompt: {}, Duración: {}s, Aspecto: {}, Resolución: {}", 
                       prompt, durationSeconds, aspectRatio, resolution);

            validateParameters(durationSeconds, aspectRatio, resolution);

            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> instance = new HashMap<>();
            instance.put("prompt", prompt);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("sampleCount", DEFAULT_SAMPLE_COUNT);
            parameters.put("durationSeconds", durationSeconds);
            parameters.put("aspectRatio", aspectRatio);
            parameters.put("resolution", resolution);

            Map<String, Object> body = new HashMap<>();
            body.put("instances", List.of(instance));
            body.put("parameters", parameters);

            String url = String.format(
                    "https://%s-aiplatform.googleapis.com/%s/projects/%s/locations/%s/publishers/google/models/%s:predictLongRunning",
                    VEO3_LOCATION, VEO3_API_VERSION, projectId, VEO3_LOCATION, VEO3_MODEL_ID
            );

            logger.debug("Request URL: {}", url);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map responseBody = response.getBody();
            logger.info("Respuesta de Veo 3 Fast: {}", responseBody);

            if (responseBody != null) {
                String operationName = (String) responseBody.get("name");
                if (operationName != null) {
                    return new GoogleVideoGenerationResponse(operationName, "PROCESSING", null, null);
                }
            }

            throw new RuntimeException("Respuesta vacía de Veo 3 Fast");

        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            logger.error("Error de cliente al generar video ({}): {}", e.getStatusCode(), errorBody);
            return handleHttpError(e.getStatusCode().value(), errorBody);
            
        } catch (HttpServerErrorException e) {
            logger.error("Error de servidor al generar video ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return new GoogleVideoGenerationResponse(null, "ERROR", null, 
                "Error del servidor de Google: " + e.getStatusCode());
                
        } catch (Exception e) {
            logger.error("Error al generar video con Veo 3 Fast", e);
            return new GoogleVideoGenerationResponse(null, "ERROR", null, e.getMessage());
        }
    }

    public GoogleVideoGenerationResponse getJobStatus(String operationName) {
        try {
            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Para Veo 3, el operationName ya contiene el path completo:
            // projects/{project}/locations/{location}/publishers/google/models/{model}/operations/{uuid}
            // Debemos usar ese path directamente, no la ruta de operaciones estándar
            String url;
            if (operationName.contains("/publishers/google/models/")) {
                // Es una operación de modelo generativo (Veo 3)
                url = String.format("https://%s-aiplatform.googleapis.com/%s/%s",
                        VEO3_LOCATION, VEO3_API_VERSION, operationName);
            } else {
                // Fallback para operaciones estándar (compatibilidad hacia atrás)
                String operationId = operationName;
                if (operationName.contains("/operations/")) {
                    operationId = operationName.substring(operationName.lastIndexOf("/") + 1);
                }
                url = String.format(
                        "https://%s-aiplatform.googleapis.com/%s/projects/%s/locations/%s/operations/%s",
                        VEO3_LOCATION, VEO3_API_VERSION, projectId, VEO3_LOCATION, operationId);
            }

            logger.debug("Consultando estado en URL: {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map responseBody = response.getBody();
            if (responseBody != null) {
                Boolean done = (Boolean) responseBody.get("done");

                if (Boolean.TRUE.equals(done)) {
                    // Verificar si hay error
                    Map error = (Map) responseBody.get("error");
                    if (error != null) {
                        String errorMessage = (String) error.get("message");
                        logger.error("Operación completada con error: {}", errorMessage);
                        return new GoogleVideoGenerationResponse(operationName, "ERROR", null, errorMessage);
                    }

                    Map responseMap = (Map) responseBody.get("response");
                    if (responseMap != null) {
                        List predictions = (List) responseMap.get("predictions");
                        if (predictions != null && !predictions.isEmpty()) {
                            Map prediction = (Map) predictions.get(0);
                            String videoUrl = (String) prediction.get("gcsUri");
                            if (videoUrl == null) {
                                videoUrl = (String) prediction.get("bytesBase64Encoded");
                            }
                            logger.info("Video generado exitosamente. URL: {}", videoUrl);
                            return new GoogleVideoGenerationResponse(operationName, "COMPLETED", videoUrl, null);
                        }
                    }
                    
                    return new GoogleVideoGenerationResponse(operationName, "ERROR", null, 
                        "Operación completada pero sin video en la respuesta");
                }
                return new GoogleVideoGenerationResponse(operationName, "PROCESSING", null, null);
            }

            return new GoogleVideoGenerationResponse(operationName, "PROCESSING", null, null);

        } catch (HttpClientErrorException e) {
            logger.error("Error al consultar estado ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 404) {
                return new GoogleVideoGenerationResponse(operationName, "ERROR", null, 
                    "Operación no encontrada. Puede haber expirado.");
            }
            return new GoogleVideoGenerationResponse(operationName, "ERROR", null, e.getMessage());
        } catch (Exception e) {
            logger.error("Error al obtener estado: {}", operationName, e);
            return new GoogleVideoGenerationResponse(operationName, "ERROR", null, e.getMessage());
        }
    }

    public boolean validateApiKey() {
        boolean isValid = clientEmail != null && !clientEmail.trim().isEmpty()
                       && privateKey != null && !privateKey.trim().isEmpty()
                       && projectId != null && !projectId.trim().isEmpty();
        
        if (!isValid) {
            logger.warn("Credenciales de Google Cloud incompletas");
        }
        return isValid;
    }

    private void validateParameters(int durationSeconds, String aspectRatio, String resolution) {
        if (durationSeconds != 4 && durationSeconds != 6 && durationSeconds != 8) {
            throw new IllegalArgumentException(
                "Duración inválida: " + durationSeconds + ". Veo 3 soporta: 4, 6, u 8 segundos");
        }
        if (!"16:9".equals(aspectRatio) && !"9:16".equals(aspectRatio)) {
            throw new IllegalArgumentException(
                "Aspect ratio inválido: " + aspectRatio + ". Veo 3 soporta: 16:9 o 9:16");
        }
        if (!"720p".equals(resolution) && !"1080p".equals(resolution)) {
            throw new IllegalArgumentException(
                "Resolución inválida: " + resolution + ". Veo 3 soporta: 720p o 1080p");
        }
    }

    private GoogleVideoGenerationResponse handleHttpError(int statusCode, String errorBody) {
        String message;
        switch (statusCode) {
            case 400:
                message = errorBody.contains("safety") 
                    ? "El prompt fue bloqueado por filtros de seguridad." 
                    : "Solicitud inválida. Verifica los parámetros.";
                break;
            case 401:
                message = "Credenciales inválidas. Verifica tu Service Account.";
                break;
            case 403:
                message = errorBody.contains("billing") 
                    ? "Facturación no habilitada en Google Cloud." 
                    : "Permisos insuficientes. Verifica los roles IAM.";
                break;
            case 404:
                message = "Modelo no encontrado. Verifica que Veo 3 esté disponible.";
                break;
            case 429:
                message = "Límite de cuota alcanzado. Intenta de nuevo en unos minutos.";
                break;
            default:
                message = "Error " + statusCode + ": " + errorBody;
        }
        return new GoogleVideoGenerationResponse(null, "ERROR", null, message);
    }
}