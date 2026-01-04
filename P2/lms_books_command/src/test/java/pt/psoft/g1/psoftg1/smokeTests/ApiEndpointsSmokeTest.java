package pt.psoft.g1.psoftg1.smokeTests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke tests for API endpoints to verify basic connectivity and responses.
 * These tests ensure that the main API endpoints are accessible.
 */
@SpringBootTest(
    classes = pt.psoft.g1.psoftg1.LMSBooks.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRabbitMockConfig.class)
class ApiEndpointsSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void swagger_ui_accessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void api_docs_accessible() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void books_endpoint_accessible() throws Exception {
        // Just verify the endpoint responds (not 5xx server error)
        mockMvc.perform(get("/api/books"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status >= 500) {
                        throw new AssertionError("Expected status < 500 but was: " + status);
                    }
                });
    }

    @Test
    void authors_endpoint_accessible() throws Exception {
        // Just verify the endpoint responds (not 5xx server error)
        mockMvc.perform(get("/api/authors"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status >= 500) {
                        throw new AssertionError("Expected status < 500 but was: " + status);
                    }
                });
    }

    @Test
    void genres_endpoint_accessible() throws Exception {
        // Just verify the endpoint responds (not 5xx server error)
        mockMvc.perform(get("/api/genre"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status >= 500) {
                        throw new AssertionError("Expected status < 500 but was: " + status);
                    }
                });
    }
}
