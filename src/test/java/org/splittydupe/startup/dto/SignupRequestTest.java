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

@DisplayName("SignupRequest DTO Tests")
class SignupRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should create SignupRequest with builder")
    void shouldCreateSignupRequestWithBuilder() {
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        assertEquals("test@example.com", request.getEmail());
        assertEquals("password123", request.getPassword());
    }

    @Test
    @DisplayName("Should create SignupRequest with all-args constructor")
    void shouldCreateSignupRequestWithAllArgsConstructor() {
        SignupRequest request = new SignupRequest("user@example.com", "mypassword");

        assertEquals("user@example.com", request.getEmail());
        assertEquals("mypassword", request.getPassword());
    }

    @Test
    @DisplayName("Should create SignupRequest with no-args constructor")
    void shouldCreateSignupRequestWithNoArgsConstructor() {
        SignupRequest request = new SignupRequest();
        request.setEmail("another@example.com");
        request.setPassword("pass456789");

        assertEquals("another@example.com", request.getEmail());
        assertEquals("pass456789", request.getPassword());
    }

    @Test
    @DisplayName("Should validate valid email and password")
    void shouldValidateValidEmailAndPassword() {
        SignupRequest request = SignupRequest.builder()
                .email("valid@example.com")
                .password("validpassword123")
                .build();

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should reject blank email")
    void shouldRejectBlankEmail() {
        SignupRequest request = SignupRequest.builder()
                .email("")
                .password("password123")
                .build();

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Email is required")));
    }

    @Test
    @DisplayName("Should reject invalid email format")
    void shouldRejectInvalidEmailFormat() {
        SignupRequest request = SignupRequest.builder()
                .email("not-an-email")
                .password("password123")
                .build();

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Email must be valid")));
    }

    @Test
    @DisplayName("Should reject blank password")
    void shouldRejectBlankPassword() {
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("")
                .build();

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Password is required")));
    }

    @Test
    @DisplayName("Should reject password shorter than 8 characters")
    void shouldRejectShortPassword() {
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("short")
                .build();

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("Password must be at least 8 characters long")));
    }

    @Test
    @DisplayName("Should accept password with exactly 8 characters")
    void shouldAcceptPasswordWith8Characters() {
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("password")
                .build();

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        SignupRequest request1 = SignupRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        SignupRequest request2 = SignupRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        SignupRequest request3 = SignupRequest.builder()
                .email("other@example.com")
                .password("different123")
                .build();

        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        SignupRequest request = SignupRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        String toString = request.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("test@example.com"));
    }
}
