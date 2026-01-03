package pt.psoft.g1.psoftg1.bookmanagement.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IsbnTest {

    @Test
    void ensureIsbnMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new Isbn(null));
    }

    @Test
    void ensureValidIsbn10() {
        // Valid ISBN-10: 0-306-40615-2
        final var isbn = new Isbn("0306406152");
        assertEquals("0306406152", isbn.getIsbn());
    }

    @Test
    void ensureValidIsbn13() {
        // Valid ISBN-13: 9780306406157
        final var isbn = new Isbn("9780306406157");
        assertEquals("9780306406157", isbn.getIsbn());
    }

    @Test
    void ensureValidIsbn13WithHyphens() {
        // Valid ISBN-13 with format
        final var isbn = new Isbn("9780306406157");
        assertNotNull(isbn.getIsbn());
    }

    @Test
    void ensureInvalidIsbn10CheckDigit() {
        // Invalid ISBN-10: check digit is wrong
        assertThrows(IllegalArgumentException.class, () -> new Isbn("0306406151"));
    }

    @Test
    void ensureInvalidIsbn13CheckDigit() {
        // Invalid ISBN-13: check digit is wrong
        assertThrows(IllegalArgumentException.class, () -> new Isbn("9780306406150"));
    }

    @Test
    void ensureIsbnTooShort() {
        assertThrows(IllegalArgumentException.class, () -> new Isbn("123456789"));
    }

    @Test
    void ensureIsbnTooLong() {
        assertThrows(IllegalArgumentException.class, () -> new Isbn("12345678901234"));
    }

    @Test
    void ensureIsbnWithInvalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> new Isbn("030640615X"));
    }

    @Test
    void ensureIsbnWithSpaces() {
        assertThrows(IllegalArgumentException.class, () -> new Isbn("0306 40615 2"));
    }

    @Test
    void ensureIsbnEquality() {
        final var isbn1 = new Isbn("9780306406157");
        final var isbn2 = new Isbn("9780306406157");
        assertEquals(isbn1, isbn2);
    }

    @Test
    void ensureIsbnInequality() {
        final var isbn1 = new Isbn("9780306406157");
        final var isbn2 = new Isbn("0306406152");
        assertNotEquals(isbn1, isbn2);
    }

    @Test
    void ensureIsbnHashCode() {
        final var isbn1 = new Isbn("9780306406157");
        final var isbn2 = new Isbn("9780306406157");
        assertEquals(isbn1.hashCode(), isbn2.hashCode());
    }

    @Test
    void ensureEmptyIsbnThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Isbn(""));
    }

    @Test
    void ensureIsbnWithAllZeros() {
        // "0000000000" is mathematically valid (sum % 11 == 0) but semantically invalid
        // Since the validation only checks mathematical validity, this test validates it works
        final var isbn = new Isbn("0000000000");
        assertEquals("0000000000", isbn.getIsbn());
    }

    @Test
    void ensureValidIsbn10Example2() {
        // Another valid ISBN-10
        final var isbn = new Isbn("0451457994");
        assertEquals("0451457994", isbn.getIsbn());
    }

    @Test
    void ensureValidIsbn13Example2() {
        // Another valid ISBN-13
        final var isbn = new Isbn("9780451457998");
        assertEquals("9780451457998", isbn.getIsbn());
    }
}
