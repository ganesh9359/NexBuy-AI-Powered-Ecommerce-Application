package com.nexbuy.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("test-secret-key-must-be-at-least-32-chars!!");
    }

    @Test
    @DisplayName("Generated token extracts correct username")
    void generateToken_extractUsername() {
        String token = jwtUtil.generateToken("user@nexbuy.com", 3600_000);
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("user@nexbuy.com");
    }

    @Test
    @DisplayName("Valid token is not expired")
    void generateToken_notExpired() {
        String token = jwtUtil.generateToken("user@nexbuy.com", 3600_000);
        assertThat(jwtUtil.isExpired(token)).isFalse();
    }

    @Test
    @DisplayName("Expired token throws ExpiredJwtException")
    void generateToken_expired() throws InterruptedException {
        String token = jwtUtil.generateToken("user@nexbuy.com", 1);
        Thread.sleep(1500);
        // isExpired() internally calls getClaims() which throws ExpiredJwtException
        assertThatThrownBy(() -> jwtUtil.isExpired(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Different users produce different tokens")
    void generateToken_uniquePerUser() {
        String t1 = jwtUtil.generateToken("user1@nexbuy.com", 3600_000);
        String t2 = jwtUtil.generateToken("user2@nexbuy.com", 3600_000);
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("Invalid token throws exception")
    void extractUsername_invalidToken() {
        assertThatThrownBy(() -> jwtUtil.extractUsername("not.a.valid.token"))
                .isInstanceOf(Exception.class);
    }
}
