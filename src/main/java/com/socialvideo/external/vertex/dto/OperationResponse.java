package com.socialvideo.external.vertex.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OperationResponse(              
        String name,
        Boolean done,
        JsonNode response,
        OperationError error
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OperationError(
            int code,
            String message
    ) {}
}
