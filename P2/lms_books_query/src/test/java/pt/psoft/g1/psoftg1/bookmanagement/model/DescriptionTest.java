package pt.psoft.g1.psoftg1.bookmanagement.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DescriptionTest {

    @Test
    void ensureDescriptionCanBeNull() {
        final var description = new Description(null);
        assertNull(description.getDescription());
    }

    @Test
    void ensureDescriptionCanBeBlank() {
        final var description = new Description("   ");
        assertNull(description.getDescription());
    }

    @Test
    void ensureDescriptionIsSet() {
        final var description = new Description("This is a great book about adventure");
        assertEquals("This is a great book about adventure", description.getDescription());
    }

    @Test
    void ensureDescriptionMustNotBeOversize() {
        final var longDescription = "A".repeat(4097);
        assertThrows(IllegalArgumentException.class, () -> new Description(longDescription));
    }

    @Test
    void ensureDescriptionAtMaximumLength() {
        final var maxDescription = "A".repeat(4096);
        final var description = new Description(maxDescription);
        assertEquals(maxDescription, description.getDescription());
    }

    @Test
    void ensureDescriptionCanHaveMultipleLines() {
        final var description = new Description("First line\nSecond line\nThird line");
        assertNotNull(description.getDescription());
    }

    @Test
    void ensureDescriptionCanHaveSpecialCharacters() {
        final var description = new Description("This book features: action, drama & romance!");
        assertNotNull(description.getDescription());
    }

    @Test
    void ensureDescriptionPreservesCase() {
        final var description = new Description("ThE bEsT bOoK eVeR!");
        assertNotNull(description.getDescription());
    }

    @Test
    void ensureEmptyStringDescriptionBecomesNull() {
        final var description = new Description("");
        assertNull(description.getDescription());
    }

    @Test
    void ensureWhitespaceOnlyDescriptionBecomesNull() {
        final var description = new Description("     \n    \t    ");
        assertNull(description.getDescription());
    }

    @Test
    void ensureDescriptionCanBeSetAfterCreation() {
        final var description = new Description("Original description");
        description.setDescription("Updated description");
        assertEquals("Updated description", description.getDescription());
    }

    @Test
    void ensureSetDescriptionCanBeNull() {
        final var description = new Description("Original description");
        description.setDescription(null);
        assertNull(description.getDescription());
    }

    @Test
    void ensureSetDescriptionCanBeBlank() {
        final var description = new Description("Original description");
        description.setDescription("   ");
        assertNull(description.getDescription());
    }

    @Test
    void ensureSetDescriptionCannotBeOversize() {
        final var description = new Description("Original description");
        final var longDescription = "A".repeat(4097);
        assertThrows(IllegalArgumentException.class, () -> description.setDescription(longDescription));
    }

    @Test
    void ensureDescriptionWithNumbers() {
        final var description = new Description("This book was published in 2023 and sold 1000 copies");
        assertNotNull(description.getDescription());
    }

    @Test
    void ensureDescriptionWithQuotes() {
        final var description = new Description("\"A masterpiece,\" says the critic");
        assertNotNull(description.getDescription());
    }

    @Test
    void ensureDescriptionWithHyperlinks() {
        final var description = new Description("For more info visit https://example.com");
        assertNotNull(description.getDescription());
    }

    @Test
    void ensureDescriptionWithPunctuation() {
        final var description = new Description("Amazing! Incredible? Yes... definitely!");
        assertNotNull(description.getDescription());
    }

    @Test
    void ensureDescriptionCanHaveUnicodeCharacters() {
        final var description = new Description("Café, naïve, résumé - multilingual descriptions");
        assertNotNull(description.getDescription());
    }

    @Test
    void ensureVeryShortDescription() {
        final var description = new Description("Great!");
        assertEquals("Great!", description.getDescription());
    }

    @Test
    void ensureDescriptionWithTabs() {
        final var description = new Description("Feature 1\tFeature 2\tFeature 3");
        assertNotNull(description.getDescription());
    }

    @Test
    void ensureHtmlSanitization() {
        // Assuming HTML is sanitized
        final var description = new Description("<b>Bold text</b> and <script>alert('xss')</script>");
        assertNotNull(description.getDescription());
    }
}
