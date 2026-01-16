package com.course_learning.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ApiIntegrationTest {

    @Test
    void contextLoads() {
        // Basic test to ensure Spring context loads with all our new components
        // This verifies that all our new beans (CertificateService, EnrollmentAnalyticsService, etc.) are properly configured
    }
}
