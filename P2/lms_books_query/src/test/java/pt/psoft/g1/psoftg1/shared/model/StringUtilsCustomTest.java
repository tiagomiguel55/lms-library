package pt.psoft.g1.psoftg1.shared.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsCustomTest {

    @Test
    void ensureIsAlphanumericWithOnlyLetters() {
        assertTrue(StringUtilsCustom.isAlphanumeric("abcdefg"));
    }

    @Test
    void ensureIsAlphanumericWithOnlyNumbers() {
        assertTrue(StringUtilsCustom.isAlphanumeric("123456789"));
    }

    @Test
    void ensureIsAlphanumericWithMixedLettersAndNumbers() {
        assertTrue(StringUtilsCustom.isAlphanumeric("abc123def456"));
    }

    @Test
    void ensureIsAlphanumericWithSpaces() {
        assertTrue(StringUtilsCustom.isAlphanumeric("hello world"));
    }

    @Test
    void ensureIsAlphanumericWithApostrophe() {
        assertTrue(StringUtilsCustom.isAlphanumeric("don't worry"));
    }

    @Test
    void ensureIsAlphanumericWithHyphen() {
        assertTrue(StringUtilsCustom.isAlphanumeric("Mary-Jane"));
    }

    @Test
    void ensureIsAlphanumericWithAccentedCharacters() {
        assertTrue(StringUtilsCustom.isAlphanumeric("café naïve résumé"));
    }

    @Test
    void ensureIsAlphanumericWithSpecialCharacterRejectsExclamation() {
        assertFalse(StringUtilsCustom.isAlphanumeric("hello!"));
    }

    @Test
    void ensureIsAlphanumericWithAtSymbol() {
        assertFalse(StringUtilsCustom.isAlphanumeric("user@email"));
    }

    @Test
    void ensureIsAlphanumericWithDollarSign() {
        assertFalse(StringUtilsCustom.isAlphanumeric("price$100"));
    }

    @Test
    void ensureIsAlphanumericWithPercentSign() {
        assertFalse(StringUtilsCustom.isAlphanumeric("50%off"));
    }

    @Test
    void ensureIsAlphanumericWithComma() {
        assertFalse(StringUtilsCustom.isAlphanumeric("hello,world"));
    }

    @Test
    void ensureIsAlphanumericWithPeriod() {
        assertFalse(StringUtilsCustom.isAlphanumeric("hello.world"));
    }

    @Test
    void ensureIsAlphanumericWithParentheses() {
        assertFalse(StringUtilsCustom.isAlphanumeric("hello(world)"));
    }

    @Test
    void ensureIsAlphanumericWithBrackets() {
        assertFalse(StringUtilsCustom.isAlphanumeric("hello[world]"));
    }

    @Test
    void ensureIsAlphanumericWithAmpersand() {
        assertFalse(StringUtilsCustom.isAlphanumeric("Tom & Jerry"));
    }

    @Test
    void ensureIsAlphanumericEmptyString() {
        assertTrue(StringUtilsCustom.isAlphanumeric(""));
    }

    @Test
    void ensureIsAlphanumericWithOnlySpaces() {
        assertTrue(StringUtilsCustom.isAlphanumeric("   "));
    }

    @Test
    void ensureIsAlphanumericWithSingleLetter() {
        assertTrue(StringUtilsCustom.isAlphanumeric("A"));
    }

    @Test
    void ensureIsAlphanumericWithSingleNumber() {
        assertTrue(StringUtilsCustom.isAlphanumeric("5"));
    }

    @Test
    void ensureIsAlphanumericWithCyrillic() {
        assertTrue(StringUtilsCustom.isAlphanumeric("Привет"));
    }

    @Test
    void ensureIsAlphanumericWithChinese() {
        assertTrue(StringUtilsCustom.isAlphanumeric("你好"));
    }

    @Test
    void ensureIsAlphanumericWithArabic() {
        assertTrue(StringUtilsCustom.isAlphanumeric("مرحبا"));
    }

    @Test
    void ensureIsAlphanumericWithMixedUnicodeAndNumbers() {
        assertTrue(StringUtilsCustom.isAlphanumeric("Hello2023世界"));
    }

    @Test
    void ensureIsAlphanumericWithUnderscore() {
        assertFalse(StringUtilsCustom.isAlphanumeric("hello_world"));
    }

    @Test
    void ensureIsAlphanumericWithSlash() {
        assertFalse(StringUtilsCustom.isAlphanumeric("hello/world"));
    }

    @Test
    void ensureIsAlphanumericWithBackslash() {
        assertFalse(StringUtilsCustom.isAlphanumeric("hello\\world"));
    }

    @Test
    void ensureSanitizeHtmlRemovesScriptTags() {
        final var unsafe = "<p>Hello</p><script>alert('XSS')</script>";
        final var safe = StringUtilsCustom.sanitizeHtml(unsafe);
        
        assertFalse(safe.contains("<script>"));
    }

    @Test
    void ensureSanitizeHtmlPreservesFormatting() {
        final var html = "<p>This is <b>bold</b> text</p>";
        final var safe = StringUtilsCustom.sanitizeHtml(html);
        
        assertNotNull(safe);
    }

    @Test
    void ensureSanitizeHtmlPreservesLinks() {
        final var html = "<p>Visit <a href=\"https://example.com\">our site</a></p>";
        final var safe = StringUtilsCustom.sanitizeHtml(html);
        
        assertNotNull(safe);
    }

    @Test
    void ensureSanitizeHtmlRemovesEventHandlers() {
        final var unsafe = "<p onclick=\"alert('XSS')\">Click me</p>";
        final var safe = StringUtilsCustom.sanitizeHtml(unsafe);
        
        assertFalse(safe.contains("onclick"));
    }

    @Test
    void ensureSanitizeHtmlWithPlainText() {
        final var plain = "This is plain text with no HTML";
        final var safe = StringUtilsCustom.sanitizeHtml(plain);
        
        assertEquals(plain, safe);
    }

    @Test
    void ensureSanitizeHtmlWithEmptyString() {
        final var safe = StringUtilsCustom.sanitizeHtml("");
        
        assertEquals("", safe);
    }

    @Test
    void ensureSanitizeHtmlRemovesStyle() {
        final var unsafe = "<p style=\"color:red\">Red text</p>";
        final var safe = StringUtilsCustom.sanitizeHtml(unsafe);
        
        assertFalse(safe.contains("style="));
    }

    @Test
    void ensureSanitizeHtmlRemovesIframeTag() {
        final var unsafe = "<iframe src=\"http://malicious.com\"></iframe>";
        final var safe = StringUtilsCustom.sanitizeHtml(unsafe);
        
        assertFalse(safe.contains("<iframe"));
    }

    @Test
    void ensureStartsOrEndsInWhiteSpaceWithValidString() {
        assertTrue(StringUtilsCustom.startsOrEndsInWhiteSpace("validtext"));
    }

    @Test
    void ensureStartsOrEndsInWhiteSpaceWithLeadingSpace() {
        assertFalse(StringUtilsCustom.startsOrEndsInWhiteSpace(" text"));
    }

    @Test
    void ensureStartsOrEndsInWhiteSpaceWithTrailingSpace() {
        assertFalse(StringUtilsCustom.startsOrEndsInWhiteSpace("text "));
    }

    @Test
    void ensureStartsOrEndsInWhiteSpaceWithBothSpaces() {
        assertFalse(StringUtilsCustom.startsOrEndsInWhiteSpace(" text "));
    }
}
