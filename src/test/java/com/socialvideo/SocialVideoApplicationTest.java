package com.socialvideo;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class SocialVideoApplicationTest {

    // Mock external GCP beans so context loads without real credentials
    @MockitoBean
    private GoogleCredentials googleCredentials;

    @MockitoBean
    private Storage storage;

    @Test
    void contextLoads() {
        // Smoke test: Spring context loads successfully
    }
}
