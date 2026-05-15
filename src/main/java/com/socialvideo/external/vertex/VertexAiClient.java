package com.socialvideo.external.vertex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.socialvideo.config.AppProperties;
import com.socialvideo.exception.VideoGenerationException;
import com.socialvideo.external.vertex.dto.OperationResponse;
import com.socialvideo.external.vertex.dto.PredictLongRunningRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class VertexAiClient {

    private final GoogleCredentials googleCredentials;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public VertexAiClient(GoogleCredentials googleCredentials,
                          AppProperties appProperties,
                          ObjectMapper objectMapper) {
        this.googleCredentials = googleCredentials;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Calls predictLongRunning on Veo 3.1 Lite.
     *
     * @return the operation name for polling
     */
    public String predictLongRunning(String prompt, int durationSeconds,
                                     Long userId, UUID videoId) {
        try {
            googleCredentials.refreshIfExpired();
            String accessToken = googleCredentials.getAccessToken().getTokenValue();

            String projectId = appProperties.getGcp().getProjectId();
            String location = appProperties.getGcp().getLocation();
            String modelId = appProperties.getVertex().getVeoModelId();
            String bucket = appProperties.getGcp().getBucket();

            String storageUri = String.format("gs://%s/videos/%d/%s/", bucket, userId, videoId);

            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("durationSeconds", durationSeconds);
            parameters.put("resolution", "720p");
            parameters.put("aspectRatio", "16:9");
            parameters.put("sampleCount", 1);
            parameters.put("storageUri", storageUri);
            parameters.put("personGeneration", "allow_adult");

            PredictLongRunningRequest requestBody = new PredictLongRunningRequest(
                    List.of(Map.of("prompt", prompt)),
                    parameters
            );

            String url = String.format(
                    "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predictLongRunning",
                    location, projectId, location, modelId
            );

            String body = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Vertex AI predictLongRunning failed: status={} body={}", response.statusCode(), response.body());
                throw new VideoGenerationException("Error al iniciar generación de video: " + response.statusCode());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
            String operationName = (String) responseMap.get("name");

            if (operationName == null) {
                throw new VideoGenerationException("Vertex AI no devolvió operation name");
            }

            log.info("Video generation started: operation={}", operationName);
            return operationName;

        } catch (IOException | InterruptedException e) {
            throw new VideoGenerationException("Error comunicándose con Vertex AI", e);
        }
    }

    /**
     * Polls the operation status.
     * <p>
     * Publisher model operations (Veo, Imagen) use a UUID-based operation ID and
     * must be polled via fetchPredictOperation (POST). The standard LRO endpoint
     * (GET /operations/{id}) only accepts numeric Long IDs and will return 400
     * INVALID_ARGUMENT for UUID-based operation IDs.
     */
    public OperationResponse getOperation(String operationName) {
        if (operationName.contains("/publishers/google/models/")) {
            return fetchPredictOperation(operationName);
        }
        return getStandardOperation(operationName);
    }

    /**
     * Polls a publisher model operation via fetchPredictOperation.
     * Endpoint: POST /v1/{modelPath}:fetchPredictOperation
     * Body: {"operationName": "{full operation name}"}
     */
    private OperationResponse fetchPredictOperation(String operationName) {
        try {
            googleCredentials.refreshIfExpired();
            String accessToken = googleCredentials.getAccessToken().getTokenValue();

            String location = appProperties.getGcp().getLocation();

            // Extract model path (everything before /operations/{id})
            int operationsIdx = operationName.lastIndexOf("/operations/");
            String modelPath = operationsIdx >= 0
                    ? operationName.substring(0, operationsIdx)
                    : operationName;

            String url = String.format(
                    "https://%s-aiplatform.googleapis.com/v1/%s:fetchPredictOperation",
                    location, modelPath);

            String body = objectMapper.writeValueAsString(Map.of("operationName", operationName));

            log.info("Polling Vertex AI publisher operation via fetchPredictOperation: url={}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                log.error("Vertex AI publisher operation not found (404): {}. Treating as failed.", operationName);
                return new OperationResponse(operationName, true, null,
                        new OperationResponse.OperationError(404, "Operation not found: " + operationName));
            }

            if (response.statusCode() != 200) {
                log.error("Vertex AI fetchPredictOperation failed: status={} body={}", response.statusCode(), response.body());
                throw new VideoGenerationException(
                        "Error consultando operación de Vertex AI: status=" + response.statusCode());
            }

            return objectMapper.readValue(response.body(), OperationResponse.class);

        } catch (IOException | InterruptedException e) {
            log.error("Error polling Vertex AI publisher operation: {}", operationName, e);
            throw new VideoGenerationException("Error comunicándose con Vertex AI al consultar operación", e);
        }
    }

    /**
     * Polls a standard LRO operation (numeric Long ID).
     * Endpoint: GET /v1/projects/{p}/locations/{l}/operations/{longId}
     */
    private OperationResponse getStandardOperation(String operationName) {
        try {
            googleCredentials.refreshIfExpired();
            String accessToken = googleCredentials.getAccessToken().getTokenValue();

            String location = appProperties.getGcp().getLocation();
            String projectId = appProperties.getGcp().getProjectId();

            int idx = operationName.lastIndexOf("/operations/");
            String operationId = idx >= 0
                    ? operationName.substring(idx + "/operations/".length())
                    : operationName;

            String url = String.format(
                    "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/operations/%s",
                    location, projectId, location, operationId);

            log.info("Polling Vertex AI standard operation: url={}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                log.error("Vertex AI operation not found (404): {}. Treating as failed.", operationName);
                return new OperationResponse(operationName, true, null,
                        new OperationResponse.OperationError(404, "Operation not found: " + operationId));
            }

            if (response.statusCode() != 200) {
                log.error("Vertex AI getOperation failed: status={} body={}", response.statusCode(), response.body());
                throw new VideoGenerationException(
                        "Error consultando operación de Vertex AI: status=" + response.statusCode());
            }

            return objectMapper.readValue(response.body(), OperationResponse.class);

        } catch (IOException | InterruptedException e) {
            log.error("Error polling Vertex AI standard operation: {}", operationName, e);
            throw new VideoGenerationException("Error comunicándose con Vertex AI al consultar operación", e);
        }
    }
}
