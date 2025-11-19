package org.splittydupe.startup.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.dto.ErrorResponse;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.service.JwtService;
import org.splittydupe.startup.service.ReceiptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class UserDashboardController {

    private final ReceiptService receiptService;
    private final JwtService jwtService;

    @GetMapping("/receipts")
    public ResponseEntity<?> getUserReceipts(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer "
            String email = jwtService.extractUsername(token);

            List<Receipt> receipts = receiptService.getUserReceipts(email);

            log.info("Retrieved {} receipts for user: {}", receipts.size(), email);
            return ResponseEntity.ok(receipts);
        } catch (Exception e) {
            log.error("Failed to get user receipts", e);
            return ResponseEntity.status(500).body(
                    ErrorResponse.builder()
                            .error("Failed to retrieve receipts")
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/receipts/{receiptId}")
    public ResponseEntity<?> getReceipt(
            @PathVariable String receiptId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtService.extractUsername(token);

            Receipt receipt = receiptService.getReceipt(receiptId);

            if (receipt == null) {
                return ResponseEntity.status(404).body(
                        ErrorResponse.builder()
                                .error("Receipt not found")
                                .message("Receipt with ID " + receiptId + " not found")
                                .build()
                );
            }

            if (!email.equals(receipt.getUserId())) {
                return ResponseEntity.status(403).body(
                        ErrorResponse.builder()
                                .error("Access denied")
                                .message("You do not have permission to view this receipt")
                                .build()
                );
            }

            return ResponseEntity.ok(receipt);
        } catch (Exception e) {
            log.error("Failed to get receipt", e);
            return ResponseEntity.status(500).body(
                    ErrorResponse.builder()
                            .error("Failed to retrieve receipt")
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @DeleteMapping("/receipts/{receiptId}")
    public ResponseEntity<?> deleteReceipt(
            @PathVariable String receiptId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtService.extractUsername(token);

            Receipt receipt = receiptService.getReceipt(receiptId);

            if (receipt == null) {
                return ResponseEntity.status(404).body(
                        ErrorResponse.builder()
                                .error("Receipt not found")
                                .message("Receipt with ID " + receiptId + " not found")
                                .build()
                );
            }

            if (!email.equals(receipt.getUserId())) {
                return ResponseEntity.status(403).body(
                        ErrorResponse.builder()
                                .error("Access denied")
                                .message("You do not have permission to delete this receipt")
                                .build()
                );
            }

            boolean deleted = receiptService.deleteReceipt(receiptId);

            if (deleted) {
                log.info("Receipt deleted successfully by user: {}, receipt ID: {}", email, receiptId);
                return ResponseEntity.ok().body(new DeleteResponse(true, "Receipt deleted successfully"));
            } else {
                return ResponseEntity.status(500).body(
                        ErrorResponse.builder()
                                .error("Delete failed")
                                .message("Failed to delete receipt")
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("Failed to delete receipt", e);
            return ResponseEntity.status(500).body(
                    ErrorResponse.builder()
                            .error("Failed to delete receipt")
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    private record DeleteResponse(boolean deleted, String message) {}
}
