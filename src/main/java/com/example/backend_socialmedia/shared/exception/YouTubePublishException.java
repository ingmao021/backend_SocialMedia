package com.example.backend_socialmedia.shared.exception;

public class YouTubePublishException extends RuntimeException {
    public YouTubePublishException(String message) {
        super(message);
    }

    public YouTubePublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
