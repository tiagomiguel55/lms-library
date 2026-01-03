package model;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.bookmanagement.model.Isbn;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional edge case tests for ISBN validation
 */
class IsbnEdgeCasesTest {

    @Test
    void testIsbn13WithHyphens() {
        // Testing ISBN-13 with hyphens should fail
        assertThrows(IllegalArgumentException.class, () -> new Isbn("978-0-544-00341-5"));
    }

    @Test
    void testIsbn10WithHyphens() {
        // Testing ISBN-10 with hyphens should fail
        assertThrows(IllegalArgumentException.class, () -> new Isbn("0-14-028329-X"));
    }

    @Test
    void testIsbnWithSpaces() {
        // Testing ISBN with spaces should fail
        assertThrows(IllegalArgumentException.class, () -> new Isbn("978 0 544 003415"));
    }

    @Test
    void testIsbnTooShort() {
        // Testing ISBN that is too short should fail
        assertThrows(IllegalArgumentException.class, () -> new Isbn("123456789"));
    }

    @Test
    void testIsbnTooLong() {
        // Testing ISBN that is too long should fail
        assertThrows(IllegalArgumentException.class, () -> new Isbn("9780544003415123"));
    }

    @Test
    void testIsbn10WithInvalidCheckDigit() {
        // Testing ISBN-10 with wrong check digit
        assertThrows(IllegalArgumentException.class, () -> new Isbn("0545134543"));
    }

    @Test
    void testIsbn10WithValidCheckDigitX() {
        // Testing valid ISBN-10 with X as check digit
        Isbn isbn = new Isbn("043942089X");
        assertEquals("043942089X", isbn.toString());
    }

    @Test
    void testIsbnValidWith978Prefix() {
        // Testing valid ISBN-13 starting with 978
        Isbn isbn = new Isbn("9780544003415");
        assertDoesNotThrow(() -> new Isbn("9780544003415"));
    }

    @Test
    void testIsbn10WithLeadingZero() {
        // Testing ISBN-10 that starts with 0
        Isbn isbn = new Isbn("0451524934");
        assertEquals("0451524934", isbn.toString());
    }

    @Test
    void testIsbnToStringReturnsOriginalValue() {
        String isbn13 = "9782826012092";
        Isbn isbn = new Isbn(isbn13);
        assertEquals(isbn13, isbn.toString());
    }

    @Test
    void testIsbn10ToStringReturnsOriginalValue() {
        String isbn10 = "8175257660";
        Isbn isbn = new Isbn(isbn10);
        assertEquals(isbn10, isbn.toString());
    }

    @Test
    void testIsbnEquality() {
        Isbn isbn1 = new Isbn("9782826012092");
        Isbn isbn2 = new Isbn("9782826012092");
        assertEquals(isbn1, isbn2);
    }

    @Test
    void testIsbnInequality() {
        Isbn isbn1 = new Isbn("9782826012092");
        Isbn isbn2 = new Isbn("9780544003415");
        assertNotEquals(isbn1, isbn2);
    }

    @Test
    void testIsbnHashCode() {
        Isbn isbn1 = new Isbn("9782826012092");
        Isbn isbn2 = new Isbn("9782826012092");
        assertEquals(isbn1.hashCode(), isbn2.hashCode());
    }

    @Test
    void testIsbnHashCodeInequality() {
        Isbn isbn1 = new Isbn("9782826012092");
        Isbn isbn2 = new Isbn("9780544003415");
        assertNotEquals(isbn1.hashCode(), isbn2.hashCode());
    }

    @Test
    void testMultipleIsbn13Checksums() {
        // Valid ISBN-13 with different check digits
        Isbn isbn1 = new Isbn("9780544003415");
        Isbn isbn2 = new Isbn("9780307476357");
        assertEquals("9780544003415", isbn1.toString());
        assertEquals("9780307476357", isbn2.toString());
    }

    @Test
    void testIsbn13ChecksumCalculation() {
        // Test an ISBN-13 where checksum is 0
        Isbn isbn = new Isbn("9780135583227");
        assertEquals("9780135583227", isbn.toString());
    }

    @Test
    void testIsbn10ContainsOnlyDigitsAndX() {
        // Test ISBN-10 with invalid character
        assertThrows(IllegalArgumentException.class, () -> new Isbn("043942089-"));
    }

    @Test
    void testIsbnNullCheckBeforeValidation() {
        // Null should be caught before length validation
        assertThrows(IllegalArgumentException.class, () -> new Isbn(null));
    }

    @Test
    void testIsbnEmptyStringAfterTrim() {
        // Empty string should fail
        assertThrows(IllegalArgumentException.class, () -> new Isbn(""));
    }

    @Test
    void testIsbnWithOnlySpaces() {
        // String with only spaces should fail
        assertThrows(IllegalArgumentException.class, () -> new Isbn("          "));
    }
}
