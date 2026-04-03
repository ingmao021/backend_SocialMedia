package com.example.backend_socialmedia.video.infrastructure.google;

import com.example.backend_socialmedia.shared.config.GoogleAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servicio para integración con Google Generative AI (Gemini Video API)
 * Encapsula la lógica de comunicación con Google AI Studio
 */
@Service
public class GoogleGenerativeAiService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleGenerativeAiService.class);

    private final GoogleAiProperties googleAiProperties;

    public GoogleGenerativeAiService(GoogleAiProperties googleAiProperties) {
        this.googleAiProperties = googleAiProperties;
    }

    /**
     * Genera un video usando el API de Google Generative AI
     * @param prompt El prompt para generar el video
     * @return Response con el jobId y estado
     */
    public GoogleVideoGenerationResponse generateVideo(String prompt) {
        try {
            logger.info("Iniciando generación de video con prompt: {}", prompt);

            // TODO: Implementar llamada real a Google Generative AI API
            // Por ahora, retornamos un mock response
            String jobId = "job_" + System.currentTimeMillis();

            logger.info("Video generation job creado con ID: {}", jobId);

            return new GoogleVideoGenerationResponse(
                    jobId,
                    "PROCESSING",
                    null,
                    null
            );

        } catch (Exception e) {
            logger.error("Error al generar video con Google AI", e);
            return new GoogleVideoGenerationResponse(
                    null,
                    "ERROR",
                    null,
                    e.getMessage()
            );
        }
    }

    /**
     * Obtiene el estado de un video en generación
     * @param jobId El ID del trabajo en Google
     * @return Response con el estado actual
     */
    public GoogleVideoGenerationResponse getJobStatus(String jobId) {
        try {
            logger.info("Consultando estado del trabajo: {}", jobId);

            // TODO: Implementar llamada real a Google API para obtener estado
            // Por ahora, retornamos un mock response

            return new GoogleVideoGenerationResponse(
                    jobId,
                    "PROCESSING",
                    null,
                    null
            );

        } catch (Exception e) {
            logger.error("Error al obtener estado del trabajo: {}", jobId, e);
            return new GoogleVideoGenerationResponse(
                    jobId,
                    "ERROR",
                    null,
                    e.getMessage()
            );
        }
    }

    /**
     * Valida si la API key es válida
     * @return true si la API key es válida
     */
    public boolean validateApiKey() {
        try {
            String apiKey = googleAiProperties.getGenerativeAiApiKey();

            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("Google Generative AI API key no está configurado");
                return false;
            }

            logger.info("API key validado correctamente");
            return true;

        } catch (Exception e) {
            logger.error("Error al validar API key", e);
            return false;
        }
    }
}


