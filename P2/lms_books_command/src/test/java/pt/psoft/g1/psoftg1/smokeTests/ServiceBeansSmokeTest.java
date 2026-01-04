package pt.psoft.g1.psoftg1.smokeTests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test to verify that main service beans are properly initialized.
 * Ensures dependency injection is working correctly for core services.
 */
@SpringBootTest(
    classes = pt.psoft.g1.psoftg1.LMSBooks.class
)
@ActiveProfiles("test")
@Import(TestRabbitMockConfig.class)
class ServiceBeansSmokeTest {

    @Autowired(required = false)
    private BookService bookService;

    @Autowired(required = false)
    private AuthorService authorService;

    @Autowired(required = false)
    private GenreService genreService;

    @Test
    void bookService_bean_exists() {
        assertThat(bookService).isNotNull();
    }

    @Test
    void authorService_bean_exists() {
        assertThat(authorService).isNotNull();
    }

    @Test
    void genreService_bean_exists() {
        assertThat(genreService).isNotNull();
    }
}
