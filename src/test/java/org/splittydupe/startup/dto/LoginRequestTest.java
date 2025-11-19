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

@DisplayName("LoginRequest DTO Tests")
class LoginRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create LoginRequest with builder")
    void shouldCreateLoginRequestWithBuilder() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        assertEquals("test@example.com", request.getEmail());
        assertEquals("password123", request.getPassword());
    }

    @Test
    @DisplayName("Should create LoginRequest with all-args constructor")
    void shouldCreateLoginRequestWithAllArgsConstructor() {
        LoginRequest request = new LoginRequest("user@example.com", "mypassword");

        assertEquals("user@example.com", request.getEmail());
        assertEquals("mypassword", request.getPassword());
    }

    @Test
    @DisplayName("Should create LoginRequest with no-args constructor")
    void shouldCreateLoginRequestWithNoArgsConstructor() {
        LoginRequest request = new LoginRequest();
        request.setEmail("another@example.com");
        request.setPassword("pass456");

        assertEquals("another@example.com", request.getEmail());
        assertEquals("pass456", request.getPassword());
    }

    @Test
    @DisplayName("Should validate valid email and password")
    void shouldValidateValidEmailAndPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("valid@example.com")
                .password("validpassword")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should reject blank email")
    void shouldRejectBlankEmail() {
        LoginRequest request = LoginRequest.builder()
                .email("")
                .password("password123")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Email is required")));
    }

    @Test
    @DisplayName("Should reject invalid email format")
    void shouldRejectInvalidEmailFormat() {
        LoginRequest request = LoginRequest.builder()
                .email("not-an-email")
                .password("password123")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Email must be valid")));
    }

    @Test
    @DisplayName("Should reject blank password")
    void shouldRejectBlankPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("")
                .build();

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Password is required")));
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        LoginRequest request1 = LoginRequest.builder()
                .email("test@example.com")
                .password("password")
                .build();

        LoginRequest request2 = LoginRequest.builder()
                .email("test@example.com")
                .password("password")
                .build();

        LoginRequest request3 = LoginRequest.builder()
                .email("other@example.com")
                .password("different")
                .build();

        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password")
                .build();

        String toString = request.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("test@example.com"));
    }
}
