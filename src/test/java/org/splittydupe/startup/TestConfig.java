package org.splittydupe.startup;

import com.google.cloud.Timestamp;
import org.splittydupe.startup.model.LineItem;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.model.User;

import java.util.*;

public class TestConfig {

    public static final String TEST_USER_EMAIL = "test@example.com";
    public static final String TEST_USER_ID = "test-user-123";
    public static final String TEST_RECEIPT_UID = "receipt-123";
    public static final String TEST_JWT_SECRET = "testSecretKeyForJWTAtLeast32CharactersLong123456789";
    public static final long TEST_JWT_EXPIRATION = 3600000L; // 1 hour

    public static User createTestUser() {
        User user = new User();
        user.setUid(TEST_USER_ID);
        user.setEmail(TEST_USER_EMAIL);
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setRoles(List.of("ROLE_USER"));
        user.setCreatedAt(Timestamp.now());
        return user;
    }

    public static Receipt createTestReceipt() {
        Receipt receipt = new Receipt();
        receipt.setUid(TEST_RECEIPT_UID);
        receipt.setVendor("Test Restaurant");
        receipt.setReceiptDate("2025-11-18");
        receipt.setCurrency("USD");
        receipt.setSubtotal(100.00);
        receipt.setTax(8.50);
        receipt.setTip(15.00);
        receipt.setTotal(123.50);
        receipt.setTaxPercentage(8.5);
        receipt.setTipPercentage(15.0);
        receipt.setTaxTipDistribution("proportional");
        receipt.setUserId(TEST_USER_ID);
        receipt.setPaymentStatus("pending");
        receipt.setLineItems(createTestLineItems());
        receipt.setAssigneePaymentStatus(new HashMap<>());
        return receipt;
    }

    public static List<LineItem> createTestLineItems() {
        List<LineItem> items = new ArrayList<>();

        LineItem item1 = new LineItem();
        item1.setName("Burger");
        item1.setPrice(12.00);
        item1.setQuantity(2);
        item1.setAssignees(Arrays.asList("Alice", "Bob"));
        item1.setSplitMode("equal");
        item1.setAssigneePaymentStatus(new HashMap<>());
        item1.getAssigneePaymentStatus().put("Alice", "unpaid");
        item1.getAssigneePaymentStatus().put("Bob", "unpaid");
        items.add(item1);

        LineItem item2 = new LineItem();
        item2.setName("Pizza");
        item2.setPrice(18.00);
        item2.setQuantity(1);
        item2.setAssignees(Arrays.asList("Alice", "Bob", "Charlie"));
        item2.setSplitMode("percentage");
        item2.setAssigneePercentages(new HashMap<>());
        item2.getAssigneePercentages().put("Alice", 50.0);
        item2.getAssigneePercentages().put("Bob", 30.0);
        item2.getAssigneePercentages().put("Charlie", 20.0);
        item2.setAssigneePaymentStatus(new HashMap<>());
        item2.getAssigneePaymentStatus().put("Alice", "unpaid");
        item2.getAssigneePaymentStatus().put("Bob", "unpaid");
        item2.getAssigneePaymentStatus().put("Charlie", "unpaid");
        items.add(item2);

        return items;
    }

    public static Receipt createAnonymousReceipt() {
        Receipt receipt = createTestReceipt();
        receipt.setUserId("anonymous");
        return receipt;
    }

    public static Receipt createMinimalReceipt() {
        Receipt receipt = new Receipt();
        receipt.setVendor("Test");
        receipt.setReceiptDate("2025-11-18");
        receipt.setCurrency("USD");
        receipt.setSubtotal(10.00);
        receipt.setTax(0.85);
        receipt.setTip(1.50);
        receipt.setTotal(12.35);
        receipt.setLineItems(new ArrayList<>());
        return receipt;
    }
}
