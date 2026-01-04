package pt.psoft.g1.psoftg1.unitTests;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.bookmanagement.model.Description;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended tests for Description value object
 */
class DescriptionEdgeCasesTest {

    @Test
    void testDescriptionWithMinimalLength() {
        // Test description at minimum boundary
        Description desc = new Description("A");
        assertEquals("A", desc.toString());
    }

    @Test
    void testDescriptionWithValidLength() {
        Description desc = new Description("This is a valid description");
        assertEquals("This is a valid description", desc.toString());
    }

    @Test
    void testDescriptionWithLeadingWhitespace() {
        Description desc = new Description("   Leading spaces");
        assertEquals("   Leading spaces", desc.toString());
    }

    @Test
    void testDescriptionWithTrailingWhitespace() {
        Description desc = new Description("Trailing spaces   ");
        assertEquals("Trailing spaces   ", desc.toString());
    }

    @Test
    void testDescriptionWithLeadingAndTrailingWhitespace() {
        Description desc = new Description("   Both sides   ");
        assertEquals("   Both sides   ", desc.toString());
    }

    @Test
    void testDescriptionWithSpecialCharacters() {
        String text = "Description with special chars: !@#$%^&*()";
        Description desc = new Description(text);
        // Values may be HTML-escaped
        assertTrue(desc.toString().contains("special chars"));
    }

    @Test
    void testDescriptionWithNewlines() {
        String text = "Description with\nnewlines";
        Description desc = new Description(text);
        assertTrue(desc.toString().contains("newlines"));
    }

    @Test
    void testDescriptionEquality() {
        Description desc1 = new Description("Same description");
        Description desc2 = new Description("Same description");
        // Compare string representations
        assertEquals(desc1.toString(), desc2.toString());
    }

    @Test
    void testDescriptionInequality() {
        Description desc1 = new Description("Description one");
        Description desc2 = new Description("Description two");
        assertNotEquals(desc1, desc2);
    }

    @Test
    void testDescriptionWithUnicodeCharacters() {
        String text = "Descrição com acentuação: é à ü ñ";
        Description desc = new Description(text);
        assertEquals(text, desc.toString());
    }

    @Test
    void testDescriptionMaximumLength() {
        // Create a description at maximum allowed length
        String longText = "x".repeat(500);
        Description desc = new Description(longText);
        assertEquals(longText, desc.toString());
    }

    @Test
    void testDescriptionToStringConsistency() {
        String original = "Consistent description";
        Description desc = new Description(original);
        assertEquals(original, desc.toString());
        assertEquals(original, desc.toString()); // Call again to ensure consistency
    }
}
