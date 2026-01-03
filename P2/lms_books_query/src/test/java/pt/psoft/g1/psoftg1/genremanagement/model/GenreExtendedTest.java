package pt.psoft.g1.psoftg1.genremanagement.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenreExtendedTest {

    @Test
    void ensureGenreWithMultipleWords() {
        final var genre = new Genre("Science Fiction and Fantasy");
        assertEquals("Science Fiction and Fantasy", genre.toString());
    }

    @Test
    void ensureGenreCanHavePunctuation() {
        final var genre = new Genre("Mystery, Thriller & Suspense");
        assertEquals("Mystery, Thriller & Suspense", genre.toString());
    }

    @Test
    void ensureGenreWithParentheses() {
        final var genre = new Genre("Fiction (Contemporary)");
        assertEquals("Fiction (Contemporary)", genre.toString());
    }

    @Test
    void ensureGenreWithSlash() {
        final var genre = new Genre("Action/Adventure");
        assertEquals("Action/Adventure", genre.toString());
    }

    @Test
    void ensureGenreWithApostrophe() {
        final var genre = new Genre("Children's Literature");
        assertEquals("Children's Literature", genre.toString());
    }

    @Test
    void ensureGenreAtMaxLength100() {
        final var maxGenre = "A".repeat(100);
        final var genre = new Genre(maxGenre);
        assertEquals(maxGenre, genre.toString());
        assertEquals(100, genre.toString().length());
    }

    @Test
    void ensureGenreAtLength1() {
        final var genre = new Genre("A");
        assertEquals("A", genre.toString());
        assertEquals(1, genre.toString().length());
    }

    @Test
    void ensureGenreWithNumbers() {
        final var genre = new Genre("20th Century Literature");
        assertEquals("20th Century Literature", genre.toString());
    }

    @Test
    void ensureGenreWithUnicodeCharacters() {
        final var genre = new Genre("Français Littérature");
        assertEquals("Français Littérature", genre.toString());
    }

    @Test
    void ensureMultipleGenresSameName() {
        final var genre1 = new Genre("Science Fiction");
        final var genre2 = new Genre("Science Fiction");
        
        assertEquals(genre1.toString(), genre2.toString());
    }

    @Test
    void ensureMultipleGenresDifferentNames() {
        final var genre1 = new Genre("Science Fiction");
        final var genre2 = new Genre("Fantasy");
        
        assertNotEquals(genre1.toString(), genre2.toString());
    }

    @Test
    void ensureGenreWithMultipleHyphens() {
        final var genre = new Genre("Science-Fiction-Fantasy");
        assertEquals("Science-Fiction-Fantasy", genre.toString());
    }

    @Test
    void ensureGenreWithMixedCase() {
        final var genre = new Genre("sMiXeD cAsE gEnRe");
        assertEquals("sMiXeD cAsE gEnRe", genre.toString());
    }

    @Test
    void ensureGenreWithLeadingSpace() {
        final var genre = new Genre(" Science Fiction");
        assertNotNull(genre.toString());
    }

    @Test
    void ensureGenreWithTrailingSpace() {
        final var genre = new Genre("Science Fiction ");
        assertNotNull(genre.toString());
    }

    @Test
    void ensureGenreWithMultipleConsecutiveSpaces() {
        final var genre = new Genre("Science  Fiction");
        assertEquals("Science  Fiction", genre.toString());
    }

    @Test
    void ensureGenreWithCommonSpecialChars() {
        final var genre = new Genre("Sci-Fi & Fantasy: A Journey");
        assertEquals("Sci-Fi & Fantasy: A Journey", genre.toString());
    }

    @Test
    void ensureGenreWithColonAndDash() {
        final var genre = new Genre("History - Ancient: Civilizations");
        assertEquals("History - Ancient: Civilizations", genre.toString());
    }

    @Test
    void ensureEmptyGenreThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Genre(""));
    }

    @Test
    void ensureGenreWithOnlySpacesThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Genre("     "));
    }

    @Test
    void ensureGenreWithOnlyTabsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Genre("\t\t\t"));
    }

    @Test
    void ensureNullGenreThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Genre(null));
    }

    @Test
    void ensureGenreTooLongThrows() {
        final var tooLongGenre = "A".repeat(101);
        assertThrows(IllegalArgumentException.class, () -> new Genre(tooLongGenre));
    }
}
