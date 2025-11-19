package org.splittydupe.startup.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorResponse DTO Tests")
class ErrorResponseTest {

    @Test
    @DisplayName("Should create ErrorResponse with builder")
    void shouldCreateErrorResponseWithBuilder() {
        ErrorResponse response = ErrorResponse.builder()
                .error("Invalid request")
                .message("The request parameters are invalid")
                .build();

        assertEquals("Invalid request", response.getError());
        assertEquals("The request parameters are invalid", response.getMessage());
    }

    @Test
    @DisplayName("Should create ErrorResponse with all-args constructor")
    void shouldCreateErrorResponseWithAllArgsConstructor() {
        ErrorResponse response = new ErrorResponse("Not found", "Resource not found");

        assertEquals("Not found", response.getError());
        assertEquals("Resource not found", response.getMessage());
    }

    @Test
    @DisplayName("Should create ErrorResponse with legacy constructor")
    void shouldCreateErrorResponseWithLegacyConstructor() {
        ErrorResponse response = new ErrorResponse("An error occurred");

        assertEquals("An error occurred", response.getError());
        assertEquals("An error occurred", response.getMessage());
    }

    @Test
    @DisplayName("Should create ErrorResponse with no-args constructor")
    void shouldCreateErrorResponseWithNoArgsConstructor() {
        ErrorResponse response = new ErrorResponse();
        response.setError("Server error");
        response.setMessage("Internal server error occurred");

        assertEquals("Server error", response.getError());
        assertEquals("Internal server error occurred", response.getMessage());
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        ErrorResponse response1 = ErrorResponse.builder()
                .error("Error")
                .message("Message")
                .build();

        ErrorResponse response2 = ErrorResponse.builder()
                .error("Error")
                .message("Message")
                .build();

        ErrorResponse response3 = ErrorResponse.builder()
                .error("Different")
                .message("Different")
                .build();

        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        ErrorResponse response = ErrorResponse.builder()
                .error("Test error")
                .message("Test message")
                .build();

        String toString = response.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("Test error"));
        assertTrue(toString.contains("Test message"));
    }
}
