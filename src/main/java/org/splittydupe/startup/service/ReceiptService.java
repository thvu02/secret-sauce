package org.splittydupe.startup.service;

import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.model.LineItem;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.repository.IReceiptRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final IReceiptRepository receiptRepository;

    public boolean saveReceipt(Receipt receipt, String userId) {
        // Generate UID if not present (for manually entered receipts)
        if (receipt.getUid() == null || receipt.getUid().trim().isEmpty()) {
            String uid = UUID.randomUUID().toString();
            receipt.setUid(uid);
            log.info("Generated new UID for receipt: {}", uid);
        }

        // Set user ID (default to "anonymous" if not provided)
        if (userId == null || userId.trim().isEmpty()) {
            receipt.setUserId("anonymous");
        } else {
            receipt.setUserId(userId);
        }

        if (receipt.getCreatedAt() == null) {
            receipt.setCreatedAt(Timestamp.now());
        }

        if ("anonymous".equals(receipt.getUserId())) {
            Timestamp now = Timestamp.now();
            Timestamp expiry = Timestamp.ofTimeSecondsAndNanos(
                now.getSeconds() + TimeUnit.DAYS.toSeconds(7),
                now.getNanos()
            );
            receipt.setExpiresAt(expiry);
            log.info("Set expiration for anonymous receipt: {}", receipt.getExpiresAt());
        }

        if (receipt.getLineItems() != null) {
            for (LineItem lineItem : receipt.getLineItems()) {
                if (lineItem.getAssigneePaymentStatus() == null) {
                    lineItem.setAssigneePaymentStatus(new HashMap<>());
                }
                if (lineItem.getAssignees() != null) {
                    for (String assignee : lineItem.getAssignees()) {
                        lineItem.getAssigneePaymentStatus().putIfAbsent(assignee, "unpaid");
                    }
                }
            }
        }

        if (receipt.getAssigneePaymentStatus() == null) {
            receipt.setAssigneePaymentStatus(new HashMap<>());
        }
        if (receipt.getLineItems() != null) {
            for (LineItem lineItem : receipt.getLineItems()) {
                if (lineItem.getAssignees() != null) {
                    for (String assignee : lineItem.getAssignees()) {
                        receipt.getAssigneePaymentStatus().putIfAbsent(assignee, "unpaid");
                    }
                }
            }
        }

        if (receipt.getPaymentStatus() == null) {
            updateReceiptPaymentStatus(receipt);
        }

        log.info("Saving receipt with UID: {}, userId: {}", receipt.getUid(), receipt.getUserId());
        return receiptRepository.save(receipt);
    }

    /**
     * Legacy method for backward compatibility
     */
    public boolean saveReceipt(Receipt receipt) {
        return saveReceipt(receipt, null);
    }

    public Receipt getReceipt(String uid) {
        log.info("Retrieving receipt with UID: {}", uid);
        return receiptRepository.findById(uid);
    }

    public List<Receipt> getUserReceipts(String userId) {
        log.info("Retrieving receipts for user: {}", userId);
        return receiptRepository.findByUserId(userId);
    }

    public boolean deleteReceipt(String receiptId) {
        log.info("Deleting receipt with ID: {}", receiptId);
        return receiptRepository.delete(receiptId);
    }

    public boolean updateAssigneePaymentStatus(String receiptId, String assignee, String status) {
        Receipt receipt = receiptRepository.findById(receiptId);
        if (receipt == null) {
            log.warn("Receipt not found: {}", receiptId);
            return false;
        }

        if (receipt.getAssigneePaymentStatus() == null) {
            receipt.setAssigneePaymentStatus(new HashMap<>());
        }
        receipt.getAssigneePaymentStatus().put(assignee, status);

        if (receipt.getLineItems() != null) {
            for (LineItem lineItem : receipt.getLineItems()) {
                if (lineItem.getAssignees() != null && lineItem.getAssignees().contains(assignee)) {
                    if (lineItem.getAssigneePaymentStatus() == null) {
                        lineItem.setAssigneePaymentStatus(new HashMap<>());
                    }
                    lineItem.getAssigneePaymentStatus().put(assignee, status);
                }
            }
        }

        log.info("Updated payment status for receipt: {}, assignee: {}, status: {} (all line items)",
                receiptId, assignee, status);

        updateReceiptPaymentStatus(receipt);

        return receiptRepository.save(receipt);
    }

    private void updateReceiptPaymentStatus(Receipt receipt) {
        if (receipt.getAssigneePaymentStatus() == null || receipt.getAssigneePaymentStatus().isEmpty()) {
            receipt.setPaymentStatus("complete");
            return;
        }

        int totalAssignees = receipt.getAssigneePaymentStatus().size();
        int paidAssignees = 0;

        for (String status : receipt.getAssigneePaymentStatus().values()) {
            if ("paid".equals(status)) {
                paidAssignees++;
            }
        }

        if (paidAssignees == 0) {
            receipt.setPaymentStatus("pending");
        } else if (paidAssignees == totalAssignees) {
            receipt.setPaymentStatus("complete");
        } else {
            receipt.setPaymentStatus("partial");
        }
    }
}
