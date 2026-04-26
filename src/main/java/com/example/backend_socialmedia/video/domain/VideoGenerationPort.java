package com.example.backend_socialmedia.video.domain;

public interface VideoGenerationPort {

    record GenerationRequest(String prompt, int durationSeconds, String aspectRatio) {}

    record GenerationResult(String jobId) {}

    record JobStatusResult(String jobId, String status, String videoUrl, String errorMessage) {}

    GenerationResult startGeneration(GenerationRequest request);

    JobStatusResult getJobStatus(String jobId);

    boolean isConfigured();
}
