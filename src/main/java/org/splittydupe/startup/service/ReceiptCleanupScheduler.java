package org.splittydupe.startup.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.repository.IReceiptRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptCleanupScheduler {

    private final IReceiptRepository receiptRepository;

    /**
     * Delete expired anonymous receipts
     * Runs every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredAnonymousReceipts() {
        try {
            log.info("Starting cleanup of expired anonymous receipts");

            List<Receipt> expiredReceipts = receiptRepository.findExpiredAnonymousReceipts();

            if (expiredReceipts.isEmpty()) {
                log.info("No expired anonymous receipts found");
                return;
            }

            int deletedCount = 0;
            for (Receipt receipt : expiredReceipts) {
                try {
                    receiptRepository.delete(receipt.getUid());
                    deletedCount++;
                    log.debug("Deleted expired receipt: {}", receipt.getUid());
                } catch (Exception e) {
                    log.error("Failed to delete expired receipt: {}", receipt.getUid(), e);
                }
            }

            log.info("Cleanup completed. Deleted {} out of {} expired anonymous receipts",
                    deletedCount, expiredReceipts.size());
        } catch (Exception e) {
            log.error("Failed to run cleanup task", e);
        }
    }
}
