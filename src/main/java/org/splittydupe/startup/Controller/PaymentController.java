package org.splittydupe.startup.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.dto.ErrorResponse;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.service.JwtService;
import org.splittydupe.startup.service.ReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ReceiptService receiptService;
    private final JwtService jwtService;

    @PatchMapping("/receipts/{receiptId}/assignees/{assignee}")
    public ResponseEntity<?> updateAssigneePaymentStatus(
            @PathVariable String receiptId,
            @PathVariable String assignee,
            @RequestBody PaymentStatusRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (!"paid".equals(request.getStatus()) && !"unpaid".equals(request.getStatus())) {
                return ResponseEntity.badRequest().body(
                        ErrorResponse.builder()
                                .error("Invalid payment status")
                                .message("Status must be either 'paid' or 'unpaid'")
                                .build()
                );
            }

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = jwtService.extractUsername(token);

                Receipt receipt = receiptService.getReceipt(receiptId);
                if (receipt != null && !email.equals(receipt.getUserId()) && !"anonymous".equals(receipt.getUserId())) {
                    return ResponseEntity.status(403).body(
                            ErrorResponse.builder()
                                    .error("Access denied")
                                    .message("You do not have permission to update this receipt")
                                    .build()
                    );
                }
            }

            boolean updated = receiptService.updateAssigneePaymentStatus(
                    receiptId,
                    assignee,
                    request.getStatus()
            );

            if (updated) {
                Receipt receipt = receiptService.getReceipt(receiptId);
                log.info("Assignee payment status updated for receipt: {}, assignee: {}, status: {} (all items)",
                        receiptId, assignee, request.getStatus());
                return ResponseEntity.ok(receipt);
            } else {
                return ResponseEntity.status(404).body(
                        ErrorResponse.builder()
                                .error("Update failed")
                                .message("Receipt not found")
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("Failed to update assignee payment status", e);
            return ResponseEntity.status(500).body(
                    ErrorResponse.builder()
                            .error("Update failed")
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @Data
    public static class PaymentStatusRequest {
        private String status; // "paid" or "unpaid"
    }
}
