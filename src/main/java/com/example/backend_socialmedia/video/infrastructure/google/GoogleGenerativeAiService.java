package com.example.backend_socialmedia.video.infrastructure.google;

import com.example.backend_socialmedia.shared.config.VertexAiProperties;
import com.example.backend_socialmedia.shared.exception.VideoGenerationException;
import com.example.backend_socialmedia.video.domain.VideoGenerationPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
<<<<<<< HEAD
import jakarta.annotation.PostConstruct;
=======
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
>>>>>>> d6a0a7953bd7015ccbaf6dc66c16201ab3f3afd1
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GoogleGenerativeAiService implements VideoGenerationPort {

    private static final Logger logger = LoggerFactory.getLogger(GoogleGenerativeAiService.class);

<<<<<<< HEAD
    private static final String VEO_ENDPOINT =
            "https://us-central1-aiplatform.googleapis.com/v1/projects/%s/locations/us-central1" +
            "/publishers/google/models/veo-2.0-generate-001:predictLongRunning";
=======
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
>>>>>>> d6a0a7953bd7015ccbaf6dc66c16201ab3f3afd1

    private static final String VERTEX_OPERATIONS_BASE =
            "https://us-central1-aiplatform.googleapis.com/v1/";

<<<<<<< HEAD
    private final VertexAiProperties vertexProps;
    private final ObjectMapper objectMapper;
=======
    @Value("${GOOGLE_CLOUD_PRIVATE_KEY}")
    private String privateKey;

    @Value("${GOOGLE_CLOUD_PRIVATE_KEY_ID}")
    private String privateKeyId;

    @Value("${GOOGLE_CLOUD_CLIENT_ID}")
    private String clientId;

    @Value("${video-generation.output-bucket:}")
    private String videoOutputBucket;

    @Value("${video-generation.signed-url-hours:12}")
    private int signedUrlExpirationHours;

    private final GoogleAiProperties googleAiProperties;
>>>>>>> d6a0a7953bd7015ccbaf6dc66c16201ab3f3afd1
    private final RestTemplate restTemplate;
    private GoogleCredentials googleCredentials;

    public GoogleGenerativeAiService(VertexAiProperties vertexProps, ObjectMapper objectMapper) {
        this.vertexProps = vertexProps;
        this.objectMapper = objectMapper;
        this.restTemplate = buildRestTemplate();
    }

    @PostConstruct
    public void init() {
        try {
            this.googleCredentials = buildCredentials();
            logger.info("Credenciales de Google Cloud inicializadas para proyecto: {}", vertexProps.getProjectId());
        } catch (IOException e) {
            logger.error("Error al inicializar credenciales de Google Cloud", e);
            throw new RuntimeException("No se pudieron inicializar las credenciales de Google: " + e.getMessage(), e);
        }
    }

    @Override
    public GenerationResult startGeneration(GenerationRequest request) {
        try {
            logger.info("Iniciando generación con Veo 2. Prompt length={}", request.prompt().length());

            String accessToken = getAccessToken();
            HttpHeaders headers = bearerHeaders(accessToken);

            Map<String, Object> body = Map.of(
                    "instances", List.of(Map.of("prompt", request.prompt())),
                    "parameters", Map.of(
                            "durationSeconds", request.durationSeconds(),
                            "aspectRatio", request.aspectRatio()
                    )
            );

            String url = VEO_ENDPOINT.formatted(vertexProps.getProjectId());
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class
            );

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("name")) {
                throw new VideoGenerationException("Respuesta inválida de Veo 2: sin campo 'name'");
            }

            String operationName = (String) responseBody.get("name");
            logger.info("Veo 2 job iniciado: operationName={}", operationName);
            return new GenerationResult(operationName);

        } catch (VideoGenerationException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al iniciar generación con Veo 2", e);
            throw new VideoGenerationException("Error al llamar a Veo 2: " + e.getMessage(), e);
        }
    }

    @Override
    public JobStatusResult getJobStatus(String jobId) {
        try {
            String accessToken = getAccessToken();
            String url = VERTEX_OPERATIONS_BASE + jobId;

            logger.debug("Consultando estado del job: {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)), Map.class
            );

            Map<?, ?> body = response.getBody();
            if (body == null) {
                return new JobStatusResult(jobId, "PROCESSING", null, null);
            }

            Boolean done = (Boolean) body.get("done");
            if (!Boolean.TRUE.equals(done)) {
                return new JobStatusResult(jobId, "PROCESSING", null, null);
            }

            if (body.containsKey("error")) {
                Map<?, ?> error = (Map<?, ?>) body.get("error");
                String errorMsg = (String) error.get("message");
                logger.error("Job {} falló: {}", jobId, errorMsg);
                return new JobStatusResult(jobId, "ERROR", null, errorMsg);
            }

            Map<?, ?> responseMap = (Map<?, ?>) body.get("response");
            if (responseMap != null) {
                List<?> predictions = (List<?>) responseMap.get("predictions");
                if (predictions != null && !predictions.isEmpty()) {
                    Map<?, ?> prediction = (Map<?, ?>) predictions.get(0);
                    String videoUrl = (String) prediction.get("gcsUri");
                    if (videoUrl == null) {
                        videoUrl = (String) prediction.get("bytesBase64Encoded");
                    }
                    logger.info("Job {} completado. videoUrl={}", jobId, videoUrl);
                    return new JobStatusResult(jobId, "COMPLETED", videoUrl, null);
                }
            }

            return new JobStatusResult(jobId, "PROCESSING", null, null);

        } catch (Exception e) {
            logger.error("Error al consultar estado del job {}: {}", jobId, e.getMessage(), e);
            return new JobStatusResult(jobId, "ERROR", null, "Error consultando estado: " + e.getMessage());
        }
    }

    @Override
    public boolean isConfigured() {
        return vertexProps.getClientEmail() != null && !vertexProps.getClientEmail().isBlank()
                && vertexProps.getProjectId() != null && !vertexProps.getProjectId().isBlank();
    }

    private String getAccessToken() throws Exception {
        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }

<<<<<<< HEAD
    private GoogleCredentials buildCredentials() throws IOException {
        ObjectNode credJson = objectMapper.createObjectNode();
        credJson.put("type", "service_account");
        credJson.put("project_id", vertexProps.getProjectId());
        credJson.put("private_key_id", vertexProps.getPrivateKeyId() != null ? vertexProps.getPrivateKeyId() : "");
        credJson.put("client_email", vertexProps.getClientEmail());
        credJson.put("client_id", vertexProps.getClientId() != null ? vertexProps.getClientId() : "");
        // Reemplaza literales \n por saltos de línea reales (común en env vars)
        credJson.put("private_key", vertexProps.getPrivateKey().replace("\\n", "\n"));
        credJson.put("token_uri", "https://oauth2.googleapis.com/token");
=======
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
>>>>>>> d6a0a7953bd7015ccbaf6dc66c16201ab3f3afd1

        byte[] jsonBytes = objectMapper.writeValueAsBytes(credJson);

<<<<<<< HEAD
        return ServiceAccountCredentials
                .fromStream(new ByteArrayInputStream(jsonBytes))
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
    }

    private HttpHeaders bearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);
        factory.setReadTimeout(300_000);
        return new RestTemplate(factory);
=======
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
            
            if (videoOutputBucket != null && !videoOutputBucket.isEmpty()) {
                Map<String, Object> outputConfig = new HashMap<>();
                Map<String, Object> gcsDestination = new HashMap<>();
                gcsDestination.put("uri", videoOutputBucket);
                outputConfig.put("gcsDestination", gcsDestination);
                body.put("outputConfig", outputConfig);
                logger.info("Output bucket configurado: {}", videoOutputBucket);
            }

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
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Para Veo 3, debemos usar el endpoint fetchPredictOperation (POST)
            // en lugar de GET a la URL de operaciones
            String url;
            String requestBody;
            
            if (operationName.contains("/publishers/google/models/")) {
                // Extraer el modelo del operationName
                // Formato: projects/{p}/locations/{l}/publishers/google/models/{model}/operations/{uuid}
                String modelPath = operationName.substring(0, operationName.indexOf("/operations/"));
                
                url = String.format("https://%s-aiplatform.googleapis.com/%s/%s:fetchPredictOperation",
                        VEO3_LOCATION, VEO3_API_VERSION, modelPath);
                
                // El body debe contener el operationName completo
                requestBody = String.format("{\"operationName\": \"%s\"}", operationName);
            } else {
                // Fallback para operaciones estándar (compatibilidad hacia atrás)
                String operationId = operationName;
                if (operationName.contains("/operations/")) {
                    operationId = operationName.substring(operationName.lastIndexOf("/") + 1);
                }
                url = String.format(
                        "https://%s-aiplatform.googleapis.com/%s/projects/%s/locations/%s/operations/%s",
                        VEO3_LOCATION, VEO3_API_VERSION, projectId, VEO3_LOCATION, operationId);
                requestBody = null;
            }

            logger.debug("Consultando estado en URL: {}", url);

            ResponseEntity<Map> response;
            if (requestBody != null) {
                // POST para fetchPredictOperation (Veo 3)
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
                response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            } else {
                // GET para operaciones estándar
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            }

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
                    logger.info("Respuesta completa de Google: {}", responseBody);
                    
                    if (responseMap != null) {
                        logger.info("Response map: {}", responseMap);
                        
                        // Veo 3 usa "generateVideoResponse" -> "generatedSamples"
                        Map generateVideoResponse = (Map) responseMap.get("generateVideoResponse");
                        if (generateVideoResponse != null) {
                            logger.info("generateVideoResponse: {}", generateVideoResponse);
                            List generatedSamples = (List) generateVideoResponse.get("generatedSamples");
                            if (generatedSamples != null && !generatedSamples.isEmpty()) {
                                Map sample = (Map) generatedSamples.get(0);
                                logger.info("Sample: {}", sample);
                                Map videoObj = (Map) sample.get("video");
                                if (videoObj != null) {
                                    String videoUrl = extractVideoUrl(videoObj);
                                    logger.info("Video generado exitosamente. URL: {}", videoUrl);
                                    return new GoogleVideoGenerationResponse(operationName, "COMPLETED", videoUrl, null);
                                }
                            }
                        }
                        
                        // Fallback: Veo 3 formato "videos"
                        List videos = (List) responseMap.get("videos");
                        if (videos != null && !videos.isEmpty()) {
                            Map video = (Map) videos.get(0);
                            logger.info("Video object: {}", video);
                            String videoUrl = extractVideoUrl(video);
                            logger.info("Video generado exitosamente. URL: {}", videoUrl);
                            return new GoogleVideoGenerationResponse(operationName, "COMPLETED", videoUrl, null);
                        }
                        
                        // Fallback para formato anterior (predictions)
                        List predictions = (List) responseMap.get("predictions");
                        if (predictions != null && !predictions.isEmpty()) {
                            Map prediction = (Map) predictions.get(0);
                            logger.info("Prediction object: {}", prediction);
                            String videoUrl = extractVideoUrl(prediction);
                            logger.info("Video generado exitosamente. URL: {}", videoUrl);
                            return new GoogleVideoGenerationResponse(operationName, "COMPLETED", videoUrl, null);
                        }
                        
                        logger.warn("Estructura de respuesta no reconocida. Keys: {}", responseMap.keySet());
                    }
                    
                    return new GoogleVideoGenerationResponse(operationName, "ERROR", null, 
                        "Operación completada pero sin video en la respuesta. Response: " + responseBody);
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

    /**
     * Extrae la URL del video de diferentes formatos de respuesta de Google
     */
    private String extractVideoUrl(Map videoObj) {
        if (videoObj == null) return null;
        
        String[] possibleFields = {
            "gcsUri",       // Google Cloud Storage URI - PRIORIDAD
            "uri",          // URI general
            "storageUri",   // Storage URI
            "videoUri"      // Video URI
            // NO incluir bytesBase64Encoded - es muy largo
        };
        
        for (String field : possibleFields) {
            Object value = videoObj.get(field);
            if (value != null && value instanceof String && !((String) value).isEmpty()) {
                return (String) value;
            }
        }
        
        logger.warn("No se encontró URL (GCS URI) en el objeto video. Keys disponibles: {}", videoObj.keySet());
        return null;
    }
    
    /**
     * Genera una Signed URL para descargar un video desde Google Cloud Storage
     */
    public String generateSignedUrl(String gcsUri, int expirationHours) {
        try {
            logger.info("Generando signed URL para: {}", gcsUri);
            
            // gcsUri ejemplo: gs://veo3-videos-social/videos/abc123.mp4
            String uriWithoutGs = gcsUri.replace("gs://", "");
            int slashIndex = uriWithoutGs.indexOf("/");
            
            if (slashIndex == -1) {
                logger.error("GCS URI inválido: {}", gcsUri);
                return null;
            }
            
            String bucketName = uriWithoutGs.substring(0, slashIndex);
            String objectName = uriWithoutGs.substring(slashIndex + 1);
            
            logger.debug("Bucket: {}, Object: {}", bucketName, objectName);
            
            // Crear credentials desde service account
            String serviceAccountJson = createServiceAccountJson();
            GoogleCredentials credentials = ServiceAccountCredentials
                    .fromStream(new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
            
            // Crear Storage client
            Storage storage = StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .setCredentials(credentials)
                    .build()
                    .getService();
            
            // Generar signed URL
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
            
            java.net.URL signedUrl = storage.signUrl(blobInfo, expirationHours, TimeUnit.HOURS);
            String url = signedUrl.toString();
            
            logger.info("Signed URL generada exitosamente: {}", url.substring(0, Math.min(50, url.length())) + "...");
            return url;
            
        } catch (Exception e) {
            logger.error("Error al generar signed URL para: {}", gcsUri, e);
            return null;
        }
    }
    
    private String createServiceAccountJson() {
        return String.format("""
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
>>>>>>> d6a0a7953bd7015ccbaf6dc66c16201ab3f3afd1
    }
}
