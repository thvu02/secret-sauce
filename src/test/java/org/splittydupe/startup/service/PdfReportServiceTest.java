package org.splittydupe.startup.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.splittydupe.startup.TestConfig;
import org.splittydupe.startup.model.LineItem;
import org.splittydupe.startup.model.Receipt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PDF Report Service Tests")
class PdfReportServiceTest {

    private PdfReportService pdfReportService;
    private Receipt testReceipt;

    @BeforeEach
    void setUp() {
        pdfReportService = new PdfReportService();
        testReceipt = TestConfig.createTestReceipt();
    }

    @Test
    @DisplayName("Should generate PDF report successfully")
    void shouldGeneratePdfReportSuccessfully() {
        byte[] pdfBytes = pdfReportService.generateReceiptReport(testReceipt);

        assertNotNull(pdfBytes, "PDF bytes should not be null");
        assertTrue(pdfBytes.length > 0, "PDF should have content");

        String header = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
        assertEquals("%PDF", header, "Should be a valid PDF file");
    }

    @Test
    @DisplayName("Should generate PDF for minimal receipt")
    void shouldGeneratePdfForMinimalReceipt() {
        Receipt minimalReceipt = TestConfig.createMinimalReceipt();

        byte[] pdfBytes = pdfReportService.generateReceiptReport(minimalReceipt);

        assertNotNull(pdfBytes, "PDF bytes should not be null");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should generate PDF with multiple assignees")
    void shouldGeneratePdfWithMultipleAssignees() {
        byte[] pdfBytes = pdfReportService.generateReceiptReport(testReceipt);

        assertNotNull(pdfBytes, "PDF should be generated");
        assertTrue(pdfBytes.length > 1000, "PDF should have substantial content");
    }

    @Test
    @DisplayName("Should generate PDF with percentage split")
    void shouldGeneratePdfWithPercentageSplit() {

        byte[] pdfBytes = pdfReportService.generateReceiptReport(testReceipt);

        assertNotNull(pdfBytes, "PDF should be generated");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should handle null receipt gracefully")
    void shouldHandleNullReceiptGracefully() {
        assertThrows(NullPointerException.class, () -> {
            pdfReportService.generateReceiptReport(null);
        }, "Should throw NullPointerException for null receipt");
    }

    @Test
    @DisplayName("Should generate PDF with proportional tax/tip distribution")
    void shouldGeneratePdfWithProportionalTaxTipDistribution() {
        testReceipt.setTaxTipDistribution("proportional");

        byte[] pdfBytes = pdfReportService.generateReceiptReport(testReceipt);

        assertNotNull(pdfBytes, "PDF should be generated");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should generate PDF with even tax/tip distribution")
    void shouldGeneratePdfWithEvenTaxTipDistribution() {
        testReceipt.setTaxTipDistribution("even");

        byte[] pdfBytes = pdfReportService.generateReceiptReport(testReceipt);

        assertNotNull(pdfBytes, "PDF should be generated");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should generate consistent PDF for same receipt")
    void shouldGenerateConsistentPdfForSameReceipt() {
        byte[] pdfBytes1 = pdfReportService.generateReceiptReport(testReceipt);
        byte[] pdfBytes2 = pdfReportService.generateReceiptReport(testReceipt);

        assertNotNull(pdfBytes1, "First PDF should be generated");
        assertNotNull(pdfBytes2, "Second PDF should be generated");
        assertTrue(Math.abs(pdfBytes1.length - pdfBytes2.length) < 1000,
                "PDF sizes should be similar");
    }

    @Test
    @DisplayName("Should generate PDF with many line items requiring new page")
    void shouldGeneratePdfWithManyLineItemsRequiringNewPage() {
        Receipt largeReceipt = TestConfig.createTestReceipt();
        ArrayList<LineItem> manyItems = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            LineItem item = LineItem.builder()
                    .name("Item " + i)
                    .price(10.0 + i)
                    .quantity(1)
                    .assignees(Arrays.asList("Alice", "Bob"))
                    .splitMode("equal")
                    .build();
            manyItems.add(item);
        }

        largeReceipt.setLineItems(manyItems);

        byte[] pdfBytes = pdfReportService.generateReceiptReport(largeReceipt);

        assertNotNull(pdfBytes, "PDF should be generated");
        assertTrue(pdfBytes.length > 2000, "PDF with many items should be large");
    }

    @Test
    @DisplayName("Should handle line item with quantity greater than 1")
    void shouldHandleLineItemWithQuantityGreaterThanOne() {
        Receipt receipt = TestConfig.createMinimalReceipt();
        LineItem multiQuantityItem = LineItem.builder()
                .name("Pizza")
                .price(15.0)
                .quantity(3)
                .assignees(Arrays.asList("Alice"))
                .splitMode("equal")
                .build();
        receipt.setLineItems(Arrays.asList(multiQuantityItem));

        byte[] pdfBytes = pdfReportService.generateReceiptReport(receipt);

        assertNotNull(pdfBytes, "PDF should be generated");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should handle line items with null assignees")
    void shouldHandleLineItemsWithNullAssignees() {
        Receipt receipt = TestConfig.createMinimalReceipt();
        LineItem itemWithNullAssignees = LineItem.builder()
                .name("Unassigned Item")
                .price(20.0)
                .quantity(1)
                .assignees(null)
                .build();
        receipt.setLineItems(Arrays.asList(itemWithNullAssignees));

        byte[] pdfBytes = pdfReportService.generateReceiptReport(receipt);

        assertNotNull(pdfBytes, "PDF should be generated even with null assignees");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should handle line items with empty assignees")
    void shouldHandleLineItemsWithEmptyAssignees() {
        Receipt receipt = TestConfig.createMinimalReceipt();
        LineItem itemWithEmptyAssignees = LineItem.builder()
                .name("Empty Assignees Item")
                .price(15.0)
                .quantity(1)
                .assignees(new ArrayList<>())
                .build();
        receipt.setLineItems(Arrays.asList(itemWithEmptyAssignees));

        byte[] pdfBytes = pdfReportService.generateReceiptReport(receipt);

        assertNotNull(pdfBytes, "PDF should be generated with empty assignees");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should handle receipt with null vendor")
    void shouldHandleReceiptWithNullVendor() {
        Receipt receipt = TestConfig.createMinimalReceipt();
        receipt.setVendor(null);

        byte[] pdfBytes = pdfReportService.generateReceiptReport(receipt);

        assertNotNull(pdfBytes, "PDF should be generated with null vendor");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should handle receipt with null date")
    void shouldHandleReceiptWithNullDate() {
        Receipt receipt = TestConfig.createMinimalReceipt();
        receipt.setReceiptDate(null);

        byte[] pdfBytes = pdfReportService.generateReceiptReport(receipt);

        assertNotNull(pdfBytes, "PDF should be generated with null date");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should handle receipt with null currency")
    void shouldHandleReceiptWithNullCurrency() {
        Receipt receipt = TestConfig.createMinimalReceipt();
        receipt.setCurrency(null);

        byte[] pdfBytes = pdfReportService.generateReceiptReport(receipt);

        assertNotNull(pdfBytes, "PDF should be generated with null currency");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should handle receipt with null taxTipDistribution (defaults to proportional)")
    void shouldHandleReceiptWithNullTaxTipDistribution() {
        Receipt receipt = TestConfig.createTestReceipt();
        receipt.setTaxTipDistribution(null);

        byte[] pdfBytes = pdfReportService.generateReceiptReport(receipt);

        assertNotNull(pdfBytes, "PDF should be generated with null taxTipDistribution");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should handle percentage split with null assigneePercentages")
    void shouldHandlePercentageSplitWithNullAssigneePercentages() {
        Receipt receipt = TestConfig.createMinimalReceipt();
        LineItem percentageItem = LineItem.builder()
                .name("Split Item")
                .price(100.0)
                .quantity(1)
                .assignees(Arrays.asList("Alice", "Bob"))
                .splitMode("percentage")
                .assigneePercentages(null)
                .build();
        receipt.setLineItems(Arrays.asList(percentageItem));

        byte[] pdfBytes = pdfReportService.generateReceiptReport(receipt);

        assertNotNull(pdfBytes, "PDF should be generated with null assigneePercentages");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should handle percentage split with missing percentage for assignee")
    void shouldHandlePercentageSplitWithMissingPercentageForAssignee() {
        Receipt receipt = TestConfig.createMinimalReceipt();
        Map<String, Double> percentages = new HashMap<>();
        percentages.put("Alice", 60.0);

        LineItem percentageItem = LineItem.builder()
                .name("Partial Split Item")
                .price(100.0)
                .quantity(1)
                .assignees(Arrays.asList("Alice", "Bob"))
                .splitMode("percentage")
                .assigneePercentages(percentages)
                .build();
        receipt.setLineItems(Arrays.asList(percentageItem));

        byte[] pdfBytes = pdfReportService.generateReceiptReport(receipt);

        assertNotNull(pdfBytes, "PDF should be generated with missing percentages");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should handle even distribution with zero assignees")
    void shouldHandleEvenDistributionWithZeroAssignees() {
        Receipt receipt = TestConfig.createMinimalReceipt();
        receipt.setTaxTipDistribution("even");
        receipt.setLineItems(new ArrayList<>());

        byte[] pdfBytes = pdfReportService.generateReceiptReport(receipt);

        assertNotNull(pdfBytes, "PDF should be generated with no assignees");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }

    @Test
    @DisplayName("Should handle proportional distribution with zero subtotal")
    void shouldHandleProportionalDistributionWithZeroSubtotal() {
        Receipt receipt = TestConfig.createMinimalReceipt();
        receipt.setSubtotal(0.0);
        receipt.setTaxTipDistribution("proportional");

        LineItem item = LineItem.builder()
                .name("Free Item")
                .price(0.0)
                .quantity(1)
                .assignees(Arrays.asList("Alice"))
                .splitMode("equal")
                .build();
        receipt.setLineItems(Arrays.asList(item));

        byte[] pdfBytes = pdfReportService.generateReceiptReport(receipt);

        assertNotNull(pdfBytes, "PDF should be generated with zero subtotal");
        assertTrue(pdfBytes.length > 0, "PDF should have content");
    }
}
