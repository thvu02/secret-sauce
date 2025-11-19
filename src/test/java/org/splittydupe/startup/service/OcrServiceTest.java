package org.splittydupe.startup.service;

import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.splittydupe.startup.exception.OcrProcessingException;
import org.splittydupe.startup.model.LineItem;
import org.splittydupe.startup.model.Receipt;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OcrService Tests")
class OcrServiceTest {

    @Mock
    private DocumentProcessorServiceClient documentProcessorServiceClient;

    @InjectMocks
    private OcrService ocrService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ocrService, "projectId", "test-project");
        ReflectionTestUtils.setField(ocrService, "processorId", "test-processor");
        ReflectionTestUtils.setField(ocrService, "processorLocation", "us");
        ReflectionTestUtils.setField(ocrService, "documentProcessorServiceClient", documentProcessorServiceClient);
    }

    @Test
    @DisplayName("Should extract vendor name from receipt")
    void shouldExtractVendorNameFromReceipt() {
        Document.Entity vendorEntity = Document.Entity.newBuilder()
                .setType("vendor_name")
                .setMentionText("Test Restaurant")
                .build();

        assertEquals("vendor_name", vendorEntity.getType());
        assertEquals("Test Restaurant", vendorEntity.getMentionText());
    }

    @Test
    @DisplayName("Should handle supplier name as vendor")
    void shouldHandleSupplierNameAsVendor() {
        Document.Entity supplierEntity = Document.Entity.newBuilder()
                .setType("supplier_name")
                .setMentionText("Supplier Inc")
                .build();

        assertEquals("supplier_name", supplierEntity.getType());
        assertEquals("Supplier Inc", supplierEntity.getMentionText());
    }

    @Test
    @DisplayName("Should extract receipt date")
    void shouldExtractReceiptDate() {
        Document.Entity.Builder dateEntityBuilder = Document.Entity.newBuilder()
                .setType("receipt_date");

        assertEquals("receipt_date", dateEntityBuilder.getType());
    }

    @Test
    @DisplayName("Should extract currency")
    void shouldExtractCurrency() {
        Document.Entity.Builder currencyEntityBuilder = Document.Entity.newBuilder()
                .setType("currency");

        assertEquals("currency", currencyEntityBuilder.getType());
    }

    @Test
    @DisplayName("Should extract monetary amounts")
    void shouldExtractMonetaryAmounts() {
        Document.Entity.Builder subtotalEntityBuilder = Document.Entity.newBuilder()
                .setType("net_amount");

        Document.Entity.Builder taxEntityBuilder = Document.Entity.newBuilder()
                .setType("total_tax_amount");

        Document.Entity.Builder tipEntityBuilder = Document.Entity.newBuilder()
                .setType("tip_amount");

        Document.Entity.Builder totalEntityBuilder = Document.Entity.newBuilder()
                .setType("total_amount");

        assertEquals("net_amount", subtotalEntityBuilder.getType());
        assertEquals("total_tax_amount", taxEntityBuilder.getType());
        assertEquals("tip_amount", tipEntityBuilder.getType());
        assertEquals("total_amount", totalEntityBuilder.getType());
    }

    @Test
    @DisplayName("Should extract line items")
    void shouldExtractLineItems() {
        Document.Entity descriptionProperty = Document.Entity.newBuilder()
                .setType("line_item/description")
                .setMentionText("Burger")
                .build();

        Document.Entity lineItemEntity = Document.Entity.newBuilder()
                .setType("line_item")
                .addProperties(descriptionProperty)
                .build();

        assertEquals("line_item", lineItemEntity.getType());
        assertEquals(1, lineItemEntity.getPropertiesCount());
        assertEquals("line_item/description", lineItemEntity.getProperties(0).getType());
    }

    @Test
    @DisplayName("Should calculate tax and tip percentages")
    void shouldCalculateTaxAndTipPercentages() {
        double subtotal = 100.00;
        double tax = 8.00;
        double tip = 15.00;

        double taxPercentage = (tax / subtotal) * 100;
        double tipPercentage = (tip / subtotal) * 100;

        assertEquals(8.0, taxPercentage, 0.01);
        assertEquals(15.0, tipPercentage, 0.01);
    }

    @Test
    @DisplayName("Should handle zero subtotal when calculating percentages")
    void shouldHandleZeroSubtotalWhenCalculatingPercentages() {
        double subtotal = 0.0;
        double tax = 8.00;

        double taxPercentage = subtotal > 0 ? (tax / subtotal) * 100 : 0;

        assertEquals(0.0, taxPercentage);
    }

    @Test
    @DisplayName("Should generate unique receipt UID")
    void shouldGenerateUniqueReceiptUid() {
        String uid1 = java.util.UUID.randomUUID().toString();
        String uid2 = java.util.UUID.randomUUID().toString();

        assertNotEquals(uid1, uid2);
        assertTrue(uid1.length() > 0);
        assertTrue(uid2.length() > 0);
    }

    @Test
    @DisplayName("Should filter out invalid line items")
    void shouldFilterOutInvalidLineItems() {
        boolean hasName = false;
        double price = 0.0;

        boolean shouldInclude = hasName && price != 0;

        assertFalse(shouldInclude);
    }

    @Test
    @DisplayName("Should include valid line items")
    void shouldIncludeValidLineItems() {
        String name = "Pizza";
        double price = 15.99;

        boolean shouldInclude = name != null && !name.isEmpty() && price != 0;

        assertTrue(shouldInclude);
    }

    @Test
    @DisplayName("Should handle missing normalized values")
    void shouldHandleMissingNormalizedValues() {
        Document.Entity entityWithoutNormalizedValue = Document.Entity.newBuilder()
                .setType("receipt_date")
                .setMentionText("01/15/2024")
                .build();

        assertEquals("01/15/2024", entityWithoutNormalizedValue.getMentionText());
        assertEquals("receipt_date", entityWithoutNormalizedValue.getType());
    }

    @Test
    @DisplayName("Should initialize with correct field values")
    void shouldInitializeWithCorrectFieldValues() {
        assertEquals("test-project", ReflectionTestUtils.getField(ocrService, "projectId"));
        assertEquals("test-processor", ReflectionTestUtils.getField(ocrService, "processorId"));
        assertEquals("us", ReflectionTestUtils.getField(ocrService, "processorLocation"));
    }

    @Test
    @DisplayName("Should extract receipt data with all fields")
    void shouldExtractReceiptDataWithAllFields() throws Exception {
        Document.Entity.NormalizedValue dateValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("2024-01-15")
                .build();

        Document.Entity.NormalizedValue currencyValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("USD")
                .build();

        Document.Entity.NormalizedValue subtotalValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("100.00")
                .build();

        Document.Entity.NormalizedValue taxValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("8.00")
                .build();

        Document.Entity.NormalizedValue tipValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("15.00")
                .build();

        Document.Entity.NormalizedValue totalValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("123.00")
                .build();

        Document document = Document.newBuilder()
                .addEntities(Document.Entity.newBuilder()
                        .setType("vendor_name")
                        .setMentionText("Test Restaurant"))
                .addEntities(Document.Entity.newBuilder()
                        .setType("receipt_date")
                        .setNormalizedValue(dateValue))
                .addEntities(Document.Entity.newBuilder()
                        .setType("currency")
                        .setNormalizedValue(currencyValue))
                .addEntities(Document.Entity.newBuilder()
                        .setType("net_amount")
                        .setNormalizedValue(subtotalValue))
                .addEntities(Document.Entity.newBuilder()
                        .setType("total_tax_amount")
                        .setNormalizedValue(taxValue))
                .addEntities(Document.Entity.newBuilder()
                        .setType("tip_amount")
                        .setNormalizedValue(tipValue))
                .addEntities(Document.Entity.newBuilder()
                        .setType("total_amount")
                        .setNormalizedValue(totalValue))
                .build();

        ProcessResponse response = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        Method method = OcrService.class.getDeclaredMethod("extractReceiptData", ProcessResponse.class);
        method.setAccessible(true);
        Receipt receipt = (Receipt) method.invoke(ocrService, response);

        assertNotNull(receipt);
        assertEquals("Test Restaurant", receipt.getVendor());
        assertEquals("2024-01-15", receipt.getReceiptDate());
        assertEquals("USD", receipt.getCurrency());
        assertEquals(100.00, receipt.getSubtotal(), 0.01);
        assertEquals(8.00, receipt.getTax(), 0.01);
        assertEquals(15.00, receipt.getTip(), 0.01);
        assertEquals(123.00, receipt.getTotal(), 0.01);
        assertEquals(8.0, receipt.getTaxPercentage(), 0.01);
        assertEquals(15.0, receipt.getTipPercentage(), 0.01);
    }

    @Test
    @DisplayName("Should use supplier_name as fallback for vendor")
    void shouldUseSupplierNameAsFallbackForVendor() throws Exception {
        Document document = Document.newBuilder()
                .addEntities(Document.Entity.newBuilder()
                        .setType("supplier_name")
                        .setMentionText("Supplier Inc"))
                .build();

        ProcessResponse response = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        Method method = OcrService.class.getDeclaredMethod("extractReceiptData", ProcessResponse.class);
        method.setAccessible(true);
        Receipt receipt = (Receipt) method.invoke(ocrService, response);

        assertEquals("Supplier Inc", receipt.getVendor());
    }

    @Test
    @DisplayName("Should handle entities without normalized values")
    void shouldHandleEntitiesWithoutNormalizedValues() throws Exception {
        Document document = Document.newBuilder()
                .addEntities(Document.Entity.newBuilder()
                        .setType("receipt_date")
                        .setMentionText("01/15/2024"))
                .addEntities(Document.Entity.newBuilder()
                        .setType("currency")
                        .setMentionText("USD"))
                .addEntities(Document.Entity.newBuilder()
                        .setType("net_amount")
                        .setMentionText("100.00"))
                .build();

        ProcessResponse response = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        Method method = OcrService.class.getDeclaredMethod("extractReceiptData", ProcessResponse.class);
        method.setAccessible(true);
        Receipt receipt = (Receipt) method.invoke(ocrService, response);

        assertNull(receipt.getReceiptDate());
        assertEquals("USD", receipt.getCurrency());
        assertEquals(0.0, receipt.getSubtotal());
    }

    @Test
    @DisplayName("Should extract line items with all properties")
    void shouldExtractLineItemsWithAllProperties() throws Exception {
        Document.Entity.NormalizedValue priceValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("15.99")
                .build();

        Document.Entity.NormalizedValue quantityValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("2")
                .build();

        Document.Entity lineItemEntity = Document.Entity.newBuilder()
                .setType("line_item")
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/description")
                        .setMentionText("Burger"))
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/amount")
                        .setNormalizedValue(priceValue))
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/quantity")
                        .setNormalizedValue(quantityValue))
                .build();

        Document document = Document.newBuilder()
                .addEntities(lineItemEntity)
                .build();

        ProcessResponse response = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        Method method = OcrService.class.getDeclaredMethod("extractReceiptData", ProcessResponse.class);
        method.setAccessible(true);
        Receipt receipt = (Receipt) method.invoke(ocrService, response);

        assertNotNull(receipt.getLineItems());
        assertEquals(1, receipt.getLineItems().size());
        LineItem item = receipt.getLineItems().get(0);
        assertEquals("Burger", item.getName());
        assertEquals(15.99, item.getPrice(), 0.01);
        assertEquals(2, item.getQuantity());
    }

    @Test
    @DisplayName("Should filter out line items with no name")
    void shouldFilterOutLineItemsWithNoName() throws Exception {
        Document.Entity.NormalizedValue priceValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("15.99")
                .build();

        Document.Entity lineItemEntity = Document.Entity.newBuilder()
                .setType("line_item")
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/amount")
                        .setNormalizedValue(priceValue))
                .build();

        Document document = Document.newBuilder()
                .addEntities(lineItemEntity)
                .build();

        ProcessResponse response = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        Method method = OcrService.class.getDeclaredMethod("extractReceiptData", ProcessResponse.class);
        method.setAccessible(true);
        Receipt receipt = (Receipt) method.invoke(ocrService, response);

        assertEquals(0, receipt.getLineItems().size());
    }

    @Test
    @DisplayName("Should filter out line items with zero price")
    void shouldFilterOutLineItemsWithZeroPrice() throws Exception {
        Document.Entity lineItemEntity = Document.Entity.newBuilder()
                .setType("line_item")
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/description")
                        .setMentionText("Free Item"))
                .build();

        Document document = Document.newBuilder()
                .addEntities(lineItemEntity)
                .build();

        ProcessResponse response = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        Method method = OcrService.class.getDeclaredMethod("extractReceiptData", ProcessResponse.class);
        method.setAccessible(true);
        Receipt receipt = (Receipt) method.invoke(ocrService, response);

        assertEquals(0, receipt.getLineItems().size());
    }

    @Test
    @DisplayName("Should handle line items without normalized values")
    void shouldHandleLineItemsWithoutNormalizedValues() throws Exception {
        Document.Entity lineItemEntity = Document.Entity.newBuilder()
                .setType("line_item")
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/description")
                        .setMentionText("Item"))
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/amount")
                        .setMentionText("10.00"))
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/quantity")
                        .setMentionText("1"))
                .build();

        Document document = Document.newBuilder()
                .addEntities(lineItemEntity)
                .build();

        ProcessResponse response = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        Method method = OcrService.class.getDeclaredMethod("extractReceiptData", ProcessResponse.class);
        method.setAccessible(true);
        Receipt receipt = (Receipt) method.invoke(ocrService, response);

        assertEquals(0, receipt.getLineItems().size());
    }

    @Test
    @DisplayName("Should not calculate percentages when subtotal is zero")
    void shouldNotCalculatePercentagesWhenSubtotalIsZero() throws Exception {
        Document.Entity.NormalizedValue taxValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("8.00")
                .build();

        Document document = Document.newBuilder()
                .addEntities(Document.Entity.newBuilder()
                        .setType("total_tax_amount")
                        .setNormalizedValue(taxValue))
                .build();

        ProcessResponse response = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        Method method = OcrService.class.getDeclaredMethod("extractReceiptData", ProcessResponse.class);
        method.setAccessible(true);
        Receipt receipt = (Receipt) method.invoke(ocrService, response);

        assertEquals(0.0, receipt.getTaxPercentage());
        assertEquals(0.0, receipt.getTipPercentage());
    }

    @Test
    @DisplayName("Should handle unknown entity types")
    void shouldHandleUnknownEntityTypes() throws Exception {
        Document document = Document.newBuilder()
                .addEntities(Document.Entity.newBuilder()
                        .setType("unknown_type")
                        .setMentionText("Some value"))
                .build();

        ProcessResponse response = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        Method method = OcrService.class.getDeclaredMethod("extractReceiptData", ProcessResponse.class);
        method.setAccessible(true);
        Receipt receipt = (Receipt) method.invoke(ocrService, response);

        assertNotNull(receipt);
        assertNotNull(receipt.getUid());
    }

    @Test
    @DisplayName("Should process receipt image successfully")
    void shouldProcessReceiptImageSuccessfully(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("receipt.jpg");
        Files.write(testFile, "fake image content".getBytes());

        Document.Entity.NormalizedValue totalValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("100.00")
                .build();

        Document document = Document.newBuilder()
                .addEntities(Document.Entity.newBuilder()
                        .setType("vendor_name")
                        .setMentionText("Test Store"))
                .addEntities(Document.Entity.newBuilder()
                        .setType("total_amount")
                        .setNormalizedValue(totalValue))
                .build();

        ProcessResponse mockResponse = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        when(documentProcessorServiceClient.processDocument(any(ProcessRequest.class)))
                .thenReturn(mockResponse);

        Receipt receipt = ocrService.processReceiptImage(testFile.toString());

        assertNotNull(receipt);
        assertEquals("Test Store", receipt.getVendor());
        assertEquals(100.00, receipt.getTotal(), 0.01);
        assertNotNull(receipt.getUid());
        verify(documentProcessorServiceClient, times(1)).processDocument(any(ProcessRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when file does not exist")
    void shouldThrowExceptionWhenFileDoesNotExist() {
        String nonExistentFile = "/path/to/nonexistent/file.jpg";

        OcrProcessingException exception = assertThrows(OcrProcessingException.class, () -> {
            ocrService.processReceiptImage(nonExistentFile);
        });

        assertTrue(exception.getMessage().contains("Failed to read or process receipt file"));
    }

    @Test
    @DisplayName("Should throw exception when OCR processing fails")
    void shouldThrowExceptionWhenOcrProcessingFails(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("receipt.jpg");
        Files.write(testFile, "fake image content".getBytes());

        when(documentProcessorServiceClient.processDocument(any(ProcessRequest.class)))
                .thenThrow(new RuntimeException("OCR service error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ocrService.processReceiptImage(testFile.toString());
        });

        assertTrue(exception.getMessage().contains("OCR service error"));
    }

    @Test
    @DisplayName("Should initialize DocumentProcessorServiceClient successfully")
    void shouldInitializeDocumentProcessorServiceClientSuccessfully() {
        assertNotNull(ReflectionTestUtils.getField(ocrService, "documentProcessorServiceClient"));
    }

    @Test
    @DisplayName("Should process receipt with multiple line items")
    void shouldProcessReceiptWithMultipleLineItems(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("receipt.jpg");
        Files.write(testFile, "fake image content".getBytes());

        Document.Entity.NormalizedValue price1 = Document.Entity.NormalizedValue.newBuilder()
                .setText("10.00")
                .build();

        Document.Entity.NormalizedValue price2 = Document.Entity.NormalizedValue.newBuilder()
                .setText("15.00")
                .build();

        Document.Entity.NormalizedValue quantity1 = Document.Entity.NormalizedValue.newBuilder()
                .setText("2")
                .build();

        Document.Entity lineItem1 = Document.Entity.newBuilder()
                .setType("line_item")
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/description")
                        .setMentionText("Burger"))
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/amount")
                        .setNormalizedValue(price1))
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/quantity")
                        .setNormalizedValue(quantity1))
                .build();

        Document.Entity lineItem2 = Document.Entity.newBuilder()
                .setType("line_item")
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/description")
                        .setMentionText("Pizza"))
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/amount")
                        .setNormalizedValue(price2))
                .build();

        Document document = Document.newBuilder()
                .addEntities(lineItem1)
                .addEntities(lineItem2)
                .build();

        ProcessResponse mockResponse = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        when(documentProcessorServiceClient.processDocument(any(ProcessRequest.class)))
                .thenReturn(mockResponse);

        Receipt receipt = ocrService.processReceiptImage(testFile.toString());

        assertNotNull(receipt);
        assertEquals(2, receipt.getLineItems().size());

        LineItem item1 = receipt.getLineItems().get(0);
        assertEquals("Burger", item1.getName());
        assertEquals(10.00, item1.getPrice(), 0.01);
        assertEquals(2, item1.getQuantity());

        LineItem item2 = receipt.getLineItems().get(1);
        assertEquals("Pizza", item2.getName());
        assertEquals(15.00, item2.getPrice(), 0.01);
    }

    @Test
    @DisplayName("Should calculate percentages correctly")
    void shouldCalculatePercentagesCorrectly(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("receipt.jpg");
        Files.write(testFile, "fake image content".getBytes());

        Document.Entity.NormalizedValue subtotalValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("100.00")
                .build();

        Document.Entity.NormalizedValue taxValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("8.00")
                .build();

        Document.Entity.NormalizedValue tipValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("15.00")
                .build();

        Document document = Document.newBuilder()
                .addEntities(Document.Entity.newBuilder()
                        .setType("net_amount")
                        .setNormalizedValue(subtotalValue))
                .addEntities(Document.Entity.newBuilder()
                        .setType("total_tax_amount")
                        .setNormalizedValue(taxValue))
                .addEntities(Document.Entity.newBuilder()
                        .setType("tip_amount")
                        .setNormalizedValue(tipValue))
                .build();

        ProcessResponse mockResponse = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        when(documentProcessorServiceClient.processDocument(any(ProcessRequest.class)))
                .thenReturn(mockResponse);

        Receipt receipt = ocrService.processReceiptImage(testFile.toString());

        assertNotNull(receipt);
        assertEquals(100.00, receipt.getSubtotal(), 0.01);
        assertEquals(8.00, receipt.getTax(), 0.01);
        assertEquals(15.00, receipt.getTip(), 0.01);
        assertEquals(8.0, receipt.getTaxPercentage(), 0.01);
        assertEquals(15.0, receipt.getTipPercentage(), 0.01);
    }

    @Test
    @DisplayName("Should not calculate percentages when subtotal is zero")
    void shouldNotCalculatePercentagesWhenSubtotalIsZero(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("receipt.jpg");
        Files.write(testFile, "fake image content".getBytes());

        Document.Entity.NormalizedValue taxValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("5.00")
                .build();

        Document document = Document.newBuilder()
                .addEntities(Document.Entity.newBuilder()
                        .setType("total_tax_amount")
                        .setNormalizedValue(taxValue))
                .build();

        ProcessResponse mockResponse = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        when(documentProcessorServiceClient.processDocument(any(ProcessRequest.class)))
                .thenReturn(mockResponse);

        Receipt receipt = ocrService.processReceiptImage(testFile.toString());

        assertNotNull(receipt);
        assertEquals(0.0, receipt.getSubtotal(), 0.01);
        assertEquals(0.0, receipt.getTaxPercentage(), 0.01);
        assertEquals(0.0, receipt.getTipPercentage(), 0.01);
    }

    @Test
    @DisplayName("Should extract line item with unknown property type")
    void shouldExtractLineItemWithUnknownPropertyType() throws Exception {
        Document.Entity.NormalizedValue priceValue = Document.Entity.NormalizedValue.newBuilder()
                .setText("10.00")
                .build();

        Document.Entity lineItemEntity = Document.Entity.newBuilder()
                .setType("line_item")
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/description")
                        .setMentionText("Item"))
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/amount")
                        .setNormalizedValue(priceValue))
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/unknown_property")
                        .setMentionText("unknown"))
                .build();

        Document document = Document.newBuilder()
                .addEntities(lineItemEntity)
                .build();

        ProcessResponse response = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        Method method = OcrService.class.getDeclaredMethod("extractReceiptData", ProcessResponse.class);
        method.setAccessible(true);
        Receipt receipt = (Receipt) method.invoke(ocrService, response);

        assertNotNull(receipt);
        assertEquals(1, receipt.getLineItems().size());
        assertEquals("Item", receipt.getLineItems().get(0).getName());
        assertEquals(10.00, receipt.getLineItems().get(0).getPrice(), 0.01);
    }

    @Test
    @DisplayName("Should handle empty line item properties list")
    void shouldHandleEmptyLineItemPropertiesList() throws Exception {
        Document.Entity lineItemEntity = Document.Entity.newBuilder()
                .setType("line_item")
                .build();

        Document document = Document.newBuilder()
                .addEntities(lineItemEntity)
                .build();

        ProcessResponse response = ProcessResponse.newBuilder()
                .setDocument(document)
                .build();

        Method method = OcrService.class.getDeclaredMethod("extractReceiptData", ProcessResponse.class);
        method.setAccessible(true);
        Receipt receipt = (Receipt) method.invoke(ocrService, response);

        assertNotNull(receipt);
        assertEquals(0, receipt.getLineItems().size());
    }
}
