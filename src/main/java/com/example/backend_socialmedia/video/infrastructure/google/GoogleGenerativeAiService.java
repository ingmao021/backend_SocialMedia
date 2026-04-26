package com.example.backend_socialmedia.video.infrastructure.google;

import com.example.backend_socialmedia.shared.config.VertexAiProperties;
import com.example.backend_socialmedia.shared.exception.VideoGenerationException;
import com.example.backend_socialmedia.video.domain.VideoGenerationPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class GoogleGenerativeAiService implements VideoGenerationPort {

    private static final Logger logger = LoggerFactory.getLogger(GoogleGenerativeAiService.class);

    private static final String VEO_ENDPOINT =
            "https://us-central1-aiplatform.googleapis.com/v1/projects/%s/locations/us-central1" +
            "/publishers/google/models/veo-2.0-generate-001:predictLongRunning";

    private static final String VERTEX_OPERATIONS_BASE =
            "https://us-central1-aiplatform.googleapis.com/v1/";

    private final VertexAiProperties vertexProps;
    private final ObjectMapper objectMapper;
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

    private GoogleCredentials buildCredentials() throws IOException {
        ObjectNode credJson = objectMapper.createObjectNode();
        credJson.put("type", "service_account");
        credJson.put("project_id", vertexProps.getProjectId());
        credJson.put("private_key_id", vertexProps.getPrivateKeyId() != null ? vertexProps.getPrivateKeyId() : "");
        credJson.put("client_email", vertexProps.getClientEmail());
        credJson.put("client_id", vertexProps.getClientId() != null ? vertexProps.getClientId() : "");
        credJson.put("private_key", vertexProps.getPrivateKey().replace("\\n", "\n"));
        credJson.put("token_uri", "https://oauth2.googleapis.com/token");

        byte[] jsonBytes = objectMapper.writeValueAsBytes(credJson);

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
    }
}
