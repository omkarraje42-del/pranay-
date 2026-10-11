package com.library.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "librarian@library.com",
            "admin@univ.edu",
            "jane.doe@example.org",
            "first+last@sub.domain.co"
    })
    @DisplayName("Should accept valid email addresses")
    void shouldAcceptValidEmail(String email) {
        assertThat(ValidationUtil.isValidEmail(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plainaddress",
            "@missingusername.com",
            "username@.com",
            "username@domain",
            ""
    })
    @DisplayName("Should reject invalid email addresses")
    void shouldRejectInvalidEmail(String email) {
        assertThat(ValidationUtil.isValidEmail(email)).isFalse();
    }

    @Test
    @DisplayName("Should reject null email")
    void shouldRejectNullEmail() {
        assertThat(ValidationUtil.isValidEmail(null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "9780132350884",
            "978-0-13-235088-4",
            "0201633612",
            "0-201-63361-2"
    })
    @DisplayName("Should accept valid ISBN-10 and ISBN-13 strings")
    void shouldAcceptValidIsbn(String isbn) {
        assertThat(ValidationUtil.isValidIsbn(isbn)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123",
            "978123456789012345",
            "abcdef"
    })
    @DisplayName("Should reject malformed ISBN values")
    void shouldRejectMalformedIsbn(String isbn) {
        assertThat(ValidationUtil.isValidIsbn(isbn)).isFalse();
    }

    @Test
    @DisplayName("Should consider null or empty ISBN as optional/valid")
    void shouldHandleOptionalIsbn() {
        assertThat(ValidationUtil.isValidIsbn(null)).isTrue();
        assertThat(ValidationUtil.isValidIsbn("")).isTrue();
    }

    @Test
    @DisplayName("Should trim and sanitize string inputs correctly")
    void shouldSanitizeInput() {
        assertThat(ValidationUtil.sanitize("  hello world  ")).isEqualTo("hello world");
        assertThat(ValidationUtil.sanitize(null)).isNull();
    }

    @Test
    @DisplayName("Should accurately check for non-empty strings")
    void shouldCheckNotEmpty() {
        assertThat(ValidationUtil.isNotEmpty("hello")).isTrue();
        assertThat(ValidationUtil.isNotEmpty("   ")).isFalse();
        assertThat(ValidationUtil.isNotEmpty("")).isFalse();
        assertThat(ValidationUtil.isNotEmpty(null)).isFalse();
    }
}
