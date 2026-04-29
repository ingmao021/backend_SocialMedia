package com.socialvideo.user.service;

import com.socialvideo.config.AppProperties;
import com.socialvideo.exception.InvalidFileTypeException;
import com.socialvideo.exception.ResourceNotFoundException;
import com.socialvideo.external.gcs.GcsService;
import com.socialvideo.user.dto.UpdateProfileRequest;
import com.socialvideo.user.dto.UserResponse;
import com.socialvideo.user.entity.User;
import com.socialvideo.user.repository.UserRepository;
import com.socialvideo.video.entity.VideoStatus;
import com.socialvideo.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg");

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final GcsService gcsService;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = findUserOrThrow(userId);
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);
        user.setName(request.name());
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse uploadAvatar(Long userId, MultipartFile file) {
        // Validate file type — Spring already rejects >2MB via MaxUploadSizeExceededException
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException("Tipo de archivo no permitido. Solo PNG o JPEG.");
        }

        User user = findUserOrThrow(userId);

        try {
            String extension = contentType.equals("image/png") ? "png" : "jpg";
            String gcsUri = gcsService.uploadAvatar(userId, file.getBytes(), contentType, extension);
            user.setAvatarUrl(gcsUri); // Store gs:// URI, resolve on read
            userRepository.save(user);
            return toUserResponse(user);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo archivo de avatar", e);
        }
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    /**
     * Maps User entity to UserResponse DTO.
     * Resolves gs:// avatar URIs to signed URLs, passes through https:// URLs.
     * Public so AuthService can reuse it.
     */
    public UserResponse toUserResponse(User user) {
        int videosGenerated = videoRepository.countByUserIdAndStatus(
                user.getId(), VideoStatus.COMPLETED);
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                resolveAvatarUrl(user.getAvatarUrl()),
                user.getPasswordHash() != null,
                user.getGoogleId() != null,
                videosGenerated,
                appProperties.getVideo().getMaxCompleted(),
                user.getCreatedAt()
        );
    }

    /**
     * Resolves avatar URL:
     * - gs:// URIs → generates a signed URL (30-day TTL)
     * - https:// URLs (Google profile pictures) → returned as-is
     * - null → null
     */
    private String resolveAvatarUrl(String avatarUrl) {
        if (avatarUrl == null) {
            return null;
        }
        if (avatarUrl.startsWith("gs://")) {
            try {
                return gcsService.generateAvatarSignedUrl(avatarUrl);
            } catch (Exception e) {
                log.error("Error generating signed URL for avatar: {}", avatarUrl, e);
                return null;
            }
        }
        // Google profile picture URL or other https:// URL — return as-is
        return avatarUrl;
    }
}
