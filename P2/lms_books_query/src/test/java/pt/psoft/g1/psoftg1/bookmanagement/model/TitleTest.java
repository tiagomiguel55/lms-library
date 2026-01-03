package pt.psoft.g1.psoftg1.bookmanagement.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TitleTest {

    @Test
    void ensureTitleMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new Title(null));
    }

    @Test
    void ensureTitleMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Title(""));
    }

    @Test
    void ensureTitleMustNotBeOnlyWhitespace() {
        assertThrows(IllegalArgumentException.class, () -> new Title("   "));
    }

    @Test
    void ensureTitleMustNotBeOversize() {
        final var longTitle = "A".repeat(129);
        assertThrows(IllegalArgumentException.class, () -> new Title(longTitle));
    }

    @Test
    void ensureTitleIsSet() {
        final var title = new Title("The Great Gatsby");
        assertEquals("The Great Gatsby", title.getTitle());
    }

    @Test
    void ensureTitleCanHaveMultipleWords() {
        final var title = new Title("The Lord of the Rings");
        assertEquals("The Lord of the Rings", title.getTitle());
    }

    @Test
    void ensureTitleCanHaveNumbers() {
        final var title = new Title("2001: A Space Odyssey");
        assertEquals("2001: A Space Odyssey", title.getTitle());
    }

    @Test
    void ensureTitleCanHaveSpecialCharacters() {
        final var title = new Title("Harry Potter & The Goblet of Fire");
        assertEquals("Harry Potter & The Goblet of Fire", title.getTitle());
    }

    @Test
    void ensureTitleAtMaximumLength() {
        final var maxTitle = "A".repeat(128);
        final var title = new Title(maxTitle);
        assertEquals(maxTitle, title.getTitle());
    }

    @Test
    void ensureTitlePreservesCase() {
        final var title = new Title("ThE gReAt gAtSbY");
        assertEquals("ThE gReAt gAtSbY", title.getTitle());
    }

    @Test
    void ensureTitleWithLeadingWhitespace() {
        // Assuming implementation doesn't trim leading whitespace
        final var title = new Title(" The Great Gatsby");
        assertNotNull(title.getTitle());
    }

    @Test
    void ensureTitleWithTrailingWhitespace() {
        final var title = new Title("The Great Gatsby ");
        assertNotNull(title.getTitle());
    }

    @Test
    void ensureTitleCanHavePunctuation() {
        final var title = new Title("Pride, Prejudice, and Power");
        assertEquals("Pride, Prejudice, and Power", title.getTitle());
    }

    @Test
    void ensureTitleWithQuotes() {
        final var title = new Title("\"Quoted Title\"");
        assertEquals("\"Quoted Title\"", title.getTitle());
    }

    @Test
    void ensureTitleWithApostrophe() {
        final var title = new Title("It's a Wonderful Life");
        assertEquals("It's a Wonderful Life", title.getTitle());
    }

    @Test
    void ensureTitleCanBeSetAfterCreation() {
        final var title = new Title("Original Title");
        title.setTitle("New Title");
        assertEquals("New Title", title.getTitle());
    }

    @Test
    void ensureSetTitleCannotBeNull() {
        final var title = new Title("Original Title");
        assertThrows(IllegalArgumentException.class, () -> title.setTitle(null));
    }

    @Test
    void ensureSetTitleCannotBeBlank() {
        final var title = new Title("Original Title");
        assertThrows(IllegalArgumentException.class, () -> title.setTitle(""));
    }

    @Test
    void ensureTitleCanHaveHyphens() {
        final var title = new Title("X-Ray and The Inner Secrets");
        assertEquals("X-Ray and The Inner Secrets", title.getTitle());
    }

    @Test
    void ensureTitleWithUnicodeCharacters() {
        final var title = new Title("Café: A Story of Coffee");
        assertEquals("Café: A Story of Coffee", title.getTitle());
    }

    @Test
    void ensureSingleCharacterTitle() {
        final var title = new Title("A");
        assertEquals("A", title.getTitle());
    }

    @Test
    void ensureMinimumLengthTitle() {
        final var title = new Title("To be");
        assertEquals("To be", title.getTitle());
    }
}
