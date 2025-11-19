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

@DisplayName("ForgotPasswordRequest DTO Tests")
class ForgotPasswordRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create ForgotPasswordRequest with builder")
    void shouldCreateForgotPasswordRequestWithBuilder() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        assertEquals("test@example.com", request.getEmail());
    }

    @Test
    @DisplayName("Should create ForgotPasswordRequest with all-args constructor")
    void shouldCreateForgotPasswordRequestWithAllArgsConstructor() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@example.com");

        assertEquals("user@example.com", request.getEmail());
    }

    @Test
    @DisplayName("Should create ForgotPasswordRequest with no-args constructor")
    void shouldCreateForgotPasswordRequestWithNoArgsConstructor() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("another@example.com");

        assertEquals("another@example.com", request.getEmail());
    }

    @Test
    @DisplayName("Should validate valid email")
    void shouldValidateValidEmail() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("valid@example.com")
                .build();

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should reject blank email")
    void shouldRejectBlankEmail() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("")
                .build();

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Email is required")));
    }

    @Test
    @DisplayName("Should reject invalid email format")
    void shouldRejectInvalidEmailFormat() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("not-an-email")
                .build();

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Email must be valid")));
    }

    @Test
    @DisplayName("Should reject null email")
    void shouldRejectNullEmail() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email(null)
                .build();

        Set<ConstraintViolation<ForgotPasswordRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        ForgotPasswordRequest request1 = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        ForgotPasswordRequest request2 = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        ForgotPasswordRequest request3 = ForgotPasswordRequest.builder()
                .email("other@example.com")
                .build();

        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        String toString = request.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("test@example.com"));
    }
}
