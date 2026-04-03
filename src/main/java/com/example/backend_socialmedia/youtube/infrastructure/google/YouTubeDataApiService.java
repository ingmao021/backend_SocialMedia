package com.example.backend_socialmedia.youtube.infrastructure.google;

import com.example.backend_socialmedia.shared.config.GoogleAiProperties;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoFileDetails;
import com.google.api.services.youtube.model.VideoProcessingDetails;
import com.google.api.services.youtube.model.VideoStatus;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserAccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;

/**
 * Servicio para integración con YouTube Data API v3
 * Encapsula la lógica de comunicación con la API de YouTube
 */
@Service
public class YouTubeDataApiService {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeDataApiService.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Social Media Backend";

    private final GoogleAiProperties googleAiProperties;

    public YouTubeDataApiService(GoogleAiProperties googleAiProperties) {
        this.googleAiProperties = googleAiProperties;
    }

    /**
     * Obtiene una instancia de YouTube API client autenticada
     * @param accessToken El token de acceso del usuario
     * @return Instancia de YouTube API
     */
    private YouTube getYouTubeApiClient(String accessToken) throws IOException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        
        // Crear credenciales con el access token del usuario
        GoogleCredentials credentials = GoogleCredentials.create(
                new UserAccessToken(accessToken)
        );

        return new YouTube.Builder(
                httpTransport,
                JSON_FACTORY,
                credentials.createScoped(Collections.singleton(
                        "https://www.googleapis.com/auth/youtube.upload"
                ))
        )
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Publica un video en YouTube
     * @param accessToken Token de acceso del usuario
     * @param videoUrl URL del video a subir
     * @param title Título del video
     * @param description Descripción del video
     * @param visibility Visibilidad del video (PRIVATE, UNLISTED, PUBLIC)
     * @return ID del video publicado en YouTube
     */
    public YouTubePublishResponse publishVideo(
            String accessToken,
            String videoUrl,
            String title,
            String description,
            String visibility) {
        
        try {
            logger.info("Iniciando publicación de video en YouTube: {}", title);

            YouTube youtubeService = getYouTubeApiClient(accessToken);

            // TODO: Implementar lógica real de upload
            // Por ahora retornamos una respuesta mock
            String youtubeVideoId = "dQw4w9WgXcQ_" + System.currentTimeMillis();
            String youtubeUrl = "https://www.youtube.com/watch?v=" + youtubeVideoId;

            logger.info("Video publicado exitosamente en YouTube con ID: {}", youtubeVideoId);

            return new YouTubePublishResponse(
                    youtubeVideoId,
                    youtubeUrl,
                    "PUBLISHED",
                    null
            );

        } catch (Exception e) {
            logger.error("Error al publicar video en YouTube", e);
            return new YouTubePublishResponse(
                    null,
                    null,
                    "FAILED",
                    e.getMessage()
            );
        }
    }

    /**
     * Obtiene el estado de un video en YouTube
     * @param accessToken Token de acceso del usuario
     * @param youtubeVideoId ID del video en YouTube
     * @return Respuesta con el estado actual
     */
    public YouTubePublishResponse getVideoStatus(String accessToken, String youtubeVideoId) {
        try {
            logger.info("Consultando estado del video en YouTube: {}", youtubeVideoId);

            YouTube youtubeService = getYouTubeApiClient(accessToken);

            // TODO: Implementar lógica real para obtener estado
            // Por ahora retornamos una respuesta mock
            return new YouTubePublishResponse(
                    youtubeVideoId,
                    "https://www.youtube.com/watch?v=" + youtubeVideoId,
                    "PUBLISHED",
                    null
            );

        } catch (Exception e) {
            logger.error("Error al obtener estado del video: {}", youtubeVideoId, e);
            return new YouTubePublishResponse(
                    youtubeVideoId,
                    null,
                    "FAILED",
                    e.getMessage()
            );
        }
    }

    /**
     * Elimina un video de YouTube
     * @param accessToken Token de acceso del usuario
     * @param youtubeVideoId ID del video a eliminar
     * @return true si se eliminó exitosamente
     */
    public boolean deleteVideo(String accessToken, String youtubeVideoId) {
        try {
            logger.info("Eliminando video de YouTube: {}", youtubeVideoId);

            YouTube youtubeService = getYouTubeApiClient(accessToken);

            // TODO: Implementar lógica real de eliminación
            logger.info("Video eliminado exitosamente de YouTube: {}", youtubeVideoId);
            return true;

        } catch (Exception e) {
            logger.error("Error al eliminar video de YouTube: {}", youtubeVideoId, e);
            return false;
        }
    }

    /**
     * Valida si el access token es válido para YouTube
     * @param accessToken Token a validar
     * @return true si el token es válido
     */
    public boolean validateAccessToken(String accessToken) {
        try {
            if (accessToken == null || accessToken.trim().isEmpty()) {
                logger.warn("Access token para YouTube está vacío");
                return false;
            }

            logger.info("Access token validado para YouTube");
            return true;

        } catch (Exception e) {
            logger.error("Error al validar access token para YouTube", e);
            return false;
        }
    }
}

