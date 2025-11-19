package org.splittydupe.startup.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SaveReceiptResponse DTO Tests")
class SaveReceiptResponseTest {

    @Test
    @DisplayName("Should create SaveReceiptResponse with all-args constructor")
    void shouldCreateSaveReceiptResponseWithAllArgsConstructor() {
        SaveReceiptResponse response = new SaveReceiptResponse(true);

        assertTrue(response.isSaved());
    }

    @Test
    @DisplayName("Should create SaveReceiptResponse with no-args constructor")
    void shouldCreateSaveReceiptResponseWithNoArgsConstructor() {
        SaveReceiptResponse response = new SaveReceiptResponse();
        response.setSaved(false);

        assertFalse(response.isSaved());
    }

    @Test
    @DisplayName("Should create response indicating successful save")
    void shouldCreateResponseIndicatingSuccessfulSave() {
        SaveReceiptResponse response = new SaveReceiptResponse(true);

        assertTrue(response.isSaved());
    }

    @Test
    @DisplayName("Should create response indicating failed save")
    void shouldCreateResponseIndicatingFailedSave() {
        SaveReceiptResponse response = new SaveReceiptResponse(false);

        assertFalse(response.isSaved());
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        SaveReceiptResponse response1 = new SaveReceiptResponse(true);
        SaveReceiptResponse response2 = new SaveReceiptResponse(true);
        SaveReceiptResponse response3 = new SaveReceiptResponse(false);

        assertEquals(response1, response2);
        assertNotEquals(response1, response3);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        SaveReceiptResponse response = new SaveReceiptResponse(true);

        String toString = response.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("true") || toString.contains("saved"));
    }

    @Test
    @DisplayName("Should allow modifying saved field")
    void shouldAllowModifyingSavedField() {
        SaveReceiptResponse response = new SaveReceiptResponse(false);

        response.setSaved(true);

        assertTrue(response.isSaved());
    }
}
