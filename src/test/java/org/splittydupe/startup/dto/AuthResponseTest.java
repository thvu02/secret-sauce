package org.splittydupe.startup.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthResponse DTO Tests")
class AuthResponseTest {

    @Test
    @DisplayName("Should create AuthResponse with builder")
    void shouldCreateAuthResponseWithBuilder() {
        AuthResponse response = AuthResponse.builder()
                .token("jwt-token-123")
                .email("test@example.com")
                .userId("user-456")
                .emailVerified(true)
                .message("Login successful")
                .build();

        assertEquals("jwt-token-123", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("user-456", response.getUserId());
        assertTrue(response.isEmailVerified());
        assertEquals("Login successful", response.getMessage());
    }

    @Test
    @DisplayName("Should create AuthResponse with all-args constructor")
    void shouldCreateAuthResponseWithAllArgsConstructor() {
        AuthResponse response = new AuthResponse(
                "token-abc",
                "user@example.com",
                "user-789",
                false,
                "Account created"
        );

        assertEquals("token-abc", response.getToken());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("user-789", response.getUserId());
        assertFalse(response.isEmailVerified());
        assertEquals("Account created", response.getMessage());
    }

    @Test
    @DisplayName("Should create AuthResponse with no-args constructor")
    void shouldCreateAuthResponseWithNoArgsConstructor() {
        AuthResponse response = new AuthResponse();
        response.setToken("new-token");
        response.setEmail("new@example.com");
        response.setUserId("user-new");
        response.setEmailVerified(true);
        response.setMessage("Success");

        assertEquals("new-token", response.getToken());
        assertEquals("new@example.com", response.getEmail());
        assertEquals("user-new", response.getUserId());
        assertTrue(response.isEmailVerified());
        assertEquals("Success", response.getMessage());
    }

    @Test
    @DisplayName("Should handle unverified email status")
    void shouldHandleUnverifiedEmailStatus() {
        AuthResponse response = AuthResponse.builder()
                .token("token")
                .email("unverified@example.com")
                .userId("user-1")
                .emailVerified(false)
                .message("Please verify your email")
                .build();

        assertFalse(response.isEmailVerified());
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        AuthResponse response1 = AuthResponse.builder()
                .token("token")
                .email("test@example.com")
                .userId("user-1")
                .emailVerified(true)
                .message("Success")
                .build();

        AuthResponse response2 = AuthResponse.builder()
                .token("token")
                .email("test@example.com")
                .userId("user-1")
                .emailVerified(true)
                .message("Success")
                .build();

        AuthResponse response3 = AuthResponse.builder()
                .token("different")
                .email("other@example.com")
                .userId("user-2")
                .emailVerified(false)
                .message("Fail")
                .build();

        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        AuthResponse response = AuthResponse.builder()
                .token("token-123")
                .email("test@example.com")
                .userId("user-456")
                .build();

        String toString = response.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("test@example.com"));
        assertTrue(toString.contains("user-456"));
    }
}
