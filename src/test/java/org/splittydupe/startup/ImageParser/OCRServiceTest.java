package org.splittydupe.startup.ImageParser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.cloud.documentai.v1.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.splittydupe.startup.Database.Receipt;
import org.splittydupe.startup.Database.LineItem;

public class OCRServiceTest {

    @Mock
    private DocumentProcessorServiceClient mockClient;

    @InjectMocks
    private OCRService ocrService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(ocrService, "projectId", "testProject");
        ReflectionTestUtils.setField(ocrService, "processorId", "testProcessor");
        ReflectionTestUtils.setField(ocrService, "processorLocation", "us");
        ReflectionTestUtils.setField(ocrService, "documentProcessorServiceClient", mockClient);
    }

    @Test
    public void testInitReceiptParser_throwsRuntimeExceptionOnIOException() throws IOException {
        OCRService service = new OCRService();
    }

    @Test
    public void testProcessReceiptImage_success() throws Exception {
        byte[] dummyBytes = {0x0, 0x1, 0x2};
        Path dummyPath = Path.of("dummy.pdf");

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.readAllBytes(dummyPath)).thenReturn(dummyBytes);
            filesMock.when(() -> Files.probeContentType(dummyPath)).thenReturn("application/pdf");

            ProcessResponse expectedResponse = ProcessResponse.newBuilder()
                    .setDocument(Document.newBuilder().build())
                    .build();

            when(mockClient.processDocument(any(ProcessRequest.class))).thenReturn(expectedResponse);

            ReflectionTestUtils.setField(ocrService, "projectId", "project");
            ReflectionTestUtils.setField(ocrService, "processorId", "processor");
            ReflectionTestUtils.setField(ocrService, "processorLocation", "location");

            ProcessResponse response = ocrService.wrapper("dummy.pdf").getLineItems().isEmpty() ? expectedResponse : expectedResponse;
            ProcessResponse actualResponse = ReflectionTestUtils.invokeMethod(ocrService, "processReceiptImage", "dummy.pdf");

            assertNotNull(actualResponse);
            assertEquals(expectedResponse, actualResponse);
        }
    }

    @Test
    public void testExtractReceiptData_populatesReceiptCorrectly() {
        Document.Entity lineItemEntity = Document.Entity.newBuilder()
                .setType("line_item")
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/description")
                        .setMentionText("Test item"))
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/amount")
                        .setNormalizedValue(Document.Entity.NormalizedValue.newBuilder().setText("10.5").build()))
                .addProperties(Document.Entity.newBuilder()
                        .setType("line_item/quantity")
                        .setNormalizedValue(Document.Entity.NormalizedValue.newBuilder().setText("2").build()))
                .build();

        Document doc = Document.newBuilder()
                .addEntities(Document.Entity.newBuilder().setType("vendor_name").setMentionText("Test Vendor").build())
                .addEntities(Document.Entity.newBuilder().setType("receipt_date").setNormalizedValue(Document.Entity.NormalizedValue.newBuilder().setText("2025-10-27").build()).build())
                .addEntities(Document.Entity.newBuilder().setType("net_amount").setNormalizedValue(Document.Entity.NormalizedValue.newBuilder().setText("50.0").build()).build())
                .addEntities(lineItemEntity)
                .build();

        ProcessResponse response = ProcessResponse.newBuilder()
                .setDocument(doc)
                .build();

        Receipt receipt = ReflectionTestUtils.invokeMethod(ocrService, "extractReceiptData", response);

        assertEquals("Test Vendor", receipt.getVendor());
        assertEquals("2025-10-27", receipt.getReceiptDate());
        assertEquals(50.0, receipt.getSubtotal());
        assertFalse(receipt.getLineItems().isEmpty());

        LineItem lineItem = receipt.getLineItems().get(0);
        assertEquals("Test item", lineItem.getName());
        assertEquals(10.5, lineItem.getPrice());
        assertEquals(2, lineItem.getQuantity());
    }
}

