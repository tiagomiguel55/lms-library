package pt.psoft.g1.psoftg1.smokeTests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test to verify that the Spring application context loads successfully.
 * This is a basic sanity check to ensure the application can start.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRabbitMockConfig.class)
class ApplicationContextSmokeTest {

    @Test
    void contextLoads() {
        // This test will fail if the application context cannot be loaded
        assertThat(true).isTrue();
    }
}
