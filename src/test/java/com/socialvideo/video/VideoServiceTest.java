package com.socialvideo.video;

import com.socialvideo.config.AppProperties;
import com.socialvideo.exception.QuotaExceededException;
import com.socialvideo.external.gcs.GcsService;
import com.socialvideo.external.vertex.VertexAiClient;
import com.socialvideo.user.entity.User;
import com.socialvideo.user.repository.UserRepository;
import com.socialvideo.video.dto.GenerateVideoRequest;
import com.socialvideo.video.dto.VideoResponse;
import com.socialvideo.video.entity.Video;
import com.socialvideo.video.entity.VideoStatus;
import com.socialvideo.video.repository.VideoRepository;
import com.socialvideo.video.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock private VideoRepository videoRepository;
    @Mock private UserRepository userRepository;
    @Mock private VertexAiClient vertexAiClient;
    @Mock private GcsService gcsService;
    @Mock private AppProperties appProperties;

    @InjectMocks
    private VideoService videoService;

    private User testUser;
    private AppProperties.Video videoConfig;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        videoConfig = new AppProperties.Video();
        videoConfig.setMaxCompleted(2);
    }

    @Test
    void generate_withAvailableQuota_returnsProcessingVideo() {
        GenerateVideoRequest request = new GenerateVideoRequest("A cat playing piano", (short) 4);
        UUID videoId = UUID.randomUUID();

        when(appProperties.getVideo()).thenReturn(videoConfig);
        when(videoRepository.countByUserIdAndStatus(1L, VideoStatus.COMPLETED)).thenReturn(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(videoRepository.save(any(Video.class))).thenAnswer(invocation -> {
            Video v = invocation.getArgument(0);
            v.setId(videoId);
            v.setCreatedAt(Instant.now());
            v.setUpdatedAt(Instant.now());
            return v;
        });
        when(vertexAiClient.predictLongRunning(
                eq("A cat playing piano"), eq(4), eq(1L), eq(videoId)))
                .thenReturn("projects/fake/locations/us-central1/operations/op-123");

        VideoResponse response = videoService.generate(1L, request);

        assertThat(response.status()).isEqualTo(VideoStatus.PROCESSING);
        assertThat(response.prompt()).isEqualTo("A cat playing piano");
        assertThat(response.durationSeconds()).isEqualTo((short) 4);
        assertThat(response.id()).isEqualTo(videoId);
    }

    @Test
    void generate_withExceededQuota_throwsQuotaExceeded() {
        GenerateVideoRequest request = new GenerateVideoRequest("Some prompt", (short) 6);

        when(appProperties.getVideo()).thenReturn(videoConfig);
        when(videoRepository.countByUserIdAndStatus(1L, VideoStatus.COMPLETED)).thenReturn(2);

        assertThatThrownBy(() -> videoService.generate(1L, request))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("límite de 2 videos");
    }
}
