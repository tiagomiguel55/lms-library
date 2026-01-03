package pt.psoft.g1.psoftg1.smokeTests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.config.TestRabbitMockConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test to verify MongoDB connectivity.
 * Ensures that the application can connect to the database successfully.
 */
@SpringBootTest(
    classes = pt.psoft.g1.psoftg1.LMSBooks.class
)
@ActiveProfiles("test")
@Import(TestRabbitMockConfig.class)
class DatabaseConnectivitySmokeTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void mongodb_connection_works() {
        // Simple query to verify database connectivity
        assertThat(mongoTemplate).isNotNull();
        assertThat(mongoTemplate.getDb()).isNotNull();
    }

    @Test
    void can_query_books_collection() {
        // Verify the main collections exist and are accessible
        long count = mongoTemplate.getCollection("books").countDocuments();
        assertThat(count).isNotNull();
    }

    @Test
    void can_query_authors_collection() {
        long count = mongoTemplate.getCollection("authors").countDocuments();
        assertThat(count).isNotNull();
    }

    @Test
    void can_query_genres_collection() {
        long count = mongoTemplate.getCollection("genres").countDocuments();
        assertThat(count).isNotNull();
    }
}
