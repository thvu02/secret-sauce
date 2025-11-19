package org.splittydupe.startup.model;

import com.google.cloud.Timestamp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Model Tests")
class UserTest {

    @Test
    @DisplayName("Should create User with builder")
    void shouldCreateUserWithBuilder() {
        Timestamp now = Timestamp.now();

        User user = User.builder()
                .uid("user-123")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .emailVerified(true)
                .enabled(true)
                .roles(new ArrayList<>(Arrays.asList("ROLE_USER")))
                .createdAt(now)
                .lastLoginAt(now)
                .build();

        assertEquals("user-123", user.getUid());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("$2a$10$hashedpassword", user.getPasswordHash());
        assertTrue(user.isEmailVerified());
        assertTrue(user.isEnabled());
        assertEquals(1, user.getRoles().size());
        assertEquals("ROLE_USER", user.getRoles().get(0));
        assertNotNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("Should create User with default values")
    void shouldCreateUserWithDefaults() {
        User user = User.builder()
                .uid("user-456")
                .email("newuser@example.com")
                .passwordHash("hashed")
                .build();

        assertFalse(user.isEmailVerified());
        assertTrue(user.isEnabled());
        assertNotNull(user.getRoles());
        assertEquals(0, user.getRoles().size());
    }

    @Test
    @DisplayName("Should create User with no-args constructor")
    void shouldCreateUserWithNoArgsConstructor() {
        User user = new User();
        user.setUid("user-789");
        user.setEmail("another@example.com");
        user.setPasswordHash("hash123");

        assertEquals("user-789", user.getUid());
        assertEquals("another@example.com", user.getEmail());
        assertEquals("hash123", user.getPasswordHash());
    }

    @Test
    @DisplayName("Should handle multiple roles")
    void shouldHandleMultipleRoles() {
        User user = User.builder()
                .uid("admin-1")
                .email("admin@example.com")
                .passwordHash("hashed")
                .roles(new ArrayList<>(Arrays.asList("ROLE_USER", "ROLE_ADMIN")))
                .build();

        assertEquals(2, user.getRoles().size());
        assertTrue(user.getRoles().contains("ROLE_USER"));
        assertTrue(user.getRoles().contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Should handle password reset fields")
    void shouldHandlePasswordResetFields() {
        Timestamp resetExpiry = Timestamp.now();

        User user = User.builder()
                .uid("user-reset")
                .email("reset@example.com")
                .passwordHash("hashed")
                .resetToken("reset-token-123")
                .resetTokenExpiry(resetExpiry)
                .build();

        assertEquals("reset-token-123", user.getResetToken());
        assertNotNull(user.getResetTokenExpiry());
    }

    @Test
    @DisplayName("Should handle disabled user")
    void shouldHandleDisabledUser() {
        User user = User.builder()
                .uid("disabled-user")
                .email("disabled@example.com")
                .passwordHash("hashed")
                .enabled(false)
                .build();

        assertFalse(user.isEnabled());
    }

    @Test
    @DisplayName("Should handle unverified email")
    void shouldHandleUnverifiedEmail() {
        User user = User.builder()
                .uid("unverified-user")
                .email("unverified@example.com")
                .passwordHash("hashed")
                .emailVerified(false)
                .build();

        assertFalse(user.isEmailVerified());
    }

    @Test
    @DisplayName("Should track login timestamps")
    void shouldTrackLoginTimestamps() {
        Timestamp created = Timestamp.now();
        Timestamp lastLogin = Timestamp.ofTimeSecondsAndNanos(
                created.getSeconds() + 3600, // 1 hour later
                created.getNanos()
        );

        User user = User.builder()
                .uid("user-timestamps")
                .email("timestamps@example.com")
                .passwordHash("hashed")
                .createdAt(created)
                .lastLoginAt(lastLogin)
                .build();

        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getLastLoginAt());
        assertTrue(user.getLastLoginAt().getSeconds() > user.getCreatedAt().getSeconds());
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        User user1 = User.builder()
                .uid("user-1")
                .email("test@example.com")
                .passwordHash("hash")
                .build();

        User user2 = User.builder()
                .uid("user-1")
                .email("test@example.com")
                .passwordHash("hash")
                .build();

        User user3 = User.builder()
                .uid("user-2")
                .email("other@example.com")
                .passwordHash("hash")
                .build();

        assertEquals(user1, user2);
        assertNotEquals(user1, user3);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        User user = User.builder()
                .uid("user-1")
                .email("test@example.com")
                .build();

        String toString = user.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("user-1"));
        assertTrue(toString.contains("test@example.com"));
    }
}
