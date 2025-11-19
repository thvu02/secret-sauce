package org.splittydupe.startup.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.splittydupe.startup.TestConfig;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.repository.IReceiptRepository;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Receipt Service Tests")
class ReceiptServiceTest {

    @Mock
    private IReceiptRepository receiptRepository;

    @InjectMocks
    private ReceiptService receiptService;

    private Receipt testReceipt;

    @BeforeEach
    void setUp() {
        testReceipt = TestConfig.createTestReceipt();
    }

    @Test
    @DisplayName("Should save receipt with user ID")
    void shouldSaveReceiptWithUserId() {
        String userId = TestConfig.TEST_USER_ID;
        when(receiptRepository.save(any(Receipt.class))).thenReturn(true);

        boolean result = receiptService.saveReceipt(testReceipt, userId);

        assertTrue(result, "Should return true when save succeeds");
        assertEquals(userId, testReceipt.getUserId(), "User ID should be set");
        assertNotNull(testReceipt.getUid(), "UID should be set");
        assertNotNull(testReceipt.getCreatedAt(), "Created timestamp should be set");
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    @DisplayName("Should generate UID if not provided")
    void shouldGenerateUidIfNotProvided() {
        testReceipt.setUid(null);
        when(receiptRepository.save(any(Receipt.class))).thenReturn(true);

        receiptService.saveReceipt(testReceipt, "user123");

        assertNotNull(testReceipt.getUid(), "UID should be generated");
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    @DisplayName("Should set userId to anonymous if not provided")
    void shouldSetUserIdToAnonymousIfNotProvided() {
        when(receiptRepository.save(any(Receipt.class))).thenReturn(true);

        receiptService.saveReceipt(testReceipt, null);

        assertEquals("anonymous", testReceipt.getUserId(), "Should set userId to anonymous");
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    @DisplayName("Should set expiration for anonymous receipts")
    void shouldSetExpirationForAnonymousReceipts() {
        when(receiptRepository.save(any(Receipt.class))).thenReturn(true);

        receiptService.saveReceipt(testReceipt, null);

        assertEquals("anonymous", testReceipt.getUserId());
        assertNotNull(testReceipt.getExpiresAt(), "Expiration should be set for anonymous receipts");
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    @DisplayName("Should not set expiration for authenticated users")
    void shouldNotSetExpirationForAuthenticatedUsers() {
        when(receiptRepository.save(any(Receipt.class))).thenReturn(true);

        receiptService.saveReceipt(testReceipt, "user123");

        assertNull(testReceipt.getExpiresAt(), "Expiration should not be set for authenticated users");
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    @DisplayName("Should initialize payment status for line items")
    void shouldInitializePaymentStatusForLineItems() {
        when(receiptRepository.save(any(Receipt.class))).thenReturn(true);

        receiptService.saveReceipt(testReceipt, "user123");

        testReceipt.getLineItems().forEach(lineItem -> {
            assertNotNull(lineItem.getAssigneePaymentStatus(), "Payment status should be initialized");
            lineItem.getAssignees().forEach(assignee -> {
                assertEquals("unpaid", lineItem.getAssigneePaymentStatus().get(assignee),
                        "Default status should be unpaid");
            });
        });
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    @DisplayName("Should initialize receipt-level payment status")
    void shouldInitializeReceiptLevelPaymentStatus() {
        when(receiptRepository.save(any(Receipt.class))).thenReturn(true);

        receiptService.saveReceipt(testReceipt, "user123");

        assertNotNull(testReceipt.getAssigneePaymentStatus(), "Receipt-level payment status should be initialized");
        assertNotNull(testReceipt.getPaymentStatus(), "Overall payment status should be set");
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    @DisplayName("Should retrieve receipt by UID")
    void shouldRetrieveReceiptByUid() {
        String uid = testReceipt.getUid();
        when(receiptRepository.findById(uid)).thenReturn(testReceipt);

        Receipt result = receiptService.getReceipt(uid);

        assertNotNull(result, "Receipt should be retrieved");
        assertEquals(uid, result.getUid(), "Retrieved receipt should have correct UID");
        verify(receiptRepository, times(1)).findById(uid);
    }

    @Test
    @DisplayName("Should retrieve user receipts")
    void shouldRetrieveUserReceipts() {
        String userId = TestConfig.TEST_USER_ID;
        List<Receipt> receipts = Arrays.asList(testReceipt, TestConfig.createMinimalReceipt());
        when(receiptRepository.findByUserId(userId)).thenReturn(receipts);

        List<Receipt> result = receiptService.getUserReceipts(userId);

        assertNotNull(result, "Should return list of receipts");
        assertEquals(2, result.size(), "Should return correct number of receipts");
        verify(receiptRepository, times(1)).findByUserId(userId);
    }

    @Test
    @DisplayName("Should delete receipt")
    void shouldDeleteReceipt() {
        String receiptId = testReceipt.getUid();
        when(receiptRepository.delete(receiptId)).thenReturn(true);

        boolean result = receiptService.deleteReceipt(receiptId);

        assertTrue(result, "Delete should succeed");
        verify(receiptRepository, times(1)).delete(receiptId);
    }

    @Test
    @DisplayName("Should update assignee payment status")
    void shouldUpdateAssigneePaymentStatus() {
        String receiptId = testReceipt.getUid();
        String assignee = "Alice";
        String status = "paid";
        when(receiptRepository.findById(receiptId)).thenReturn(testReceipt);
        when(receiptRepository.save(any(Receipt.class))).thenReturn(true);

        boolean result = receiptService.updateAssigneePaymentStatus(receiptId, assignee, status);

        assertTrue(result, "Update should succeed");
        assertEquals(status, testReceipt.getAssigneePaymentStatus().get(assignee),
                "Receipt-level status should be updated");
        verify(receiptRepository, times(1)).findById(receiptId);
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    @DisplayName("Should update payment status in all line items for assignee")
    void shouldUpdatePaymentStatusInAllLineItems() {
        String receiptId = testReceipt.getUid();
        String assignee = "Alice";
        String status = "paid";
        when(receiptRepository.findById(receiptId)).thenReturn(testReceipt);
        when(receiptRepository.save(any(Receipt.class))).thenReturn(true);

        receiptService.updateAssigneePaymentStatus(receiptId, assignee, status);

        testReceipt.getLineItems().stream()
                .filter(item -> item.getAssignees().contains(assignee))
                .forEach(item -> {
                    assertEquals(status, item.getAssigneePaymentStatus().get(assignee),
                            "Line item status should be updated");
                });
        verify(receiptRepository, times(1)).save(testReceipt);
    }

    @Test
    @DisplayName("Should return false when receipt not found for update")
    void shouldReturnFalseWhenReceiptNotFoundForUpdate() {
        String receiptId = "non-existent";
        when(receiptRepository.findById(receiptId)).thenReturn(null);

        boolean result = receiptService.updateAssigneePaymentStatus(receiptId, "Alice", "paid");

        assertFalse(result, "Should return false when receipt not found");
        verify(receiptRepository, times(1)).findById(receiptId);
        verify(receiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should use legacy saveReceipt method without userId")
    void shouldUseLegacySaveReceiptMethod() {
        when(receiptRepository.save(any(Receipt.class))).thenReturn(true);

        boolean result = receiptService.saveReceipt(testReceipt);

        assertTrue(result, "Legacy method should work");
        assertEquals("anonymous", testReceipt.getUserId(), "Should default to anonymous");
        verify(receiptRepository, times(1)).save(testReceipt);
    }
}
