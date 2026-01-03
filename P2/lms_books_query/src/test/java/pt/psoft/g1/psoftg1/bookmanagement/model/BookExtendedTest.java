package pt.psoft.g1.psoftg1.bookmanagement.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookExtendedTest {

    private Author author;
    private Genre genre;

    @BeforeEach
    void setUp() {
        author = new Author("AuthorName", "Author Bio", null);
        genre = new Genre("SciFi");
    }

    @Test
    void ensureBookWithMinimalValidData() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        
        final var book = new Book("9780451457998", "A", null, genre, authors, null);
        
        assertNotNull(book);
        assertEquals("9780451457998", book.getIsbn());
    }

    @Test
    void ensureBookWithLongTitle() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        final var longTitle = "A".repeat(128);
        
        final var book = new Book("9780451457998", longTitle, "Description", genre, authors, null);
        
        assertEquals(longTitle, book.getTitle().toString());
    }

    @Test
    void ensureBookWithLongDescription() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        final var longDesc = "A".repeat(4096);
        
        final var book = new Book("9780451457998", "Title", longDesc, genre, authors, null);
        
        assertEquals(longDesc, book.getDescription());
    }

    @Test
    void ensureBookWithSpecialCharactersInTitle() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        
        final var book = new Book("9780451457998", "Book & Author's Journey: A Story", "Description", genre, authors, null);
        
        assertEquals("Book & Author's Journey: A Story", book.getTitle().toString());
    }

    @Test
    void ensureBookWithMultipleAuthorsCanBeCreated() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        final var author2 = new Author("Second Author", "Bio 2", null);
        authors.add(author2);
        final var author3 = new Author("Third Author", "Bio 3", null);
        authors.add(author3);
        
        final var book = new Book("9780451457998", "Collaboration", "Three authors worked", genre, authors, null);
        
        assertEquals(3, book.getAuthors().size());
    }

    @Test
    void ensureBookCanBeIdentifiedByIsbn() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        final var isbn = "9780451457998";
        
        final var book = new Book(isbn, "Title", "Description", genre, authors, null);
        
        assertEquals(isbn, book.getIsbn());
    }

    @Test
    void ensureBookTitleCanContainNumbers() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        
        final var book = new Book("9780451457998", "2001: A Space Odyssey", "Description", genre, authors, null);
        
        assertEquals("2001: A Space Odyssey", book.getTitle().toString());
    }

    @Test
    void ensureBookDescriptionIsOptional() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        
        final var book = new Book("9780451457998", "Title", null, genre, authors, null);
        
        // Just verify the book exists
        assertNotNull(book);
    }

    @Test
    void ensureBookPhotoCanBeSet() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        final var photoURI = "https://example.com/book-cover.jpg";
        
        final var book = new Book("9780451457998", "Title", "Description", genre, authors, photoURI);
        
        assertNotNull(book);
    }

    @Test
    void ensureBookGenreCanBeDifferentGenres() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        
        final var fantasyGenre = new Genre("Fantasy");
        final var book = new Book("9780451457998", "Title", "Description", fantasyGenre, authors, null);
        
        assertEquals("Fantasy", book.getGenre().toString());
    }

    @Test
    void ensureBooksWithDifferentIsbnAreDifferent() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        
        final var book1 = new Book("9780451457998", "Title", "Description", genre, authors, null);
        final var book2 = new Book("0306406152", "Title", "Description", genre, authors, null);
        
        assertNotEquals(book1.getIsbn(), book2.getIsbn());
    }

    @Test
    void ensureEmptyAuthorListThrows() {
        List<Author> emptyAuthors = new ArrayList<>();
        
        assertThrows(IllegalArgumentException.class, () -> 
            new Book("9780451457998", "Title", "Description", genre, emptyAuthors, null)
        );
    }

    @Test
    void ensureBookCanHaveUnicodeInTitle() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        
        final var book = new Book("9780451457998", "Café: A Story", "Description", genre, authors, null);
        
        assertEquals("Café: A Story", book.getTitle().toString());
    }

    @Test
    void ensureBookCanHaveUnicodeInDescription() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        
        final var book = new Book("9780451457998", "Title", "Naïve résumé café", genre, authors, null);
        
        assertEquals("Naïve résumé café", book.getDescription());
    }

    @Test
    void ensureBookTitlePreservesCasing() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        
        final var book = new Book("9780451457998", "ThE tItLe CaSe", "Description", genre, authors, null);
        
        assertEquals("ThE tItLe CaSe", book.getTitle().toString());
    }

    @Test
    void ensureInvalidIsbnThrows() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        
        assertThrows(IllegalArgumentException.class, () -> 
            new Book("invalid-isbn", "Title", "Description", genre, authors, null)
        );
    }

    @Test
    void ensureBlankTitleThrows() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        
        assertThrows(IllegalArgumentException.class, () -> 
            new Book("9780451457998", "   ", "Description", genre, authors, null)
        );
    }

    @Test
    void ensureOversizeTitleThrows() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        final var longTitle = "A".repeat(129);
        
        assertThrows(IllegalArgumentException.class, () -> 
            new Book("9780451457998", longTitle, "Description", genre, authors, null)
        );
    }

    @Test
    void ensureOversizeDescriptionThrows() {
        List<Author> authors = new ArrayList<>();
        authors.add(author);
        final var longDesc = "A".repeat(4097);
        
        assertThrows(IllegalArgumentException.class, () -> 
            new Book("9780451457998", "Title", longDesc, genre, authors, null)
        );
    }
}

