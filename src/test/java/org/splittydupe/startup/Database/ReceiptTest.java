package org.splittydupe.startup.Database;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class ReceiptTest {

    @Test
    public void testNoArgsConstructorAndDefaults() {
        Receipt r = new Receipt();
        assertNull(r.getUid());
        assertNull(r.getVendor());
        assertNull(r.getReceiptDate());
        assertEquals("USD", r.getCurrency());
        assertEquals(0.0, r.getSubtotal());
        assertEquals(0.0, r.getTax());
        assertEquals(0.0, r.getTaxPercentage());
        assertEquals(0.0, r.getTip());
        assertEquals(0.0, r.getTipPercentage());
        assertEquals(0.0, r.getTotal());
        assertNull(r.getLineItems());
    }

    @Test
    public void testAllArgsConstructor() {
        List<LineItem> items = Arrays.asList(new LineItem("A", 1.0, 1, 1, Collections.singletonList("foo")));
        Receipt r = new Receipt("id1", "Walmart", "2025-10-27", "EUR", 100, 8, 8.0, 5, 5.0, 108.0, items);

        assertEquals("id1", r.getUid());
        assertEquals("Walmart", r.getVendor());
        assertEquals("2025-10-27", r.getReceiptDate());
        assertEquals("EUR", r.getCurrency());
        assertEquals(100, r.getSubtotal());
        assertEquals(8, r.getTax());
        assertEquals(8.0, r.getTaxPercentage());
        assertEquals(5, r.getTip());
        assertEquals(5.0, r.getTipPercentage());
        assertEquals(108.0, r.getTotal());
        assertEquals(items, r.getLineItems());
    }

    @Test
    public void testBuilderPattern() {
        Receipt r = Receipt.builder()
                        .uid("id2")
                        .vendor("Target")
                        .subtotal(50.0)
                        .currency("GBP")
                        .build();
        assertEquals("id2", r.getUid());
        assertEquals("Target", r.getVendor());
        assertEquals(50.0, r.getSubtotal());
        assertEquals("GBP", r.getCurrency());
        assertNull(r.getLineItems());
    }

    @Test
    public void testAddLineItems() {
        Receipt receipt = new Receipt();
        LineItem item1 = LineItem.builder().name("Item1").price(10).quantity(2).build();
        LineItem item2 = LineItem.builder().name("Item2").price(5).quantity(1).build();
        receipt.setLineItems(new ArrayList<>());
        receipt.getLineItems().add(item1);
        receipt.getLineItems().add(item2);

        assertEquals(2, receipt.getLineItems().size());
    }
}

