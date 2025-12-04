package org.splittydupe.startup.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.dto.ErrorResponse;
import org.splittydupe.startup.model.Friend;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.model.UserProfile;
import org.splittydupe.startup.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ReceiptService receiptService;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UserProfileService userProfileService;
    private final FriendService friendService;
    private final PdfReportService pdfReportService;

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

    @PostMapping("/receipts/{receiptId}/assignees/{assignee}/remind")
    public ResponseEntity<?> sendPaymentReminder(
            @PathVariable String receiptId,
            @PathVariable String assignee,
            Authentication authentication) {
        try {
            String userId = authentication.getName();

            Receipt receipt = receiptService.getReceipt(receiptId);
            if (receipt == null) {
                return ResponseEntity.status(404).body(
                        ErrorResponse.builder()
                                .error("Receipt not found")
                                .message("Receipt with ID " + receiptId + " not found")
                                .build()
                );
            }

            if (!userId.equals(receipt.getUserId())) {
                return ResponseEntity.status(403).body(
                        ErrorResponse.builder()
                                .error("Access denied")
                                .message("You do not have permission to send reminders for this receipt")
                                .build()
                );
            }

            Optional<UserProfile> profileOpt = userProfileService.getProfileByUserId(userId);
            if (profileOpt.isEmpty()) {
                return ResponseEntity.status(400).body(
                        ErrorResponse.builder()
                                .error("Profile not found")
                                .message("Please complete your profile before sending payment reminders")
                                .build()
                );
            }

            UserProfile profile = profileOpt.get();
            if (profile.getDisplayName() == null || profile.getDisplayName().isEmpty()) {
                return ResponseEntity.status(400).body(
                        ErrorResponse.builder()
                                .error("Profile incomplete")
                                .message("Please add your display name to your profile")
                                .build()
                );
            }

            List<Friend> friends = friendService.getFriendsByUserId(userId);
            Optional<Friend> friendOpt = friends.stream()
                    .filter(f -> f.getDisplayName().equals(assignee))
                    .findFirst();

            if (friendOpt.isEmpty() || friendOpt.get().getContactEmail() == null
                    || friendOpt.get().getContactEmail().isEmpty()) {
                return ResponseEntity.status(400).body(
                        ErrorResponse.builder()
                                .error("Email not found")
                                .message("No email address found for " + assignee)
                                .build()
                );
            }

            String toEmail = friendOpt.get().getContactEmail();

            byte[] pdfReport = pdfReportService.generateReceiptReport(receipt);

            emailService.sendPaymentReminderEmail(toEmail, assignee, receipt, profile, pdfReport);

            log.info("Payment reminder sent to {} for receipt {}", toEmail, receiptId);
            return ResponseEntity.ok().body(new ReminderSentResponse(true, "Reminder sent successfully to " + toEmail));

        } catch (Exception e) {
            log.error("Failed to send payment reminder", e);
            return ResponseEntity.status(500).body(
                    ErrorResponse.builder()
                            .error("Failed to send reminder")
                            .message(e.getMessage())
                            .build()
            );
        }
    }

    @Data
    public static class PaymentStatusRequest {
        private String status; // "paid" or "unpaid"
    }

    @Data
    @RequiredArgsConstructor
    public static class ReminderSentResponse {
        private final boolean sent;
        private final String message;
    }
}
