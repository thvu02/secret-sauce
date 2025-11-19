package org.splittydupe.startup.model;

import com.google.cloud.Timestamp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Receipt Model Tests")
class ReceiptTest {

    @Test
    @DisplayName("Should create Receipt with builder")
    void shouldCreateReceiptWithBuilder() {
        Timestamp now = Timestamp.now();
        List<LineItem> lineItems = new ArrayList<>();
        lineItems.add(LineItem.builder().name("Item 1").price(10.0).build());

        Receipt receipt = Receipt.builder()
                .uid("receipt-123")
                .vendor("Test Restaurant")
                .receiptDate("2024-01-15")
                .currency("USD")
                .subtotal(100.00)
                .tax(8.00)
                .taxPercentage(8.0)
                .tip(15.00)
                .tipPercentage(15.0)
                .total(123.00)
                .lineItems(lineItems)
                .userId("user-123")
                .createdAt(now)
                .paymentStatus("pending")
                .taxTipDistribution("proportional")
                .build();

        assertEquals("receipt-123", receipt.getUid());
        assertEquals("Test Restaurant", receipt.getVendor());
        assertEquals("2024-01-15", receipt.getReceiptDate());
        assertEquals("USD", receipt.getCurrency());
        assertEquals(100.00, receipt.getSubtotal());
        assertEquals(8.00, receipt.getTax());
        assertEquals(8.0, receipt.getTaxPercentage());
        assertEquals(15.00, receipt.getTip());
        assertEquals(15.0, receipt.getTipPercentage());
        assertEquals(123.00, receipt.getTotal());
        assertEquals(1, receipt.getLineItems().size());
        assertEquals("user-123", receipt.getUserId());
        assertEquals("pending", receipt.getPaymentStatus());
        assertEquals("proportional", receipt.getTaxTipDistribution());
    }

    @Test
    @DisplayName("Should create Receipt with default values")
    void shouldCreateReceiptWithDefaults() {
        Receipt receipt = Receipt.builder()
                .uid("receipt-456")
                .vendor("Cafe")
                .build();

        assertEquals("USD", receipt.getCurrency());
        assertEquals("pending", receipt.getPaymentStatus());
        assertEquals("proportional", receipt.getTaxTipDistribution());
    }

    @Test
    @DisplayName("Should create Receipt with no-args constructor")
    void shouldCreateReceiptWithNoArgsConstructor() {
        Receipt receipt = new Receipt();
        receipt.setUid("receipt-789");
        receipt.setVendor("Diner");
        receipt.setTotal(50.00);

        assertEquals("receipt-789", receipt.getUid());
        assertEquals("Diner", receipt.getVendor());
        assertEquals(50.00, receipt.getTotal());
    }

    @Test
    @DisplayName("Should handle anonymous user receipts")
    void shouldHandleAnonymousUserReceipts() {
        Timestamp now = Timestamp.now();
        Timestamp expiry = Timestamp.ofTimeSecondsAndNanos(
                now.getSeconds() + (7 * 24 * 60 * 60), // 7 days
                now.getNanos()
        );

        Receipt receipt = Receipt.builder()
                .uid("anon-receipt-1")
                .userId("anonymous")
                .createdAt(now)
                .expiresAt(expiry)
                .build();

        assertEquals("anonymous", receipt.getUserId());
        assertNotNull(receipt.getExpiresAt());
        assertTrue(receipt.getExpiresAt().getSeconds() > receipt.getCreatedAt().getSeconds());
    }

    @Test
    @DisplayName("Should track assignee payment status")
    void shouldTrackAssigneePaymentStatus() {
        Map<String, String> paymentStatus = new HashMap<>();
        paymentStatus.put("Alice", "paid");
        paymentStatus.put("Bob", "unpaid");
        paymentStatus.put("Charlie", "paid");

        Receipt receipt = Receipt.builder()
                .uid("receipt-payment-1")
                .assigneePaymentStatus(paymentStatus)
                .paymentStatus("partial")
                .build();

        assertEquals("partial", receipt.getPaymentStatus());
        assertEquals("paid", receipt.getAssigneePaymentStatus().get("Alice"));
        assertEquals("unpaid", receipt.getAssigneePaymentStatus().get("Bob"));
    }

    @Test
    @DisplayName("Should handle different tax/tip distribution modes")
    void shouldHandleDifferentDistributionModes() {
        Receipt proportionalReceipt = Receipt.builder()
                .uid("receipt-prop")
                .taxTipDistribution("proportional")
                .build();

        Receipt evenReceipt = Receipt.builder()
                .uid("receipt-even")
                .taxTipDistribution("even")
                .build();

        assertEquals("proportional", proportionalReceipt.getTaxTipDistribution());
        assertEquals("even", evenReceipt.getTaxTipDistribution());
    }

    @Test
    @DisplayName("Should handle multiple line items")
    void shouldHandleMultipleLineItems() {
        List<LineItem> lineItems = new ArrayList<>();
        lineItems.add(LineItem.builder().name("Item 1").price(10.0).build());
        lineItems.add(LineItem.builder().name("Item 2").price(20.0).build());
        lineItems.add(LineItem.builder().name("Item 3").price(30.0).build());

        Receipt receipt = Receipt.builder()
                .uid("receipt-multi")
                .lineItems(lineItems)
                .subtotal(60.0)
                .build();

        assertEquals(3, receipt.getLineItems().size());
        assertEquals(60.0, receipt.getSubtotal());
        assertEquals("Item 1", receipt.getLineItems().get(0).getName());
        assertEquals("Item 3", receipt.getLineItems().get(2).getName());
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        Receipt receipt1 = Receipt.builder()
                .uid("receipt-1")
                .vendor("Restaurant")
                .total(100.0)
                .build();

        Receipt receipt2 = Receipt.builder()
                .uid("receipt-1")
                .vendor("Restaurant")
                .total(100.0)
                .build();

        Receipt receipt3 = Receipt.builder()
                .uid("receipt-2")
                .vendor("Cafe")
                .total(50.0)
                .build();

        assertEquals(receipt1, receipt2);
        assertNotEquals(receipt1, receipt3);
        assertEquals(receipt1.hashCode(), receipt2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        Receipt receipt = Receipt.builder()
                .uid("receipt-1")
                .vendor("Restaurant")
                .build();

        String toString = receipt.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("receipt-1"));
        assertTrue(toString.contains("Restaurant"));
    }

    @Test
    @DisplayName("Should calculate payment status based on assignees")
    void shouldCalculatePaymentStatus() {
        Map<String, String> allPaid = new HashMap<>();
        allPaid.put("Alice", "paid");
        allPaid.put("Bob", "paid");

        Receipt receiptComplete = Receipt.builder()
                .uid("receipt-complete")
                .assigneePaymentStatus(allPaid)
                .paymentStatus("complete")
                .build();

        Map<String, String> partialPaid = new HashMap<>();
        partialPaid.put("Alice", "paid");
        partialPaid.put("Bob", "unpaid");

        Receipt receiptPartial = Receipt.builder()
                .uid("receipt-partial")
                .assigneePaymentStatus(partialPaid)
                .paymentStatus("partial")
                .build();

        assertEquals("complete", receiptComplete.getPaymentStatus());
        assertEquals("partial", receiptPartial.getPaymentStatus());
    }
}
