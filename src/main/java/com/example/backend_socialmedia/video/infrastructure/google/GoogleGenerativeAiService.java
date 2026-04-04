package com.example.backend_socialmedia.video.infrastructure.google;

import com.example.backend_socialmedia.shared.config.GoogleAiProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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

    public GoogleVideoGenerationResponse generateVideo(String prompt) {
        try {
            logger.info("Generando video con Veo 2. Prompt: {}", prompt);

            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> instance = new HashMap<>();
            instance.put("prompt", prompt);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("durationSeconds", 5);
            parameters.put("aspectRatio", "16:9");

            Map<String, Object> body = new HashMap<>();
            body.put("instances", List.of(instance));
            body.put("parameters", parameters);

            String url = String.format(
                    "https://us-central1-aiplatform.googleapis.com/v1/projects/%s/locations/us-central1/publishers/google/models/veo-2.0-generate-001:predictLongRunning",
                    projectId
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map responseBody = response.getBody();
            logger.info("Respuesta de Veo 2: {}", responseBody);

            if (responseBody != null) {
                String operationName = (String) responseBody.get("name");
                return new GoogleVideoGenerationResponse(operationName, "PROCESSING", null, null);
            }

            throw new RuntimeException("Respuesta vacía de Veo 2");

        } catch (Exception e) {
            logger.error("Error al generar video con Veo 2", e);
            return new GoogleVideoGenerationResponse(null, "ERROR", null, e.getMessage());
        }
    }

    public GoogleVideoGenerationResponse getJobStatus(String operationName) {
        try {
            String accessToken = getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = "https://us-central1-aiplatform.googleapis.com/v1/" + operationName;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map responseBody = response.getBody();
            if (responseBody != null) {
                Boolean done = (Boolean) responseBody.get("done");

                if (Boolean.TRUE.equals(done)) {
                    Map responseMap = (Map) responseBody.get("response");
                    if (responseMap != null) {
                        List predictions = (List) responseMap.get("predictions");
                        if (predictions != null && !predictions.isEmpty()) {
                            Map prediction = (Map) predictions.get(0);
                            String videoUrl = (String) prediction.get("gcsUri");
                            if (videoUrl == null) {
                                videoUrl = (String) prediction.get("bytesBase64Encoded");
                            }
                            return new GoogleVideoGenerationResponse(operationName, "COMPLETED", videoUrl, null);
                        }
                    }
                }
                return new GoogleVideoGenerationResponse(operationName, "PROCESSING", null, null);
            }

            return new GoogleVideoGenerationResponse(operationName, "PROCESSING", null, null);

        } catch (Exception e) {
            logger.error("Error al obtener estado: {}", operationName, e);
            return new GoogleVideoGenerationResponse(operationName, "ERROR", null, e.getMessage());
        }
    }

    public boolean validateApiKey() {
        return clientEmail != null && !clientEmail.trim().isEmpty();
    }
}