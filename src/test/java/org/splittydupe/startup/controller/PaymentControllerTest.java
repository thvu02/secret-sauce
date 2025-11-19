package org.splittydupe.startup.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.splittydupe.startup.dto.ErrorResponse;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.service.JwtService;
import org.splittydupe.startup.service.ReceiptService;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController Tests")
class PaymentControllerTest {

    @Mock
    private ReceiptService receiptService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private PaymentController paymentController;

    private Receipt testReceipt;
    private PaymentController.PaymentStatusRequest request;

    @BeforeEach
    void setUp() {
        testReceipt = Receipt.builder()
                .uid("receipt-123")
                .userId("user@example.com")
                .vendor("Test Restaurant")
                .total(100.00)
                .build();

        request = new PaymentController.PaymentStatusRequest();
        request.setStatus("paid");
    }

    @Test
    @DisplayName("Should update assignee payment status successfully")
    void shouldUpdateAssigneePaymentStatusSuccessfully() {
        String receiptId = "receipt-123";
        String assignee = "Alice";
        String authHeader = "Bearer valid-token";

        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(receiptService.getReceipt(receiptId)).thenReturn(testReceipt);
        when(receiptService.updateAssigneePaymentStatus(receiptId, assignee, "paid")).thenReturn(true);
        when(receiptService.getReceipt(receiptId)).thenReturn(testReceipt);

        ResponseEntity<?> response = paymentController.updateAssigneePaymentStatus(
                receiptId, assignee, request, authHeader);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof Receipt);
        verify(receiptService, times(1)).updateAssigneePaymentStatus(receiptId, assignee, "paid");
    }

    @Test
    @DisplayName("Should allow anonymous user to update payment status")
    void shouldAllowAnonymousUserToUpdatePaymentStatus() {
        String receiptId = "receipt-123";
        String assignee = "Alice";

        Receipt anonymousReceipt = Receipt.builder()
                .uid(receiptId)
                .userId("anonymous")
                .vendor("Test Restaurant")
                .total(100.00)
                .build();

        when(receiptService.updateAssigneePaymentStatus(receiptId, assignee, "paid")).thenReturn(true);
        when(receiptService.getReceipt(receiptId)).thenReturn(anonymousReceipt);

        ResponseEntity<?> response = paymentController.updateAssigneePaymentStatus(
                receiptId, assignee, request, null);

        assertEquals(200, response.getStatusCodeValue());
        verify(receiptService, times(1)).updateAssigneePaymentStatus(receiptId, assignee, "paid");
    }

    @Test
    @DisplayName("Should reject invalid payment status")
    void shouldRejectInvalidPaymentStatus() {
        String receiptId = "receipt-123";
        String assignee = "Alice";
        PaymentController.PaymentStatusRequest invalidRequest = new PaymentController.PaymentStatusRequest();
        invalidRequest.setStatus("invalid");

        ResponseEntity<?> response = paymentController.updateAssigneePaymentStatus(
                receiptId, assignee, invalidRequest, null);

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("Invalid payment status", error.getError());
        verify(receiptService, never()).updateAssigneePaymentStatus(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should accept 'paid' status")
    void shouldAcceptPaidStatus() {
        String receiptId = "receipt-123";
        String assignee = "Alice";

        when(receiptService.updateAssigneePaymentStatus(receiptId, assignee, "paid")).thenReturn(true);
        when(receiptService.getReceipt(receiptId)).thenReturn(testReceipt);

        ResponseEntity<?> response = paymentController.updateAssigneePaymentStatus(
                receiptId, assignee, request, null);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    @DisplayName("Should accept 'unpaid' status")
    void shouldAcceptUnpaidStatus() {
        String receiptId = "receipt-123";
        String assignee = "Alice";
        PaymentController.PaymentStatusRequest unpaidRequest = new PaymentController.PaymentStatusRequest();
        unpaidRequest.setStatus("unpaid");

        when(receiptService.updateAssigneePaymentStatus(receiptId, assignee, "unpaid")).thenReturn(true);
        when(receiptService.getReceipt(receiptId)).thenReturn(testReceipt);

        ResponseEntity<?> response = paymentController.updateAssigneePaymentStatus(
                receiptId, assignee, unpaidRequest, null);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    @DisplayName("Should return 404 when receipt not found")
    void shouldReturn404WhenReceiptNotFound() {
        String receiptId = "nonexistent";
        String assignee = "Alice";

        when(receiptService.updateAssigneePaymentStatus(receiptId, assignee, "paid")).thenReturn(false);

        ResponseEntity<?> response = paymentController.updateAssigneePaymentStatus(
                receiptId, assignee, request, null);

        assertEquals(404, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("Update failed", error.getError());
    }

    @Test
    @DisplayName("Should return 403 when user does not own receipt")
    void shouldReturn403WhenUserDoesNotOwnReceipt() {
        String receiptId = "receipt-123";
        String assignee = "Alice";
        String authHeader = "Bearer valid-token";

        Receipt otherUserReceipt = Receipt.builder()
                .uid(receiptId)
                .userId("other@example.com")
                .vendor("Test Restaurant")
                .total(100.00)
                .build();

        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(receiptService.getReceipt(receiptId)).thenReturn(otherUserReceipt);

        ResponseEntity<?> response = paymentController.updateAssigneePaymentStatus(
                receiptId, assignee, request, authHeader);

        assertEquals(403, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("Access denied", error.getError());
        verify(receiptService, never()).updateAssigneePaymentStatus(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should allow user to update their own receipt")
    void shouldAllowUserToUpdateTheirOwnReceipt() {
        String receiptId = "receipt-123";
        String assignee = "Alice";
        String authHeader = "Bearer valid-token";

        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(receiptService.getReceipt(receiptId)).thenReturn(testReceipt);
        when(receiptService.updateAssigneePaymentStatus(receiptId, assignee, "paid")).thenReturn(true);

        ResponseEntity<?> response = paymentController.updateAssigneePaymentStatus(
                receiptId, assignee, request, authHeader);

        assertEquals(200, response.getStatusCodeValue());
        verify(receiptService, times(1)).updateAssigneePaymentStatus(receiptId, assignee, "paid");
    }

    @Test
    @DisplayName("Should handle exception during update")
    void shouldHandleExceptionDuringUpdate() {
        String receiptId = "receipt-123";
        String assignee = "Alice";

        when(receiptService.updateAssigneePaymentStatus(receiptId, assignee, "paid"))
                .thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = paymentController.updateAssigneePaymentStatus(
                receiptId, assignee, request, null);

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof ErrorResponse);
        ErrorResponse error = (ErrorResponse) response.getBody();
        assertEquals("Update failed", error.getError());
    }

    @Test
    @DisplayName("Should extract token from Bearer header")
    void shouldExtractTokenFromBearerHeader() {
        String receiptId = "receipt-123";
        String assignee = "Alice";
        String authHeader = "Bearer token-with-bearer-prefix";

        when(jwtService.extractUsername("token-with-bearer-prefix")).thenReturn("user@example.com");
        when(receiptService.getReceipt(receiptId)).thenReturn(testReceipt);
        when(receiptService.updateAssigneePaymentStatus(receiptId, assignee, "paid")).thenReturn(true);

        ResponseEntity<?> response = paymentController.updateAssigneePaymentStatus(
                receiptId, assignee, request, authHeader);

        assertEquals(200, response.getStatusCodeValue());
        verify(jwtService, times(1)).extractUsername("token-with-bearer-prefix");
    }

    @Test
    @DisplayName("Should not check ownership for anonymous receipts")
    void shouldNotCheckOwnershipForAnonymousReceipts() {
        String receiptId = "receipt-123";
        String assignee = "Alice";
        String authHeader = "Bearer valid-token";

        Receipt anonymousReceipt = Receipt.builder()
                .uid(receiptId)
                .userId("anonymous")
                .vendor("Test Restaurant")
                .total(100.00)
                .build();

        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(receiptService.getReceipt(receiptId)).thenReturn(anonymousReceipt);
        when(receiptService.updateAssigneePaymentStatus(receiptId, assignee, "paid")).thenReturn(true);

        ResponseEntity<?> response = paymentController.updateAssigneePaymentStatus(
                receiptId, assignee, request, authHeader);

        assertEquals(200, response.getStatusCodeValue());
    }
}
