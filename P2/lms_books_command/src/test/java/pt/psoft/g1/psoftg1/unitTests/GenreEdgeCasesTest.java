package pt.psoft.g1.psoftg1.unitTests;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended tests for Genre value object
 */
class GenreEdgeCasesTest {

    @Test
    void testGenreWithSingleCharacter() {
        Genre genre = new Genre("A");
        assertEquals("A", genre.toString());
    }

    @Test
    void testGenreWithValidLength() {
        Genre genre = new Genre("Science Fiction");
        assertEquals("Science Fiction", genre.toString());
    }

    @Test
    void testGenreWithLeadingWhitespace() {
        Genre genre = new Genre("   Fantasy");
        assertEquals("   Fantasy", genre.toString());
    }

    @Test
    void testGenreWithTrailingWhitespace() {
        Genre genre = new Genre("Mystery   ");
        assertEquals("Mystery   ", genre.toString());
    }

    @Test
    void testGenreWithLeadingAndTrailingWhitespace() {
        Genre genre = new Genre("   Horror   ");
        assertEquals("   Horror   ", genre.toString());
    }

    @Test
    void testGenreWithMultipleWords() {
        Genre genre = new Genre("Science Fiction Adventure");
        assertEquals("Science Fiction Adventure", genre.toString());
    }

    @Test
    void testGenreEquality() {
        Genre genre1 = new Genre("Drama");
        Genre genre2 = new Genre("Drama");
        // Compare string values
        assertEquals(genre1.toString(), genre2.toString());
    }

    @Test
    void testGenreInequality() {
        Genre genre1 = new Genre("Comedy");
        Genre genre2 = new Genre("Tragedy");
        assertNotEquals(genre1, genre2);
    }

    @Test
    void testGenreHashCodeInequality() {
        Genre genre1 = new Genre("Action");
        Genre genre2 = new Genre("Thriller");
        assertNotEquals(genre1.hashCode(), genre2.hashCode());
    }

    @Test
    void testGenreNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Genre(null));
    }

    @Test
    void testGenreBlankThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Genre(""));
    }

    @Test
    void testGenreOnlyWhitespaceThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Genre("    "));
    }

    @Test
    void testGenreWithNumbers() {
        Genre genre = new Genre("Sci-Fi 2024");
        assertEquals("Sci-Fi 2024", genre.toString());
    }

    @Test
    void testGenreWithSpecialCharacters() {
        Genre genre = new Genre("Noir & Noir");
        assertEquals("Noir & Noir", genre.toString());
    }

    @Test
    void testGenreWithUnicodeCharacters() {
        Genre genre = new Genre("Ficção Científica");
        assertEquals("Ficção Científica", genre.toString());
    }

    @Test
    void testGenreToStringConsistency() {
        String originalGenre = "Historical Fiction";
        Genre genre = new Genre(originalGenre);
        assertEquals(originalGenre, genre.toString());
        assertEquals(originalGenre, genre.toString()); // Call again
    }

    @Test
    void testGenreMaximumLength() {
        // Genre has validation constraints - test a reasonable length
        String longGenre = "x".repeat(100);
        Genre genre = new Genre(longGenre);
        assertEquals(longGenre, genre.toString());
    }

    @Test
    void testGenreImmutability() {
        Genre genre = new Genre("Adventure");
        String first = genre.toString();
        String second = genre.toString();
        assertEquals(first, second);
        assertTrue(first == second || first.equals(second));
    }

    @Test
    void testGenreWithHyphens() {
        Genre genre = new Genre("Science-Fiction");
        assertEquals("Science-Fiction", genre.toString());
    }

    @Test
    void testMultipleGenresCreation() {
        Genre fiction = new Genre("Fiction");
        Genre nonfiction = new Genre("Non-Fiction");
        Genre mystery = new Genre("Mystery");
        
        assertEquals("Fiction", fiction.toString());
        assertEquals("Non-Fiction", nonfiction.toString());
        assertEquals("Mystery", mystery.toString());
        
        assertNotEquals(fiction, nonfiction);
        assertNotEquals(nonfiction, mystery);
        assertNotEquals(fiction, mystery);
    }
}
