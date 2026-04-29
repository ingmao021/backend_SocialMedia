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
     *
     * @throws VideoGenerationException if Vertex returns an HTTP error (401, 403, 500, etc.)
     */
    public OperationResponse getOperation(String operationName) {
        try {
            googleCredentials.refreshIfExpired();
            String accessToken = googleCredentials.getAccessToken().getTokenValue();

            String location = appProperties.getGcp().getLocation();
            String url = String.format("https://%s-aiplatform.googleapis.com/v1/%s",
                    location, operationName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Vertex AI getOperation failed: status={} body={}", response.statusCode(), response.body());
                throw new VideoGenerationException(
                        "Error consultando operación de Vertex AI: status=" + response.statusCode());
            }

            return objectMapper.readValue(response.body(), OperationResponse.class);

        } catch (IOException | InterruptedException e) {
            log.error("Error polling Vertex AI operation: {}", operationName, e);
            throw new VideoGenerationException("Error comunicándose con Vertex AI al consultar operación", e);
        }
    }
}
