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

    private static final String HF_API_URL = "https://api-inference.huggingface.co/models/damo-vilab/text-to-video-ms-1.7b";

    @Value("${HUGGINGFACE_API_KEY}")
    private String huggingFaceApiKey;

    private final GoogleAiProperties googleAiProperties;
    private final RestTemplate restTemplate;

    public GoogleGenerativeAiService(GoogleAiProperties googleAiProperties) {
        this.googleAiProperties = googleAiProperties;
        this.restTemplate = new RestTemplate();
    }

    public GoogleVideoGenerationResponse generateVideo(String prompt) {
        try {
            logger.info("Generando video con Hugging Face. Prompt: {}", prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + huggingFaceApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("inputs", prompt);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    HF_API_URL,
                    HttpMethod.POST,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Hugging Face devuelve el video como bytes
                // Convertimos a base64 para guardarlo como URL de datos
                String base64Video = java.util.Base64.getEncoder().encodeToString(response.getBody());
                String videoDataUrl = "data:video/mp4;base64," + base64Video;

                String jobId = "hf_job_" + System.currentTimeMillis();
                logger.info("Video generado exitosamente. Job ID: {}", jobId);

                return new GoogleVideoGenerationResponse(
                        jobId,
                        "COMPLETED",
                        videoDataUrl,
                        null
                );
            } else {
                throw new RuntimeException("Respuesta inesperada de Hugging Face: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error al generar video con Hugging Face", e);
            return new GoogleVideoGenerationResponse(null, "ERROR", null, e.getMessage());
        }
    }

    public GoogleVideoGenerationResponse getJobStatus(String jobId) {
        // Con Hugging Face la generación es síncrona, siempre completed
        return new GoogleVideoGenerationResponse(jobId, "COMPLETED", null, null);
    }

    public boolean validateApiKey() {
        return huggingFaceApiKey != null && !huggingFaceApiKey.trim().isEmpty();
    }
}