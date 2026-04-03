package com.example.backend_socialmedia.video.infrastructure.google;

import com.example.backend_socialmedia.shared.config.GoogleAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleGenerativeAiService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleGenerativeAiService.class);
    private static final String REPLICATE_API_URL = "https://api.replicate.com/v1/models/wan-ai/wan2.1-t2v-turbo/predictions";

    @Value("${REPLICATE_API_KEY}")
    private String replicateApiKey;

    private final GoogleAiProperties googleAiProperties;
    private final RestTemplate restTemplate;

    public GoogleGenerativeAiService(GoogleAiProperties googleAiProperties) {
        this.googleAiProperties = googleAiProperties;
        this.restTemplate = new RestTemplate();
    }

    public GoogleVideoGenerationResponse generateVideo(String prompt) {
        try {
            logger.info("Generando video con Replicate. Prompt: {}", prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + replicateApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Prefer", "wait");

            Map<String, Object> input = new HashMap<>();
            input.put("prompt", prompt);
            input.put("num_frames", 16);
            input.put("fps", 8);

            Map<String, Object> body = new HashMap<>();
            body.put("input", input);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    REPLICATE_API_URL,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map responseBody = response.getBody();
            logger.info("Respuesta de Replicate: {}", responseBody);

            if (responseBody != null) {
                String status = (String) responseBody.get("status");
                String jobId = (String) responseBody.get("id");

                Object output = responseBody.get("output");
                String videoUrl = null;

                if (output instanceof java.util.List) {
                    java.util.List outputList = (java.util.List) output;
                    if (!outputList.isEmpty()) {
                        videoUrl = outputList.get(0).toString();
                    }
                } else if (output instanceof String) {
                    videoUrl = (String) output;
                }

                String mappedStatus = "COMPLETED".equals(status) ? "COMPLETED" : "PROCESSING";

                return new GoogleVideoGenerationResponse(jobId, mappedStatus, videoUrl, null);
            }

            throw new RuntimeException("Respuesta vacía de Replicate");

        } catch (Exception e) {
            logger.error("Error al generar video con Replicate", e);
            return new GoogleVideoGenerationResponse(null, "ERROR", null, e.getMessage());
        }
    }

    public GoogleVideoGenerationResponse getJobStatus(String jobId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + replicateApiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.replicate.com/v1/predictions/" + jobId,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody != null) {
                String status = (String) responseBody.get("status");
                Object output = responseBody.get("output");
                String videoUrl = null;

                if (output instanceof java.util.List) {
                    java.util.List outputList = (java.util.List) output;
                    if (!outputList.isEmpty()) {
                        videoUrl = outputList.get(0).toString();
                    }
                }

                String mappedStatus = "succeeded".equals(status) ? "COMPLETED" :
                        "failed".equals(status) ? "ERROR" : "PROCESSING";

                return new GoogleVideoGenerationResponse(jobId, mappedStatus, videoUrl, null);
            }

            return new GoogleVideoGenerationResponse(jobId, "PROCESSING", null, null);

        } catch (Exception e) {
            logger.error("Error al obtener estado del job: {}", jobId, e);
            return new GoogleVideoGenerationResponse(jobId, "ERROR", null, e.getMessage());
        }
    }

    public boolean validateApiKey() {
        return replicateApiKey != null && !replicateApiKey.trim().isEmpty();
    }
}