package com.example.backend_socialmedia.youtube.infrastructure.google;

import com.example.backend_socialmedia.shared.config.VertexAiProperties;
import com.example.backend_socialmedia.shared.exception.YouTubePublishException;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class YouTubeDataApiService {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeDataApiService.class);

    private static final String YOUTUBE_UPLOAD_INITIATE_URL =
            "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status";
    private static final String YOUTUBE_VIDEOS_URL =
            "https://www.googleapis.com/youtube/v3/videos";
    private static final String GOOGLE_TOKENINFO_URL =
            "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=";
    private static final String GCS_DOWNLOAD_BASE =
            "https://storage.googleapis.com/download/storage/v1/b/%s/o/%s?alt=media";

    private final VertexAiProperties vertexProps;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private GoogleCredentials serviceAccountCredentials;

    public YouTubeDataApiService(VertexAiProperties vertexProps, ObjectMapper objectMapper) {
        this.vertexProps = vertexProps;
        this.objectMapper = objectMapper;
        this.restTemplate = buildRestTemplate();
    }

    @PostConstruct
    public void init() {
        try {
            this.serviceAccountCredentials = buildServiceAccountCredentials();
            logger.info("Credenciales de service account inicializadas para YouTubeDataApiService");
        } catch (IOException e) {
            logger.error("Error al inicializar credenciales de service account en YouTubeDataApiService", e);
            throw new RuntimeException("No se pudieron inicializar credenciales para YouTube: " + e.getMessage(), e);
        }
    }

    public YouTubePublishResponse publishVideo(String userAccessToken, String videoUrl,
                                               String title, String description,
                                               String visibility) {
        try {
            logger.info("Iniciando publicación en YouTube: title='{}', visibility={}", title, visibility);

            byte[] videoBytes = downloadVideo(videoUrl);
            logger.info("Video descargado: {} bytes", videoBytes.length);

            String uploadUrl = initiateResumableUpload(userAccessToken, title, description, mapVisibility(visibility), videoBytes.length);
            logger.info("Upload resumable iniciado. URL obtenida de YouTube.");

            String youtubeVideoId = uploadVideoBytes(uploadUrl, videoBytes);
            String youtubeUrl = "https://www.youtube.com/watch?v=" + youtubeVideoId;

            logger.info("Video publicado exitosamente en YouTube. ID={}", youtubeVideoId);
            return new YouTubePublishResponse(youtubeVideoId, youtubeUrl, "PUBLISHED", null);

        } catch (YouTubePublishException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al publicar video en YouTube", e);
            return new YouTubePublishResponse(null, null, "FAILED", e.getMessage());
        }
    }

    public YouTubePublishResponse getVideoStatus(String accessToken, String youtubeVideoId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<Map> response = restTemplate.exchange(
                    YOUTUBE_VIDEOS_URL + "?id=" + youtubeVideoId + "&part=status,snippet",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map<?, ?> body = response.getBody();
            if (body != null) {
                List<?> items = (List<?>) body.get("items");
                if (items != null && !items.isEmpty()) {
                    Map<?, ?> item = (Map<?, ?>) items.get(0);
                    Map<?, ?> status = (Map<?, ?>) item.get("status");
                    String uploadStatus = status != null ? (String) status.get("uploadStatus") : "unknown";
                    return new YouTubePublishResponse(
                            youtubeVideoId,
                            "https://www.youtube.com/watch?v=" + youtubeVideoId,
                            mapYouTubeUploadStatus(uploadStatus),
                            null
                    );
                }
            }
            logger.warn("Video {} no encontrado en YouTube", youtubeVideoId);
            return new YouTubePublishResponse(youtubeVideoId, null, "UNKNOWN", "Video no encontrado en YouTube");

        } catch (Exception e) {
            logger.error("Error al obtener estado del video {}: {}", youtubeVideoId, e.getMessage(), e);
            return new YouTubePublishResponse(youtubeVideoId, null, "FAILED", e.getMessage());
        }
    }

    public boolean deleteVideo(String accessToken, String youtubeVideoId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            restTemplate.exchange(
                    YOUTUBE_VIDEOS_URL + "?id=" + youtubeVideoId,
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    Void.class
            );

            logger.info("Video {} eliminado de YouTube", youtubeVideoId);
            return true;

        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Video {} no encontrado en YouTube para eliminar", youtubeVideoId);
            return true;
        } catch (Exception e) {
            logger.error("Error al eliminar video {} de YouTube: {}", youtubeVideoId, e.getMessage(), e);
            return false;
        }
    }

    public boolean validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            logger.warn("Access token está vacío");
            return false;
        }
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    GOOGLE_TOKENINFO_URL + accessToken, Map.class
            );
            boolean valid = response.getStatusCode().is2xxSuccessful();
            if (!valid) {
                logger.warn("Token OAuth inválido según Google tokeninfo");
            }
            return valid;
        } catch (Exception e) {
            logger.warn("Token OAuth no válido: {}", e.getMessage());
            return false;
        }
    }

    private String initiateResumableUpload(String userAccessToken, String title,
                                           String description, String privacyStatus,
                                           long contentLength) {
        String metadataJson = buildVideoMetadataJson(title, description, privacyStatus);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userAccessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Upload-Content-Type", "video/mp4");
        headers.setContentLength(metadataJson.getBytes(StandardCharsets.UTF_8).length);

        ResponseEntity<String> initResponse = restTemplate.exchange(
                YOUTUBE_UPLOAD_INITIATE_URL,
                HttpMethod.POST,
                new HttpEntity<>(metadataJson, headers),
                String.class
        );

        String uploadUrl = initResponse.getHeaders().getFirst("Location");
        if (uploadUrl == null || uploadUrl.isBlank()) {
            throw new YouTubePublishException("YouTube no devolvió URL de upload resumable en el header Location");
        }
        return uploadUrl;
    }

    private String uploadVideoBytes(String uploadUrl, byte[] videoBytes) {
        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.parseMediaType("video/mp4"));
        uploadHeaders.setContentLength(videoBytes.length);

        ResponseEntity<Map> uploadResponse = restTemplate.exchange(
                uploadUrl,
                HttpMethod.PUT,
                new HttpEntity<>(videoBytes, uploadHeaders),
                Map.class
        );

        Map<?, ?> uploadBody = uploadResponse.getBody();
        if (uploadBody == null || !uploadBody.containsKey("id")) {
            throw new YouTubePublishException("YouTube no devolvió ID del video tras el upload. Respuesta: " + uploadBody);
        }
        return (String) uploadBody.get("id");
    }

    private byte[] downloadVideo(String videoUrl) throws Exception {
        String downloadUrl = convertToHttpUrl(videoUrl);
        String accessToken = getServiceAccountAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                downloadUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                byte[].class
        );

        if (response.getBody() == null) {
            throw new YouTubePublishException("No se pudo descargar el video desde: " + videoUrl);
        }
        return response.getBody();
    }

    private String convertToHttpUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) {
            throw new YouTubePublishException("URL del video es nula o vacía");
        }
        if (videoUrl.startsWith("gs://")) {
            // gs://bucket-name/path/to/video.mp4 → GCS REST API URL
            String withoutScheme = videoUrl.substring(5);
            int slashIndex = withoutScheme.indexOf('/');
            if (slashIndex < 0) {
                throw new YouTubePublishException("URL GCS inválida (sin path): " + videoUrl);
            }
            String bucket = withoutScheme.substring(0, slashIndex);
            String objectPath = withoutScheme.substring(slashIndex + 1);
            String encodedObject = URLEncoder.encode(objectPath, StandardCharsets.UTF_8);
            return GCS_DOWNLOAD_BASE.formatted(bucket, encodedObject);
        }
        if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
            return videoUrl;
        }
        throw new YouTubePublishException("Formato de URL no soportado: " + videoUrl);
    }

    private String buildVideoMetadataJson(String title, String description, String privacyStatus) {
        try {
            ObjectNode snippet = objectMapper.createObjectNode();
            snippet.put("title", title != null ? title : "");
            snippet.put("description", description != null ? description : "");
            snippet.put("categoryId", "22");

            ObjectNode status = objectMapper.createObjectNode();
            status.put("privacyStatus", privacyStatus);
            status.put("selfDeclaredMadeForKids", false);

            ObjectNode root = objectMapper.createObjectNode();
            root.set("snippet", snippet);
            root.set("status", status);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new YouTubePublishException("Error al construir metadata del video: " + e.getMessage());
        }
    }

    private String mapVisibility(String visibility) {
        if (visibility == null) return "private";
        return switch (visibility.toUpperCase()) {
            case "PUBLIC" -> "public";
            case "UNLISTED" -> "unlisted";
            default -> "private";
        };
    }

    private String mapYouTubeUploadStatus(String youtubeStatus) {
        if (youtubeStatus == null) return "UNKNOWN";
        return switch (youtubeStatus) {
            case "processed" -> "PUBLISHED";
            case "uploaded" -> "PUBLISHING";
            case "failed", "rejected", "deleted" -> "FAILED";
            default -> "PUBLISHING";
        };
    }

    private String getServiceAccountAccessToken() throws Exception {
        serviceAccountCredentials.refreshIfExpired();
        return serviceAccountCredentials.getAccessToken().getTokenValue();
    }

    private GoogleCredentials buildServiceAccountCredentials() throws IOException {
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
                .createScoped(List.of(
                        "https://www.googleapis.com/auth/cloud-platform",
                        "https://www.googleapis.com/auth/devstorage.read_only"
                ));
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30_000);
        factory.setReadTimeout(300_000);
        return new RestTemplate(factory);
    }
}
