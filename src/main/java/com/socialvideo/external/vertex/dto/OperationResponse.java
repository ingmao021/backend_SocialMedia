package com.socialvideo.external.vertex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OperationResponse(              
        String name,
        Boolean done,
        OperationResponseBody response,
        OperationError error
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OperationResponseBody(
            List<VideoResult> videos
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VideoResult(
            String gcsUri,
            String mimeType
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OperationError(
            int code,
            String message
    ) {}
}
