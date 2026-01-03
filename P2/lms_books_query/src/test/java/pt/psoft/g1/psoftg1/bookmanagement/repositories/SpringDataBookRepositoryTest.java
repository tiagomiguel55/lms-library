package pt.psoft.g1.psoftg1.bookmanagement.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringDataBookRepositoryTest {

    @Mock
    private BookRepository bookRepository;

    @Test
    void findByIsbnReturnsBook() {
        Author author = new Author(1L, "A", "Bio", null);
        Genre genre = new Genre("Sci-Fi");
        Book book = new Book("9780306406157", "Title", "Desc", genre, List.of(author), null);

        when(bookRepository.findByIsbn("9780306406157")).thenReturn(Optional.of(book));

        Optional<Book> found = bookRepository.findByIsbn("9780306406157");

        assertTrue(found.isPresent());
        assertEquals("9780306406157", found.get().getIsbn());
    }

    @Test
    void findByGenreReturnsBooksInGenre() {
        Author author = new Author(1L, "A", "Bio", null);
        Genre genre = new Genre("Sci-Fi");
        Book book = new Book("9781234567897", "Title", "Desc", genre, List.of(author), null);

        when(bookRepository.findByGenre("Sci-Fi")).thenReturn(List.of(book));

        List<Book> books = bookRepository.findByGenre("Sci-Fi");

        assertEquals(1, books.size());
        assertEquals("Sci-Fi", books.get(0).getGenre().toString());
    }
}
