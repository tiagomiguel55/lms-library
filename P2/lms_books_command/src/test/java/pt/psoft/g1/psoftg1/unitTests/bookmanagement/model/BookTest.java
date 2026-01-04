package pt.psoft.g1.psoftg1.unitTests.bookmanagement.model;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookTest {

    @Test
    void constructorRejectsNullIsbn() {
        Genre genre = new Genre("Drama");
        List<Author> authors = List.of(new Author("Alice", "Bio", null));
        assertThrows(IllegalArgumentException.class, () -> new Book(null, "Title", "Desc", genre, authors, null));
    }

    @Test
    void constructorRejectsNullTitle() {
        Genre genre = new Genre("Drama");
        List<Author> authors = List.of(new Author("Alice", "Bio", null));
        assertThrows(IllegalArgumentException.class, () -> new Book("9780134685991", null, "Desc", genre, authors, null));
    }

    @Test
    void constructorRejectsNullGenre() {
        List<Author> authors = List.of(new Author("Alice", "Bio", null));
        assertThrows(IllegalArgumentException.class, () -> new Book("9780134685991", "Title", "Desc", null, authors, null));
    }

    @Test
    void constructorRejectsNullAuthorList() {
        Genre genre = new Genre("Drama");
        assertThrows(IllegalArgumentException.class, () -> new Book("9780134685991", "Title", "Desc", genre, null, null));
    }

    @Test
    void constructorRejectsEmptyAuthorList() {
        Genre genre = new Genre("Drama");
        assertThrows(IllegalArgumentException.class, () -> new Book("9780134685991", "Title", "Desc", genre, new ArrayList<>(), null));
    }

    @Test
    void constructorAllowsMultipleAuthors() {
        Genre genre = new Genre("Drama");
        List<Author> authors = List.of(
                new Author("Alice", "Bio", null),
                new Author("Bob", "Bio", null)
        );

        assertDoesNotThrow(() -> new Book("9780134685991", "Title", "Desc", genre, authors, "photos/cover.png"));
    }
}
