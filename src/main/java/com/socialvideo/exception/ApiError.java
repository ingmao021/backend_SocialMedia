package com.socialvideo.exception;

import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, String> fields
) {}
