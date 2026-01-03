package pt.psoft.g1.psoftg1.smokeTests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.config.TestRabbitMockConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test to verify database connectivity.
 * Ensures that the application can connect to the database successfully.
 */
@SpringBootTest(
    classes = pt.psoft.g1.psoftg1.LMSBooks.class
)
@ActiveProfiles("test")
@Import(TestRabbitMockConfig.class)
class DatabaseConnectivitySmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void database_connection_works() {
        // Simple query to verify database connectivity
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void can_query_book_table() {
        // Verify the main tables exist and are accessible
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book", Long.class);
        assertThat(count).isNotNull();
    }

    @Test
    void can_query_author_table() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM author", Long.class);
        assertThat(count).isNotNull();
    }

    @Test
    void can_query_genre_table() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM genre", Long.class);
        assertThat(count).isNotNull();
    }
}
