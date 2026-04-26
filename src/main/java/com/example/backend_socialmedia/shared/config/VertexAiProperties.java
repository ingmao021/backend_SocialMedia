package com.example.backend_socialmedia.shared.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "vertex-ai")
@Validated
public class VertexAiProperties {

    @NotBlank(message = "vertex-ai.project-id es obligatorio. Configura GOOGLE_CLOUD_PROJECT_ID")
    private String projectId;

    @NotBlank(message = "vertex-ai.client-email es obligatorio. Configura GOOGLE_CLOUD_CLIENT_EMAIL")
    private String clientEmail;

    @NotBlank(message = "vertex-ai.private-key es obligatorio. Configura GOOGLE_CLOUD_PRIVATE_KEY")
    private String privateKey;

    private String privateKeyId;

    private String clientId;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

    public String getPrivateKeyId() { return privateKeyId; }
    public void setPrivateKeyId(String privateKeyId) { this.privateKeyId = privateKeyId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
}
