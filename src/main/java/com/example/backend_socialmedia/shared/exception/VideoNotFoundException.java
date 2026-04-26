package com.example.backend_socialmedia.shared.exception;

public class VideoNotFoundException extends RuntimeException {
    public VideoNotFoundException(Long videoId) {
        super("Video no encontrado con ID: " + videoId);
    }
}
