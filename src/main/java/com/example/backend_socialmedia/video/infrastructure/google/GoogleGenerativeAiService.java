package com.example.backend_socialmedia.video.infrastructure.google;

import com.example.backend_socialmedia.shared.config.GoogleAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoogleGenerativeAiService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleGenerativeAiService.class);
    private static final String VEO_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/veo-2.0-generate-001:predictLongRunning";

    @Value("${google.generative-ai-api-key}")
    private String apiKey;

    private final GoogleAiProperties googleAiProperties;
    private final RestTemplate restTemplate;

    public GoogleGenerativeAiService(GoogleAiProperties googleAiProperties) {
        this.googleAiProperties = googleAiProperties;
        this.restTemplate = new RestTemplate();
    }

    public GoogleVideoGenerationResponse generateVideo(String prompt) {
        try {
            logger.info("Generando video con Veo 2. Prompt: {}", prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> videoConfig = new HashMap<>();
            videoConfig.put("durationSeconds", 5);
            videoConfig.put("aspectRatio", "16:9");

            Map<String, Object> instance = new HashMap<>();
            instance.put("prompt", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("instances", List.of(instance));
            body.put("parameters", videoConfig);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    VEO_API_URL + "?key=" + apiKey,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map responseBody = response.getBody();
            logger.info("Respuesta de Veo 2: {}", responseBody);

            if (responseBody != null) {
                String operationName = (String) responseBody.get("name");
                logger.info("Operación iniciada: {}", operationName);
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://generativelanguage.googleapis.com/v1beta/" + operationName + "?key=" + apiKey,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody != null) {
                Boolean done = (Boolean) responseBody.get("done");

                if (Boolean.TRUE.equals(done)) {
                    Map responseMap = (Map) responseBody.get("response");
                    if (responseMap != null) {
                        List predictions = (List) responseMap.get("predictions");
                        if (predictions != null && !predictions.isEmpty()) {
                            Map prediction = (Map) predictions.get(0);
                            String videoUrl = (String) prediction.get("bytesBase64Encoded");
                            return new GoogleVideoGenerationResponse(operationName, "COMPLETED", videoUrl, null);
                        }
                    }
                }

                return new GoogleVideoGenerationResponse(operationName, "PROCESSING", null, null);
            }

            return new GoogleVideoGenerationResponse(operationName, "PROCESSING", null, null);

        } catch (Exception e) {
            logger.error("Error al obtener estado de operación: {}", operationName, e);
            return new GoogleVideoGenerationResponse(operationName, "ERROR", null, e.getMessage());
        }
    }

    public boolean validateApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}