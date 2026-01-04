package pt.psoft.g1.psoftg1.unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended edge case tests for Book entity
 */
class BookEdgeCasesTest {
    private String validIsbn = "9782826012092";
    private String validTitle = "Test Book Title";
    private Author author1;
    private Author author2;
    private Author author3;
    private Genre genre1;
    private Genre genre2;
    private List<Author> authors;

    @BeforeEach
    void setUp() {
        author1 = new Author("Author One", "Bio for author one", null);
        author2 = new Author("Author Two", "Bio for author two", null);
        author3 = new Author("Author Three", "Bio for author three", null);
        genre1 = new Genre("Fiction");
        genre2 = new Genre("Mystery");
        authors = new ArrayList<>();
    }

    @Test
    void testBookWithSingleAuthor() {
        authors.add(author1);
        Book book = new Book(validIsbn, validTitle, null, genre1, authors, null);
        assertNotNull(book);
        assertEquals(1, book.getAuthors().size());
    }

    @Test
    void testBookWithMultipleAuthors() {
        authors.add(author1);
        authors.add(author2);
        authors.add(author3);
        Book book = new Book(validIsbn, validTitle, null, genre1, authors, null);
        assertNotNull(book);
        assertEquals(3, book.getAuthors().size());
        assertTrue(book.getAuthors().contains(author1));
        assertTrue(book.getAuthors().contains(author2));
        assertTrue(book.getAuthors().contains(author3));
    }

    @Test
    void testBookAuthorsList() {
        authors.add(author1);
        authors.add(author2);
        Book book = new Book(validIsbn, validTitle, null, genre1, authors, null);
        assertNotNull(book.getAuthors());
        assertEquals(2, book.getAuthors().size());
    }

    @Test
    void testBookGenreAssignment() {
        authors.add(author1);
        Book book = new Book(validIsbn, validTitle, null, genre1, authors, null);
        assertEquals(genre1, book.getGenre());
    }

    @Test
    void testBookTitleAssignment() {
        authors.add(author1);
        Book book = new Book(validIsbn, validTitle, null, genre1, authors, null);
        assertEquals(validTitle, book.getTitle().toString());
    }

    @Test
    void testBookIsbnAssignment() {
        authors.add(author1);
        Book book = new Book(validIsbn, validTitle, null, genre1, authors, null);
        assertEquals(validIsbn, book.getIsbn().toString());
    }

    @Test
    void testBookWithDescription() {
        authors.add(author1);
        String description = "This is a book description";
        Book book = new Book(validIsbn, validTitle, description, genre1, authors, null);
        assertEquals(description, book.getDescription().toString());
    }

    @Test
    void testBookWithoutDescription() {
        authors.add(author1);
        Book book = new Book(validIsbn, validTitle, null, genre1, authors, null);
        // Description is null when not provided
        assertDoesNotThrow(() -> book.getTitle());
    }

    @Test
    void testBookEmptyAuthorsListFails() {
        // Empty authors list should fail
        assertThrows(IllegalArgumentException.class,
                () -> new Book(validIsbn, validTitle, null, genre1, authors, null));
    }

    @Test
    void testBookCreationWithMinimalRequiredFields() {
        authors.add(author1);
        assertDoesNotThrow(() -> new Book(validIsbn, validTitle, null, genre1, authors, null));
    }

    @Test
    void testBookWithValidIsbn13() {
        authors.add(author1);
        String isbn13 = "9780544003415";
        Book book = new Book(isbn13, validTitle, null, genre1, authors, null);
        assertEquals(isbn13, book.getIsbn().toString());
    }

    @Test
    void testBookWithValidIsbn10() {
        authors.add(author1);
        String isbn10 = "8175257660";
        Book book = new Book(isbn10, validTitle, null, genre1, authors, null);
        assertEquals(isbn10, book.getIsbn().toString());
    }

    @Test
    void testBookDifferentGenres() {
        authors.add(author1);
        Book book1 = new Book(validIsbn, validTitle, null, genre1, authors, null);
        
        authors.clear();
        authors.add(author2);
        Book book2 = new Book("9780307476357", "Another Book", null, genre2, authors, null);
        
        assertEquals(genre1, book1.getGenre());
        assertEquals(genre2, book2.getGenre());
        assertNotEquals(book1.getGenre(), book2.getGenre());
    }

    @Test
    void testBookWithLongTitle() {
        authors.add(author1);
        String longTitle = "The Exceptionally Long and Detailed Title of a Book " +
                          "That Contains Many Words and Describes the Content";
        Book book = new Book(validIsbn, longTitle, null, genre1, authors, null);
        assertEquals(longTitle, book.getTitle().toString());
    }

    @Test
    void testBookAuthorOrder() {
        authors.add(author1);
        authors.add(author2);
        authors.add(author3);
        Book book = new Book(validIsbn, validTitle, null, genre1, authors, null);
        
        List<Author> bookAuthors = book.getAuthors();
        assertEquals(author1, bookAuthors.get(0));
        assertEquals(author2, bookAuthors.get(1));
        assertEquals(author3, bookAuthors.get(2));
    }

    @Test
    void testBookWithSpecialCharactersInTitle() {
        authors.add(author1);
        String specialTitle = "Book & Co.: The Story (2024)";
        Book book = new Book(validIsbn, specialTitle, null, genre1, authors, null);
        assertEquals(specialTitle, book.getTitle().toString());
    }

    @Test
    void testBookWithDifferentIsbns() {
        authors.add(author1);
        Book book1 = new Book("9782826012092", validTitle, null, genre1, authors, null);
        
        authors.clear();
        authors.add(author2);
        Book book2 = new Book("9780307476357", "Different Book", null, genre1, authors, null);
        
        assertNotEquals(book1.getIsbn(), book2.getIsbn());
    }
}
