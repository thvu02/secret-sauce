package org.splittydupe.startup.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LineItem Model Tests")
class LineItemTest {

    @Test
    @DisplayName("Should create LineItem with builder")
    void shouldCreateLineItemWithBuilder() {
        LineItem lineItem = LineItem.builder()
                .name("Pizza")
                .price(15.99)
                .quantity(2)
                .numAssignees(3)
                .assignees(Arrays.asList("Alice", "Bob", "Charlie"))
                .splitMode("equal")
                .assigneePercentages(new HashMap<>())
                .assigneePaymentStatus(new HashMap<>())
                .build();

        assertEquals("Pizza", lineItem.getName());
        assertEquals(15.99, lineItem.getPrice());
        assertEquals(2, lineItem.getQuantity());
        assertEquals(3, lineItem.getNumAssignees());
        assertEquals(3, lineItem.getAssignees().size());
        assertEquals("equal", lineItem.getSplitMode());
    }

    @Test
    @DisplayName("Should create LineItem with default values")
    void shouldCreateLineItemWithDefaults() {
        LineItem lineItem = LineItem.builder()
                .name("Burger")
                .price(10.99)
                .build();

        assertEquals("Burger", lineItem.getName());
        assertEquals(10.99, lineItem.getPrice());
        assertEquals(1, lineItem.getQuantity());
        assertEquals("equal", lineItem.getSplitMode());
        assertNotNull(lineItem.getAssigneePaymentStatus());
    }

    @Test
    @DisplayName("Should create LineItem with no-args constructor")
    void shouldCreateLineItemWithNoArgsConstructor() {
        LineItem lineItem = new LineItem();
        lineItem.setName("Salad");
        lineItem.setPrice(8.99);

        assertEquals("Salad", lineItem.getName());
        assertEquals(8.99, lineItem.getPrice());
    }

    @Test
    @DisplayName("Should create LineItem with all-args constructor")
    void shouldCreateLineItemWithAllArgsConstructor() {
        Map<String, Double> percentages = new HashMap<>();
        percentages.put("Alice", 50.0);
        percentages.put("Bob", 50.0);

        Map<String, String> paymentStatus = new HashMap<>();
        paymentStatus.put("Alice", "paid");
        paymentStatus.put("Bob", "unpaid");

        LineItem lineItem = new LineItem(
                "Pasta",
                12.50,
                2,
                2,
                Arrays.asList("Alice", "Bob"),
                "percentage",
                percentages,
                paymentStatus
        );

        assertEquals("Pasta", lineItem.getName());
        assertEquals(12.50, lineItem.getPrice());
        assertEquals(2, lineItem.getQuantity());
        assertEquals("percentage", lineItem.getSplitMode());
        assertEquals(2, lineItem.getAssigneePercentages().size());
        assertEquals(50.0, lineItem.getAssigneePercentages().get("Alice"));
    }

    @Test
    @DisplayName("Should handle percentage split mode")
    void shouldHandlePercentageSplitMode() {
        Map<String, Double> percentages = new HashMap<>();
        percentages.put("Alice", 60.0);
        percentages.put("Bob", 40.0);

        LineItem lineItem = LineItem.builder()
                .name("Steak")
                .price(25.00)
                .splitMode("percentage")
                .assigneePercentages(percentages)
                .build();

        assertEquals("percentage", lineItem.getSplitMode());
        assertEquals(60.0, lineItem.getAssigneePercentages().get("Alice"));
        assertEquals(40.0, lineItem.getAssigneePercentages().get("Bob"));
    }

    @Test
    @DisplayName("Should handle payment status tracking")
    void shouldHandlePaymentStatusTracking() {
        Map<String, String> paymentStatus = new HashMap<>();
        paymentStatus.put("Alice", "paid");
        paymentStatus.put("Bob", "unpaid");
        paymentStatus.put("Charlie", "paid");

        LineItem lineItem = LineItem.builder()
                .name("Drinks")
                .price(15.00)
                .assignees(Arrays.asList("Alice", "Bob", "Charlie"))
                .assigneePaymentStatus(paymentStatus)
                .build();

        assertEquals("paid", lineItem.getAssigneePaymentStatus().get("Alice"));
        assertEquals("unpaid", lineItem.getAssigneePaymentStatus().get("Bob"));
        assertEquals("paid", lineItem.getAssigneePaymentStatus().get("Charlie"));
    }

    @Test
    @DisplayName("Should support equals and hashCode")
    void shouldSupportEqualsAndHashCode() {
        LineItem item1 = LineItem.builder()
                .name("Burger")
                .price(10.99)
                .quantity(1)
                .build();

        LineItem item2 = LineItem.builder()
                .name("Burger")
                .price(10.99)
                .quantity(1)
                .build();

        LineItem item3 = LineItem.builder()
                .name("Pizza")
                .price(15.99)
                .quantity(1)
                .build();

        assertEquals(item1, item2);
        assertNotEquals(item1, item3);
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    @DisplayName("Should support toString")
    void shouldSupportToString() {
        LineItem lineItem = LineItem.builder()
                .name("Burger")
                .price(10.99)
                .build();

        String toString = lineItem.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("Burger"));
        assertTrue(toString.contains("10.99"));
    }
}
