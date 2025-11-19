package org.splittydupe.startup.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordResetRequest DTO Tests")
class PasswordResetRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create PasswordResetRequest with builder")
    void shouldCreatePasswordResetRequestWithBuilder() {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .token("reset-token-123")
                .newPassword("newpassword123")
                .build();

        assertEquals("reset-token-123", request.getToken());
        assertEquals("newpassword123", request.getNewPassword());
    }

    @Test
    @DisplayName("Should create PasswordResetRequest with all-args constructor")
    void shouldCreatePasswordResetRequestWithAllArgsConstructor() {
        PasswordResetRequest request = new PasswordResetRequest("token-abc", "password456");

        assertEquals("token-abc", request.getToken());
        assertEquals("password456", request.getNewPassword());
    }

    @Test
    @DisplayName("Should create PasswordResetRequest with no-args constructor")
    void shouldCreatePasswordResetRequestWithNoArgsConstructor() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setToken("token-xyz");
        request.setNewPassword("newpass789");

        assertEquals("token-xyz", request.getToken());
        assertEquals("newpass789", request.getNewPassword());
    }

    @Test
    @DisplayName("Should validate valid token and password")
    void shouldValidateValidTokenAndPassword() {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .token("valid-token-123")
                .newPassword("validpassword123")
                .build();

        Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should reject blank token")
    void shouldRejectBlankToken() {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .token("")
                .newPassword("password123")
                .build();

        Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Token is required")));
    }

    @Test
    @DisplayName("Should reject blank new password")
    void shouldRejectBlankNewPassword() {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .token("token-123")
                .newPassword("")
                .build();

        Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("New password is required")));
    }

    @Test
    @DisplayName("Should reject password shorter than 8 characters")
    void shouldRejectShortPassword() {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .token("token-123")
                .newPassword("short")
                .build();

        Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Password must be at least 8 characters long")));
    }

    @Test
    @DisplayName("Should accept password with exactly 8 characters")
    void shouldAcceptPasswordWith8Characters() {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .token("token-123")
                .newPassword("password")
                .build();

        Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should reject null token")
    void shouldRejectNullToken() {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .token(null)
                .newPassword("password123")
                .build();

        Set<ConstraintViolation<PasswordResetRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        PasswordResetRequest request1 = PasswordResetRequest.builder()
                .token("token-123")
                .newPassword("password123")
                .build();

        PasswordResetRequest request2 = PasswordResetRequest.builder()
                .token("token-123")
                .newPassword("password123")
                .build();

        PasswordResetRequest request3 = PasswordResetRequest.builder()
                .token("different-token")
                .newPassword("different123")
                .build();

        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .token("token-123")
                .newPassword("password123")
                .build();

        String toString = request.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("token-123"));
    }
}
