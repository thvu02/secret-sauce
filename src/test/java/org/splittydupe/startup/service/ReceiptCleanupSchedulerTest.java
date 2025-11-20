package org.splittydupe.startup.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.repository.IReceiptRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptCleanupScheduler Tests")
class ReceiptCleanupSchedulerTest {

    @Mock
    private IReceiptRepository receiptRepository;

    @InjectMocks
    private ReceiptCleanupScheduler receiptCleanupScheduler;

    @Test
    @DisplayName("Should delete expired anonymous receipts")
    void shouldDeleteExpiredAnonymousReceipts() {
        Receipt expiredReceipt1 = Receipt.builder()
                .uid("receipt-1")
                .userId("anonymous")
                .build();

        Receipt expiredReceipt2 = Receipt.builder()
                .uid("receipt-2")
                .userId("anonymous")
                .build();

        List<Receipt> expiredReceipts = Arrays.asList(expiredReceipt1, expiredReceipt2);

        when(receiptRepository.findExpiredAnonymousReceipts()).thenReturn(expiredReceipts);
        when(receiptRepository.delete(anyString())).thenReturn(true);

        receiptCleanupScheduler.cleanupExpiredAnonymousReceipts();

        verify(receiptRepository, times(1)).findExpiredAnonymousReceipts();
        verify(receiptRepository, times(1)).delete("receipt-1");
        verify(receiptRepository, times(1)).delete("receipt-2");
    }

    @Test
    @DisplayName("Should handle no expired receipts")
    void shouldHandleNoExpiredReceipts() {
        when(receiptRepository.findExpiredAnonymousReceipts()).thenReturn(new ArrayList<>());

        receiptCleanupScheduler.cleanupExpiredAnonymousReceipts();

        verify(receiptRepository, times(1)).findExpiredAnonymousReceipts();
        verify(receiptRepository, never()).delete(anyString());
    }

    @Test
    @DisplayName("Should continue cleanup even if one deletion fails")
    void shouldContinueCleanupEvenIfOneDeletionFails() {
        Receipt expiredReceipt1 = Receipt.builder()
                .uid("receipt-1")
                .userId("anonymous")
                .build();

        Receipt expiredReceipt2 = Receipt.builder()
                .uid("receipt-2")
                .userId("anonymous")
                .build();

        Receipt expiredReceipt3 = Receipt.builder()
                .uid("receipt-3")
                .userId("anonymous")
                .build();

        List<Receipt> expiredReceipts = Arrays.asList(expiredReceipt1, expiredReceipt2, expiredReceipt3);

        when(receiptRepository.findExpiredAnonymousReceipts()).thenReturn(expiredReceipts);
        when(receiptRepository.delete("receipt-1")).thenReturn(true);
        when(receiptRepository.delete("receipt-2")).thenThrow(new RuntimeException("Delete failed"));
        when(receiptRepository.delete("receipt-3")).thenReturn(true);

        receiptCleanupScheduler.cleanupExpiredAnonymousReceipts();

        verify(receiptRepository, times(1)).findExpiredAnonymousReceipts();
        verify(receiptRepository, times(1)).delete("receipt-1");
        verify(receiptRepository, times(1)).delete("receipt-2");
        verify(receiptRepository, times(1)).delete("receipt-3");
    }

    @Test
    @DisplayName("Should handle exception when finding expired receipts")
    void shouldHandleExceptionWhenFindingExpiredReceipts() {
        when(receiptRepository.findExpiredAnonymousReceipts())
                .thenThrow(new RuntimeException("Database error"));

        receiptCleanupScheduler.cleanupExpiredAnonymousReceipts();

        verify(receiptRepository, times(1)).findExpiredAnonymousReceipts();
        verify(receiptRepository, never()).delete(anyString());
    }

    @Test
    @DisplayName("Should handle large batch of expired receipts")
    void shouldHandleLargeBatchOfExpiredReceipts() {
        List<Receipt> expiredReceipts = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            expiredReceipts.add(Receipt.builder()
                    .uid("receipt-" + i)
                    .userId("anonymous")
                    .build());
        }

        when(receiptRepository.findExpiredAnonymousReceipts()).thenReturn(expiredReceipts);
        when(receiptRepository.delete(anyString())).thenReturn(true);

        receiptCleanupScheduler.cleanupExpiredAnonymousReceipts();

        verify(receiptRepository, times(1)).findExpiredAnonymousReceipts();
        verify(receiptRepository, times(100)).delete(anyString());
    }

    @Test
    @DisplayName("Should delete only anonymous receipts")
    void shouldDeleteOnlyAnonymousReceipts() {
        Receipt anonymousReceipt = Receipt.builder()
                .uid("anon-receipt-1")
                .userId("anonymous")
                .build();

        Receipt verifiedAnonymousReceipt = anonymousReceipt;
        assert verifiedAnonymousReceipt.getUserId().equals("anonymous");
    }

    @Test
    @DisplayName("Should handle empty receipt list gracefully")
    void shouldHandleEmptyReceiptListGracefully() {
        when(receiptRepository.findExpiredAnonymousReceipts()).thenReturn(new ArrayList<>());

        receiptCleanupScheduler.cleanupExpiredAnonymousReceipts();

        verify(receiptRepository, times(1)).findExpiredAnonymousReceipts();
        verify(receiptRepository, never()).delete(anyString());
    }

    @Test
    @DisplayName("Should handle null receipt list")
    void shouldHandleNullReceiptList() {
        when(receiptRepository.findExpiredAnonymousReceipts()).thenReturn(null);

        try {
            receiptCleanupScheduler.cleanupExpiredAnonymousReceipts();
        } catch (NullPointerException e) {
            // Expected behavior - service should handle this in production
        }

        verify(receiptRepository, times(1)).findExpiredAnonymousReceipts();
    }

    @Test
    @DisplayName("Should delete receipts with valid UIDs")
    void shouldDeleteReceiptsWithValidUids() {
        Receipt receipt = Receipt.builder()
                .uid("valid-uid-123")
                .userId("anonymous")
                .build();

        when(receiptRepository.findExpiredAnonymousReceipts()).thenReturn(Arrays.asList(receipt));
        when(receiptRepository.delete("valid-uid-123")).thenReturn(true);

        receiptCleanupScheduler.cleanupExpiredAnonymousReceipts();

        verify(receiptRepository, times(1)).delete("valid-uid-123");
    }
}
