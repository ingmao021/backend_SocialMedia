package com.socialvideo.external.vertex.dto;

import java.util.List;
import java.util.Map;

public record PredictLongRunningRequest(
        List<Map<String, String>> instances,
        Map<String, Object> parameters
) {}
