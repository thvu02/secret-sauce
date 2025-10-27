package org.splittydupe.startup.Database;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LineItemTest {

    @Test
    public void testNoArgsConstructorAndDefaults() {
        LineItem item = new LineItem();
        assertNull(item.getName(), "Name should be null");
        assertEquals(0.0, item.getPrice(), "Price should default to 0.0");
        assertEquals(1, item.getQuantity(), "Quantity should default to 1");
        assertEquals(0, item.getNumAssignees(), "NumAssignees should default to 0");
        assertNull(item.getAssignees(), "Assignees should default to null");
    }

    @Test
    public void testAllArgsConstructor() {
        List<String> assignees = Arrays.asList("Alice", "Bob");
        LineItem item = new LineItem("Test Item", 10.5, 2, 2, assignees);

        assertEquals("Test Item", item.getName());
        assertEquals(10.5, item.getPrice());
        assertEquals(2, item.getQuantity());
        assertEquals(2, item.getNumAssignees());
        assertEquals(assignees, item.getAssignees());
    }

    @Test
    public void testBuilderPattern() {
        List<String> assignees = Collections.singletonList("Charlie");
        LineItem item = LineItem.builder()
                                .name("Builder Item")
                                .price(15.0)
                                .assignees(assignees)
                                .numAssignees(1)
                                .build();

        assertEquals("Builder Item", item.getName());
        assertEquals(15.0, item.getPrice());
        assertEquals(1, item.getQuantity(), "Default quantity should be 1");
        assertEquals(1, item.getNumAssignees());
        assertEquals(assignees, item.getAssignees());
    }

    @Test
    public void testSettersAndGetters() {
        LineItem item = new LineItem();
        item.setName("New Name");
        item.setPrice(20.0);
        item.setQuantity(5);
        item.setNumAssignees(3);
        List<String> assignees = Arrays.asList("A", "B", "C");
        item.setAssignees(assignees);

        assertEquals("New Name", item.getName());
        assertEquals(20.0, item.getPrice());
        assertEquals(5, item.getQuantity());
        assertEquals(3, item.getNumAssignees());
        assertEquals(assignees, item.getAssignees());
    }

    @Test
    public void testNegativePriceAndQuantity() {
        LineItem item = new LineItem();
        item.setPrice(-10.0);
        item.setQuantity(-5);

        assertEquals(-10.0, item.getPrice());
        assertEquals(-5, item.getQuantity());
    }

    @Test
    public void testEmptyAndNullAssignees() {
        LineItem item = new LineItem();
        item.setAssignees(null);
        assertNull(item.getAssignees());

        item.setAssignees(Collections.emptyList());
        assertTrue(item.getAssignees().isEmpty());
    }
}

