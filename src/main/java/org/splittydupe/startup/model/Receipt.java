package org.splittydupe.startup.model;

import com.google.cloud.Timestamp;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Receipt {
    private String uid;

    private String vendor;

    private String receiptDate;

    @Builder.Default
    private String currency = "USD";

    private double subtotal;

    private double tax;

    private double taxPercentage;

    private double tip;

    private double tipPercentage;

    private double total;

    private List<LineItem> lineItems;

    // User ID - "anonymous" for non-logged-in users
    private String userId;

    private Timestamp createdAt;

    // Expiration for anonymous receipts (7 days from creation)
    private Timestamp expiresAt;

    @Builder.Default
    private String paymentStatus = "pending"; // "pending", "partial", "complete"

    // Maps assignee name to payment status ("paid" or "unpaid")
    private Map<String, String> assigneePaymentStatus;

    // "proportional" - distribute based on line item costs (default)
    // "even" - distribute evenly across all assignees
    @Builder.Default
    private String taxTipDistribution = "proportional";
}
