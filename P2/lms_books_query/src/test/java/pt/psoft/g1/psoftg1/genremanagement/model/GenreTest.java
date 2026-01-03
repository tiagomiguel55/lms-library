package pt.psoft.g1.psoftg1.genremanagement.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GenreTest {

    @Test
    void ensureGenreMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new Genre(null));
    }

    @Test
    void ensureGenreMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Genre(""));
    }

    @Test
    void ensureGenreMustNotBeOnlyWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> new Genre("   "));
    }

    @Test
    void ensureGenreMustNotBeOversize() {
        final var longGenre = "A".repeat(101);
        assertThrows(IllegalArgumentException.class, () -> new Genre(longGenre));
    }

    @Test
    void ensureGenreIsSet() {
        final var genre = new Genre("Science Fiction");
        assertEquals("Science Fiction", genre.toString());
    }

    @Test
    void ensureGenreCanHaveWhitespace() {
        final var genre = new Genre("Science Fiction and Fantasy");
        assertEquals("Science Fiction and Fantasy", genre.toString());
    }

    @Test
    void ensureGenreCanHaveHyphens() {
        final var genre = new Genre("Science-Fiction");
        assertEquals("Science-Fiction", genre.toString());
    }

    @Test
    void ensureGenreCanBeComparedByName() {
        final var genre1 = new Genre("Science Fiction");
        final var genre2 = new Genre("Science Fiction");
        
        assertEquals(genre1.toString(), genre2.toString());
    }

    @Test
    void ensureDifferentGenresHaveDifferentNames() {
        final var genre1 = new Genre("Science Fiction");
        final var genre2 = new Genre("Fantasy");
        
        assertNotEquals(genre1.toString(), genre2.toString());
    }

    @Test
    void ensureGenreAtMaximumLength() {
        final var maxGenre = "A".repeat(100);
        final var genre = new Genre(maxGenre);
        
        assertEquals(maxGenre, genre.toString());
    }

    @Test
    void ensureGenrePreservesCase() {
        final var genre = new Genre("sCiEnCe FiCtIoN");
        assertEquals("sCiEnCe FiCtIoN", genre.toString());
    }

    @Test
    void ensureGenreWithSpecialCharacters() {
        final var genre = new Genre("Sci-Fi & Fantasy");
        assertEquals("Sci-Fi & Fantasy", genre.toString());
    }
}
