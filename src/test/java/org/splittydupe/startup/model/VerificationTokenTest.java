package org.splittydupe.startup.model;

import com.google.cloud.Timestamp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VerificationToken Model Tests")
class VerificationTokenTest {

    @Test
    @DisplayName("Should create VerificationToken with builder")
    void shouldCreateVerificationTokenWithBuilder() {
        Timestamp now = Timestamp.now();
        Timestamp expiry = Timestamp.ofTimeSecondsAndNanos(
                now.getSeconds() + 86400, // 24 hours
                now.getNanos()
        );

        VerificationToken token = VerificationToken.builder()
                .uid("token-123")
                .token("abc-123-xyz")
                .userEmail("test@example.com")
                .userId("user-456")
                .tokenType("email_verification")
                .expiryDate(expiry)
                .used(false)
                .createdAt(now)
                .build();

        assertEquals("token-123", token.getUid());
        assertEquals("abc-123-xyz", token.getToken());
        assertEquals("test@example.com", token.getUserEmail());
        assertEquals("user-456", token.getUserId());
        assertEquals("email_verification", token.getTokenType());
        assertNotNull(token.getExpiryDate());
        assertFalse(token.isUsed());
        assertNotNull(token.getCreatedAt());
    }

    @Test
    @DisplayName("Should create VerificationToken with default values")
    void shouldCreateVerificationTokenWithDefaults() {
        VerificationToken token = VerificationToken.builder()
                .uid("token-456")
                .token("def-456-uvw")
                .userEmail("user@example.com")
                .userId("user-789")
                .build();

        assertFalse(token.isUsed());
        assertEquals("email_verification", token.getTokenType());
    }

    @Test
    @DisplayName("Should create VerificationToken with no-args constructor")
    void shouldCreateVerificationTokenWithNoArgsConstructor() {
        VerificationToken token = new VerificationToken();
        token.setUid("token-789");
        token.setToken("ghi-789-rst");
        token.setUserEmail("another@example.com");
        token.setUserId("user-111");

        assertEquals("token-789", token.getUid());
        assertEquals("ghi-789-rst", token.getToken());
        assertEquals("another@example.com", token.getUserEmail());
        assertEquals("user-111", token.getUserId());
    }

    @Test
    @DisplayName("Should handle email verification token type")
    void shouldHandleEmailVerificationTokenType() {
        VerificationToken token = VerificationToken.builder()
                .uid("token-email")
                .token("email-token-123")
                .userEmail("verify@example.com")
                .userId("user-verify")
                .tokenType("email_verification")
                .build();

        assertEquals("email_verification", token.getTokenType());
    }

    @Test
    @DisplayName("Should handle password reset token type")
    void shouldHandlePasswordResetTokenType() {
        VerificationToken token = VerificationToken.builder()
                .uid("token-reset")
                .token("reset-token-456")
                .userEmail("reset@example.com")
                .userId("user-reset")
                .tokenType("password_reset")
                .build();

        assertEquals("password_reset", token.getTokenType());
    }

    @Test
    @DisplayName("Should handle used token")
    void shouldHandleUsedToken() {
        VerificationToken token = VerificationToken.builder()
                .uid("token-used")
                .token("used-token-789")
                .userEmail("used@example.com")
                .userId("user-used")
                .used(true)
                .build();

        assertTrue(token.isUsed());
    }

    @Test
    @DisplayName("Should handle expired token")
    void shouldHandleExpiredToken() {
        Timestamp now = Timestamp.now();
        Timestamp pastExpiry = Timestamp.ofTimeSecondsAndNanos(
                now.getSeconds() - 3600, // 1 hour ago
                now.getNanos()
        );

        VerificationToken token = VerificationToken.builder()
                .uid("token-expired")
                .token("expired-token-123")
                .userEmail("expired@example.com")
                .userId("user-expired")
                .expiryDate(pastExpiry)
                .createdAt(Timestamp.ofTimeSecondsAndNanos(
                        now.getSeconds() - 7200, // 2 hours ago
                        now.getNanos()
                ))
                .build();

        assertTrue(token.getExpiryDate().compareTo(Timestamp.now()) < 0);
    }

    @Test
    @DisplayName("Should handle valid unexpired token")
    void shouldHandleValidUnexpiredToken() {
        Timestamp now = Timestamp.now();
        Timestamp futureExpiry = Timestamp.ofTimeSecondsAndNanos(
                now.getSeconds() + 86400, // 24 hours from now
                now.getNanos()
        );

        VerificationToken token = VerificationToken.builder()
                .uid("token-valid")
                .token("valid-token-123")
                .userEmail("valid@example.com")
                .userId("user-valid")
                .expiryDate(futureExpiry)
                .used(false)
                .createdAt(now)
                .build();

        assertTrue(token.getExpiryDate().compareTo(Timestamp.now()) > 0);
        assertFalse(token.isUsed());
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        Timestamp now = Timestamp.now();

        VerificationToken token1 = VerificationToken.builder()
                .uid("token-1")
                .token("token-abc")
                .userEmail("test@example.com")
                .userId("user-1")
                .createdAt(now)
                .build();

        VerificationToken token2 = VerificationToken.builder()
                .uid("token-1")
                .token("token-abc")
                .userEmail("test@example.com")
                .userId("user-1")
                .createdAt(now)
                .build();

        VerificationToken token3 = VerificationToken.builder()
                .uid("token-2")
                .token("token-xyz")
                .userEmail("other@example.com")
                .userId("user-2")
                .createdAt(now)
                .build();

        assertEquals(token1, token2);
        assertNotEquals(token1, token3);
        assertEquals(token1.hashCode(), token2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        VerificationToken token = VerificationToken.builder()
                .uid("token-1")
                .token("token-abc")
                .userEmail("test@example.com")
                .build();

        String toString = token.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("token-1"));
        assertTrue(toString.contains("test@example.com"));
    }
}
