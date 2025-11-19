package org.splittydupe.startup.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.splittydupe.startup.TestConfig;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JWT Service Tests")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TestConfig.TEST_JWT_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", TestConfig.TEST_JWT_EXPIRATION);
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidJwtToken() {
        String userEmail = TestConfig.TEST_USER_EMAIL;

        String token = jwtService.generateToken(userEmail);

        assertNotNull(token, "Generated token should not be null");
        assertTrue(token.length() > 0, "Token should not be empty");
        assertTrue(token.split("\\.").length == 3, "JWT should have 3 parts");
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void shouldExtractUsernameFromValidToken() {
        String userEmail = TestConfig.TEST_USER_EMAIL;
        String token = jwtService.generateToken(userEmail);

        String extractedEmail = jwtService.extractUsername(token);

        assertEquals(userEmail, extractedEmail, "Extracted email should match original");
    }

    @Test
    @DisplayName("Should validate token for correct user")
    void shouldValidateTokenForCorrectUser() {
        String userEmail = TestConfig.TEST_USER_EMAIL;
        String token = jwtService.generateToken(userEmail);

        boolean isValid = jwtService.isTokenValid(token, userEmail);

        assertTrue(isValid, "Token should be valid for correct user");
    }

    @Test
    @DisplayName("Should reject token for wrong user")
    void shouldRejectTokenForWrongUser() {
        String userEmail = TestConfig.TEST_USER_EMAIL;
        String token = jwtService.generateToken(userEmail);
        String wrongEmail = "wrong@example.com";

        boolean isValid = jwtService.isTokenValid(token, wrongEmail);

        assertFalse(isValid, "Token should be invalid for wrong user");
    }

    @Test
    @DisplayName("Should reject malformed token")
    void shouldRejectMalformedToken() {
        String malformedToken = "invalid.token.format";
        String userEmail = TestConfig.TEST_USER_EMAIL;

        assertThrows(MalformedJwtException.class, () -> {
            jwtService.isTokenValid(malformedToken, userEmail);
        }, "Should throw MalformedJwtException for invalid token format");
    }

    @Test
    @DisplayName("Should reject null token")
    void shouldRejectNullToken() {
        String userEmail = TestConfig.TEST_USER_EMAIL;

        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.isTokenValid(null, userEmail);
        }, "Should throw IllegalArgumentException for null token");
    }

    @Test
    @DisplayName("Should handle token with custom claims")
    void shouldHandleTokenWithCustomClaims() {
        String userEmail = TestConfig.TEST_USER_EMAIL;
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("role", "ADMIN");
        claims.put("userId", "123");

        String token = jwtService.generateToken(claims, userEmail);
        String extractedEmail = jwtService.extractUsername(token);

        assertNotNull(token, "Token with claims should be generated");
        assertEquals(userEmail, extractedEmail, "Email should be extractable");
    }

    @Test
    @DisplayName("Should detect expired token")
    void shouldDetectExpiredToken() {
        JwtService shortExpirationService = new JwtService();
        ReflectionTestUtils.setField(shortExpirationService, "secret", TestConfig.TEST_JWT_SECRET);
        ReflectionTestUtils.setField(shortExpirationService, "jwtExpiration", 1L); // 1ms

        String userEmail = TestConfig.TEST_USER_EMAIL;
        String token = shortExpirationService.generateToken(userEmail);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThrows(ExpiredJwtException.class, () -> {
            shortExpirationService.isTokenValid(token, userEmail);
        }, "Should throw ExpiredJwtException for expired token");
    }
}
