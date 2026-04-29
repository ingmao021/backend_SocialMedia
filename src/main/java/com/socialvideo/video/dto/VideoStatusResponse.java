package com.socialvideo.video.dto;

import com.socialvideo.video.entity.VideoStatus;

public record VideoStatusResponse(
        VideoStatus status,
        String signedUrl
) {}
