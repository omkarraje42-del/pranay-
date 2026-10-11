package com.library.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordUtilTest {

    @Test
    @DisplayName("Should hash password with BCrypt prefix $2a$ and verify correctly")
    void shouldHashAndVerifyPassword() {
        String raw = "StrongP@ssw0rd!2026";
        String hashed = PasswordUtil.hashPassword(raw);

        assertThat(hashed).isNotNull();
        assertThat(hashed).startsWith("$2a$");
        assertThat(hashed).isNotEqualTo(raw);

        assertThat(PasswordUtil.checkPassword(raw, hashed)).isTrue();
        assertThat(PasswordUtil.checkPassword("WrongPassword", hashed)).isFalse();
    }

    @Test
    @DisplayName("Should produce different salts for identical plain passwords")
    void shouldProduceDistinctSalts() {
        String raw = "SecretKey123";
        String hash1 = PasswordUtil.hashPassword(raw);
        String hash2 = PasswordUtil.hashPassword(raw);

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(PasswordUtil.checkPassword(raw, hash1)).isTrue();
        assertThat(PasswordUtil.checkPassword(raw, hash2)).isTrue();
    }

    @Test
    @DisplayName("Should reject null or empty password on hash")
    void shouldRejectInvalidInputOnHash() {
        assertThatThrownBy(() -> PasswordUtil.hashPassword(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PasswordUtil.hashPassword(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should return false safely when null arguments passed to checkPassword")
    void shouldHandleNullChecksSafely() {
        assertThat(PasswordUtil.checkPassword(null, "somehash")).isFalse();
        assertThat(PasswordUtil.checkPassword("somepass", null)).isFalse();
        assertThat(PasswordUtil.checkPassword(null, null)).isFalse();
    }
}
