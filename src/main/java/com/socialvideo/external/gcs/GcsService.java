package com.socialvideo.external.gcs;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Cors;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.socialvideo.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GcsService {

    private final Storage storage;
    private final AppProperties appProperties;

    /**
     * Configures CORS on the GCS bucket so browsers can load videos
     * directly via Signed URLs from any frontend origin.
     * Uses wildcard origin ("*") because Signed URLs are already authenticated.
     */
    public void configureBucketCors() {
        String bucketName = appProperties.getGcp().getBucket();

        Cors cors = Cors.newBuilder()
                .setOrigins(List.of(Cors.Origin.of("*")))
                .setMethods(List.of(HttpMethod.GET, HttpMethod.HEAD))
                .setResponseHeaders(List.of(
                        "Content-Type", "Content-Length", "Content-Range",
                        "Accept-Ranges", "Range"
                ))
                .setMaxAgeSeconds(3600)
                .build();

        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            log.error("GCS bucket not found: {}", bucketName);
            return;
        }

        bucket.toBuilder().setCors(List.of(cors)).build().update();
        log.info("CORS configured on GCS bucket: {}", bucketName);
    }

    /**
     * Generates a Signed URL V4 for a GCS object (videos).
     * TTL: app.signed-url.ttl-days (default 7 days).
     *
     * @param gcsUri full gs:// URI (e.g. gs://bucket/path/file.mp4)
     * @return signed URL
     */
    public URL generateSignedUrl(String gcsUri) {
        String[] parts = parseGcsUri(gcsUri);
        return storage.signUrl(
                BlobInfo.newBuilder(parts[0], parts[1]).build(),
                appProperties.getSignedUrl().getTtlDays(), TimeUnit.DAYS,
                Storage.SignUrlOption.withV4Signature(),
                Storage.SignUrlOption.httpMethod(HttpMethod.GET)
        );
    }

    /**
     * Generates a Signed URL V4 for an avatar.
     * TTL: app.signed-url.avatar-ttl-days (default 30 days).
     *
     * @param gcsUri full gs:// URI (e.g. gs://bucket/avatars/123/avatar.jpg)
     * @return signed URL string
     */
    public String generateAvatarSignedUrl(String gcsUri) {
        String[] parts = parseGcsUri(gcsUri);
        URL url = storage.signUrl(
                BlobInfo.newBuilder(parts[0], parts[1]).build(),
                appProperties.getSignedUrl().getAvatarTtlDays(), TimeUnit.DAYS,
                Storage.SignUrlOption.withV4Signature(),
                Storage.SignUrlOption.httpMethod(HttpMethod.GET)
        );
        return url.toString();
    }

    /**
     * Uploads an avatar image to GCS.
     *
     * @return the gs:// URI of the uploaded file (NOT a signed URL)
     */
    public String uploadAvatar(Long userId, byte[] data, String contentType, String extension) {
        String bucket = appProperties.getGcp().getBucket();
        String objectName = "avatars/" + userId + "/avatar." + extension;

        BlobId blobId = BlobId.of(bucket, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        storage.create(blobInfo, data);

        return "gs://" + bucket + "/" + objectName;
    }

    private String[] parseGcsUri(String gcsUri) {
        if (!gcsUri.startsWith("gs://")) {
            throw new IllegalArgumentException("Invalid GCS URI: " + gcsUri);
        }
        String withoutPrefix = gcsUri.substring(5); // remove "gs://"
        int slashIndex = withoutPrefix.indexOf('/');
        if (slashIndex < 0) {
            throw new IllegalArgumentException("Invalid GCS URI (no object path): " + gcsUri);
        }
        String bucket = withoutPrefix.substring(0, slashIndex);
        String object = withoutPrefix.substring(slashIndex + 1);
        return new String[]{bucket, object};
    }
}
