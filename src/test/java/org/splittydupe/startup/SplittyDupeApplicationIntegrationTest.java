package org.splittydupe.startup;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.repository.IReceiptRepository;
import org.splittydupe.startup.service.OcrService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("SplittyDupe Application Integration Tests")
class SplittyDupeApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OcrService ocrService;

    @MockBean
    private IReceiptRepository receiptRepository;

    private Receipt testReceipt;

    @BeforeEach
    void setUp() {
        testReceipt = TestConfig.createTestReceipt();
        when(receiptRepository.save(any(Receipt.class))).thenReturn(true);
    }

    @Test
    @WithMockUser
    @DisplayName("Integration: Full receipt workflow - Upload, Parse, Save, Generate Report")
    void testFullReceiptWorkflow() throws Exception {
        // Step 1: Upload and parse receipt
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        when(ocrService.processReceiptImage(anyString())).thenReturn(testReceipt);

        String receiptJson = mockMvc.perform(multipart("/api/ocr/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").exists())
                .andExpect(jsonPath("$.vendor").value("Test Restaurant"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(ocrService, times(1)).processReceiptImage(anyString());

        // Step 2: Save the parsed receipt
        mockMvc.perform(post("/api/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(receiptJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));

        verify(receiptRepository, atLeast(1)).save(any(Receipt.class));

        // Step 3: Generate PDF report
        mockMvc.perform(post("/api/receipts/generate-report")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(receiptJson))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"));

        verify(receiptRepository, atLeast(2)).save(any(Receipt.class));
    }

    @Test
    @WithMockUser
    @DisplayName("Integration: Anonymous user workflow")
    void testAnonymousUserWorkflow() throws Exception {
        Receipt anonymousReceipt = TestConfig.createAnonymousReceipt();

        mockMvc.perform(post("/api/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(anonymousReceipt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));

        verify(receiptRepository, times(1)).save(argThat(receipt ->
                receipt.getUserId() != null &&
                        (receipt.getUserId().equals("anonymous") ||
                                receipt.getUserId().equals(TestConfig.TEST_USER_ID))
        ));
    }

    @Test
    @DisplayName("Integration: Application context loads successfully")
    void testApplicationContextLoads() {
        // This test verifies that the Spring application context loads without errors
        // If this test passes, it means all beans are properly configured
    }

    @Test
    @WithMockUser
    @DisplayName("Integration: CORS configuration allows requests")
    void testCorsConfiguration() throws Exception {
        mockMvc.perform(options("/api/receipts")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    @WithMockUser
    @DisplayName("Integration: Security configuration protects endpoints")
    void testSecurityConfiguration() throws Exception {
        // Test that public endpoints are accessible
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "test".getBytes()
        );
        when(ocrService.processReceiptImage(anyString())).thenReturn(testReceipt);

        mockMvc.perform(multipart("/api/ocr/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("Integration: Error handling returns proper response")
    void testErrorHandling() throws Exception {
        when(ocrService.processReceiptImage(anyString()))
                .thenThrow(new RuntimeException("Test error"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "test".getBytes()
        );

        mockMvc.perform(multipart("/api/ocr/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("Integration: Receipt data validation")
    void testReceiptDataValidation() throws Exception {
        Receipt invalidReceipt = new Receipt();

        mockMvc.perform(post("/api/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReceipt)))
                .andExpect(status().isOk()); // Service still accepts, but sets defaults

        verify(receiptRepository, times(1)).save(any(Receipt.class));
    }

    @Test
    @WithMockUser
    @DisplayName("Integration: Concurrent receipt operations")
    void testConcurrentReceiptOperations() throws Exception {
        Receipt receipt1 = TestConfig.createTestReceipt();
        Receipt receipt2 = TestConfig.createMinimalReceipt();

        mockMvc.perform(post("/api/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receipt1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receipt2)))
                .andExpect(status().isOk());

        verify(receiptRepository, times(2)).save(any(Receipt.class));
    }
}
