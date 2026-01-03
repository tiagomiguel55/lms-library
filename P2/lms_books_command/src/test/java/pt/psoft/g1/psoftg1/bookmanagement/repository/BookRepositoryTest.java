package pt.psoft.g1.psoftg1.bookmanagement.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;
import pt.psoft.g1.psoftg1.shared.services.Page;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BookRepositoryTest {

    private BookRepository repository;

    private Book dramaAlice;
    private Book thrillerBob;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookRepository();
        dramaAlice = makeBook("9780134685991", "Alice Adventures", "Drama", "Alice", 1L);
        thrillerBob = makeBook("9780596007126", "Bob Mystery", "Thriller", "Bob", 2L);
        repository.save(dramaAlice);
        repository.save(thrillerBob);
    }

    @Test
    void saveAndFindByIsbnRoundTrip() {
        Optional<Book> found = repository.findByIsbn("9780134685991");
        assertTrue(found.isPresent());
        assertEquals("Alice Adventures", found.get().getTitle().toString());
    }

    @Test
    void findByGenreAndTitleAndAuthorName() {
        assertEquals(1, repository.findByGenre("Drama").size());
        assertEquals(1, repository.findByTitle("Bob Mystery").size());
        assertEquals(1, repository.findByAuthorName("Ali").size());
    }

    @Test
    void searchBooksFiltersByQueryFields() {
        List<Book> results = repository.searchBooks(new Page(1, 10), new SearchBooksQuery("Alice", "Drama", "Alice"));
        assertEquals(1, results.size());
        assertEquals("9780134685991", results.get(0).getIsbn());
    }

    @Test
    void findBooksByAuthorNumberMatchesExact() {
        assertEquals(1, repository.findBooksByAuthorNumber(2L).size());
        assertTrue(repository.findBooksByAuthorNumber(99L).isEmpty());
    }

    @Test
    void deleteRemovesBook() {
        repository.delete(dramaAlice);
        assertTrue(repository.findByIsbn("9780134685991").isEmpty());
    }

    private Book makeBook(String isbn, String title, String genreName, String authorName, long authorNumber) {
        Genre genre = new Genre(genreName);
        Author author = new Author(authorName, "Bio", null);
        ReflectionTestUtils.setField(author, "authorNumber", authorNumber);
        return new Book(isbn, title, "Desc", genre, List.of(author), null);
    }

    private static final class InMemoryBookRepository implements BookRepository {
        private final Map<String, Book> store = new HashMap<>();

        @Override
        public List<Book> findByGenre(String genre) {
            return store.values().stream()
                    .filter(b -> b.getGenre().toString().equalsIgnoreCase(genre))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Book> findByTitle(String title) {
            return store.values().stream()
                    .filter(b -> b.getTitle().toString().equalsIgnoreCase(title))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Book> findByAuthorName(String authorName) {
            return store.values().stream()
                    .filter(b -> b.getAuthors().stream().anyMatch(a -> a.getName().toLowerCase().contains(authorName.toLowerCase())))
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<Book> findByIsbn(String isbn) {
            return Optional.ofNullable(store.get(isbn));
        }

        @Override
        public List<Book> findBooksByAuthorNumber(Long authorNumber) {
            return store.values().stream()
                    .filter(b -> b.getAuthors().stream().anyMatch(a -> Objects.equals(a.getAuthorNumber(), authorNumber)))
                    .collect(Collectors.toList());
        }

        @Override
        public List<Book> searchBooks(Page page, SearchBooksQuery query) {
            return store.values().stream()
                    .filter(b -> query.getTitle() == null || b.getTitle().toString().toLowerCase().contains(query.getTitle().toLowerCase()))
                    .filter(b -> query.getGenre() == null || b.getGenre().toString().equalsIgnoreCase(query.getGenre()))
                    .filter(b -> query.getAuthorName() == null || b.getAuthors().stream().anyMatch(a -> a.getName().toLowerCase().contains(query.getAuthorName().toLowerCase())))
                    .collect(Collectors.toList());
        }

        @Override
        public Book save(Book book) {
            store.put(book.getIsbn(), book);
            return book;
        }

        @Override
        public void delete(Book book) {
            store.remove(book.getIsbn());
        }
    }
}
