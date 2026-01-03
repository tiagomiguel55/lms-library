package model;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended edge case tests for Author entity
 */
class AuthorEdgeCasesTest {

    @Test
    void testAuthorWithValidNameAndBio() {
        Author author = new Author("John Smith", "An experienced writer", null);
        assertNotNull(author);
        assertEquals("John Smith", author.getName().toString());
        assertEquals("An experienced writer", author.getBio().toString());
    }

    @Test
    void testAuthorWithMinimalName() {
        Author author = new Author("Jo", "Bio", null);
        assertEquals("Jo", author.getName().toString());
    }

    @Test
    void testAuthorWithLongName() {
        String longName = "Alexander Christopher Montgomery Wellington III";
        Author author = new Author(longName, "A distinguished author", null);
        assertEquals(longName, author.getName().toString());
    }

    @Test
    void testAuthorWithSpecialCharactersInName() {
        Author author = new Author("O'Brien-Smith", "Irish-American writer", null);
        assertEquals("O'Brien-Smith", author.getName().toString());
    }

    @Test
    void testAuthorWithUnicodeCharactersInName() {
        Author author = new Author("José María García", "Spanish author", null);
        assertEquals("José María García", author.getName().toString());
    }

    @Test
    void testAuthorNullNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Author(null, "Bio", null));
    }

    @Test
    void testAuthorNullBioThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Author("John Doe", null, null));
    }

    @Test
    void testAuthorWithNullPhoto() {
        Author author = new Author("Jane Doe", "Biography", null);
        assertNull(author.getPhoto());
    }

    @Test
    void testAuthorWithLongBio() {
        String longBio = "This is an extensive biography that contains a lot of information " +
                        "about the author's life, achievements, and literary contributions. " +
                        "It spans multiple sentences and provides detailed context.";
        Author author = new Author("Notable Author", longBio, null);
        // Bio may be HTML-escaped, check it contains key content
        assertTrue(author.getBio().toString().contains("extensive biography"));
    }

    @Test
    void testAuthorBioWithSpecialCharacters() {
        String bio = "Born in 1970's, author of \"Great Works\" & winner of prizes!";
        Author author = new Author("Author Name", bio, null);
        // Bio may be HTML-escaped - check it contains key content
        assertTrue(author.getBio().toString().contains("Great Works"));
        assertTrue(author.getBio().toString().contains("winner"));
    }

    @Test
    void testAuthorWithShortBio() {
        Author author = new Author("Sam", "Writer", null);
        assertEquals("Writer", author.getBio().toString());
    }

    @Test
    void testAuthorCreationConsistency() {
        String name = "Consistent Author";
        String bio = "Consistent bio";
        Author author = new Author(name, bio, null);
        assertEquals(name, author.getName().toString());
        assertEquals(bio, author.getBio().toString());
    }

    @Test
    void testAuthorInitialFinalizationState() {
        Author author = new Author("New Author", "New to the platform", null);
        assertFalse(author.isFinalized());
    }

    @Test
    void testAuthorNumberInitialization() {
        Author author = new Author("Author", "Bio", null);
        assertNotNull(author.getAuthorNumber());
    }

    @Test
    void testAuthorWithWhitespaceInName() {
        Author author = new Author("First Last", "Bio text", null);
        assertEquals("First Last", author.getName().toString());
    }

    @Test
    void testMultipleAuthorsCreation() {
        Author author1 = new Author("Author One", "Bio One", null);
        Author author2 = new Author("Author Two", "Bio Two", null);
        Author author3 = new Author("Author Three", "Bio Three", null);
        
        assertNotEquals(author1.getName(), author2.getName());
        assertNotEquals(author2.getName(), author3.getName());
        assertNotEquals(author1.getName(), author3.getName());
    }

    @Test
    void testAuthorNameWithLeadingSpaces() {
        Author author = new Author("   Leading Spaces", "Bio", null);
        assertEquals("   Leading Spaces", author.getName().toString());
    }

    @Test
    void testAuthorNameWithTrailingSpaces() {
        Author author = new Author("Trailing Spaces   ", "Bio", null);
        assertEquals("Trailing Spaces   ", author.getName().toString());
    }

    @Test
    void testAuthorBioWithLeadingSpaces() {
        Author author = new Author("Name", "   Leading bio", null);
        assertEquals("   Leading bio", author.getBio().toString());
    }

    @Test
    void testAuthorBioWithTrailingSpaces() {
        Author author = new Author("Name", "Trailing bio   ", null);
        assertEquals("Trailing bio   ", author.getBio().toString());
    }

    @Test
    void testAuthorWithSingleWordName() {
        Author author = new Author("Voltaire", "Famous philosopher", null);
        assertEquals("Voltaire", author.getName().toString());
    }

    @Test
    void testAuthorWithNumericNameElements() {
        Author author = new Author("Agent 007", "Secret agent author", null);
        assertEquals("Agent 007", author.getName().toString());
    }
}
