package com.dp.ai_code_agent.user.local.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {

    @Test
    void hashAndMatches() {
        PasswordHasher h = new PasswordHasher();
        String hash = h.hash("secret");
        assertThat(hash).isNotEqualTo("secret");
        assertThat(h.matches("secret", hash)).isTrue();
        assertThat(h.matches("wrong", hash)).isFalse();
    }

    @Test
    void hashIsSaltAware() {
        PasswordHasher h = new PasswordHasher();
        assertThat(h.hash("secret")).isNotEqualTo(h.hash("secret"));
    }
}
