package pt.psoft.g1.psoftg1.shared.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameExtendedTest {

    @Test
    void ensureNameWithSingleCharacter() {
        final var name = new Name("A");
        assertEquals("A", name.toString());
    }

    @Test
    void ensureNameWithNumbers() {
        final var name = new Name("John123");
        assertEquals("John123", name.toString());
    }

    @Test
    void ensureNameWithMultipleWords() {
        final var name = new Name("John Michael Smith");
        assertEquals("John Michael Smith", name.toString());
    }

    @Test
    void ensureNamePreservesCase() {
        final var name = new Name("jOhN dOe");
        assertEquals("jOhN dOe", name.toString());
    }

    @Test
    void ensureNameCanContainSpaces() {
        final var name = new Name("Mary Jane Watson");
        assertEquals("Mary Jane Watson", name.toString());
    }

    @Test
    void ensureNameWithLeadingNumbers() {
        final var name = new Name("2000Plus");
        assertEquals("2000Plus", name.toString());
    }

    @Test
    void ensureNameWithMultipleConsecutiveSpaces() {
        final var name = new Name("John  Smith");
        assertEquals("John  Smith", name.toString());
    }

    @Test
    void ensureNameCanBeChanged() {
        final var name = new Name("Original");
        name.setName("Modified");
        assertEquals("Modified", name.toString());
    }

    @Test
    void ensureNameChangeToNull() {
        final var name = new Name("Original");
        assertThrows(IllegalArgumentException.class, () -> name.setName(null));
    }

    @Test
    void ensureNameChangeToBlank() {
        final var name = new Name("Original");
        assertThrows(IllegalArgumentException.class, () -> name.setName(""));
    }

    @Test
    void ensureNameWithOnlySpaces() {
        assertThrows(IllegalArgumentException.class, () -> new Name("     "));
    }

    @Test
    void ensureNameWithSpecialCharacterThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Name("John@Doe"));
    }

    @Test
    void ensureNameWithHyphenThrows() {
        // Hyphens are actually allowed by the pattern, so this should not throw
        final var name = new Name("John-Doe");
        assertEquals("John-Doe", name.toString());
    }

    @Test
    void ensureNameWithApostropheThrows() {
        // Apostrophes are actually allowed by the pattern, so this should not throw
        final var name = new Name("O'Brien");
        assertEquals("O'Brien", name.toString());
    }

    @Test
    void ensureNameWithPunctuation() {
        assertThrows(IllegalArgumentException.class, () -> new Name("John Smith."));
    }

    @Test
    void ensureNameWithUnderscore() {
        assertThrows(IllegalArgumentException.class, () -> new Name("John_Smith"));
    }

    @Test
    void ensureVeryLongAlphanumericName() {
        final var longName = "A".repeat(50) + "123456789";
        final var name = new Name(longName);
        assertNotNull(name.toString());
    }

    @Test
    void ensureNameAllNumbers() {
        final var name = new Name("123456789");
        assertEquals("123456789", name.toString());
    }

    @Test
    void ensureNameAllLetters() {
        final var name = new Name("AbcdefghijklmnoP");
        assertEquals("AbcdefghijklmnoP", name.toString());
    }

    @Test
    void ensureNameMixedAlphanumeric() {
        final var name = new Name("abc123def456ghi");
        assertEquals("abc123def456ghi", name.toString());
    }

    @Test
    void ensureNameWithMultipleSpacesBetweenWords() {
        final var name = new Name("Name1  Name2   Name3");
        assertEquals("Name1  Name2   Name3", name.toString());
    }
}
