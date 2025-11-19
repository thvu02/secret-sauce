package org.splittydupe.startup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.splittydupe.startup.TestConfig;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.service.JwtService;
import org.splittydupe.startup.service.OcrService;
import org.splittydupe.startup.service.PdfReportService;
import org.splittydupe.startup.service.ReceiptService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReceiptController.class)
@DisplayName("Receipt Controller Tests")
class ReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OcrService ocrService;

    @MockBean
    private ReceiptService receiptService;

    @MockBean
    private PdfReportService pdfReportService;

    @MockBean
    private JwtService jwtService;

    private Receipt testReceipt;

    @BeforeEach
    void setUp() {
        testReceipt = TestConfig.createTestReceipt();
    }

    @Test
    @WithMockUser
    @DisplayName("Should upload and parse receipt successfully")
    void shouldUploadAndParseReceiptSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        when(ocrService.processReceiptImage(anyString())).thenReturn(testReceipt);

        mockMvc.perform(multipart("/api/ocr/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value(testReceipt.getUid()))
                .andExpect(jsonPath("$.vendor").value("Test Restaurant"))
                .andExpect(jsonPath("$.total").value(123.50));

        verify(ocrService, times(1)).processReceiptImage(anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when no file provided")
    void shouldReturn400WhenNoFileProvided() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/ocr/upload")
                        .file(emptyFile)
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(ocrService, never()).processReceiptImage(anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 500 when OCR processing fails")
    void shouldReturn500WhenOcrProcessingFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        when(ocrService.processReceiptImage(anyString()))
                .thenThrow(new RuntimeException("OCR failed"));

        mockMvc.perform(multipart("/api/ocr/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());

        verify(ocrService, times(1)).processReceiptImage(anyString());
    }

    @Test
    @WithMockUser
    @DisplayName("Should save receipt successfully")
    void shouldSaveReceiptSuccessfully() throws Exception {
        when(receiptService.saveReceipt(any(Receipt.class), isNull())).thenReturn(true);

        mockMvc.perform(post("/api/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReceipt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));

        verify(receiptService, times(1)).saveReceipt(any(Receipt.class), isNull());
    }

    @Test
    @WithMockUser
    @DisplayName("Should save receipt with authenticated user")
    void shouldSaveReceiptWithAuthenticatedUser() throws Exception {
        String authHeader = "Bearer validtoken";
        when(jwtService.extractUsername(anyString())).thenReturn(TestConfig.TEST_USER_EMAIL);
        when(receiptService.saveReceipt(any(Receipt.class), eq(TestConfig.TEST_USER_EMAIL))).thenReturn(true);

        mockMvc.perform(post("/api/receipts")
                        .with(csrf())
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReceipt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));

        // Note: extractUsername is called twice - once by JWT filter and once by controller
        verify(jwtService, times(2)).extractUsername("validtoken");
        verify(receiptService, times(1)).saveReceipt(any(Receipt.class), eq(TestConfig.TEST_USER_EMAIL));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when receipt is null")
    void shouldReturn400WhenReceiptIsNull() throws Exception {
        mockMvc.perform(post("/api/receipts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest());

        verify(receiptService, never()).saveReceipt(any(), any());
    }

    @Test
    @WithMockUser
    @DisplayName("Should generate PDF report successfully")
    void shouldGeneratePdfReportSuccessfully() throws Exception {
        byte[] pdfBytes = "PDF content".getBytes();
        when(receiptService.saveReceipt(any(Receipt.class), isNull())).thenReturn(true);
        when(pdfReportService.generateReceiptReport(any(Receipt.class))).thenReturn(pdfBytes);

        mockMvc.perform(post("/api/receipts/generate-report")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReceipt)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(pdfBytes));

        verify(receiptService, times(1)).saveReceipt(any(Receipt.class), isNull());
        verify(pdfReportService, times(1)).generateReceiptReport(any(Receipt.class));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 500 when PDF generation fails")
    void shouldReturn500WhenPdfGenerationFails() throws Exception {
        when(receiptService.saveReceipt(any(Receipt.class), isNull())).thenReturn(true);
        when(pdfReportService.generateReceiptReport(any(Receipt.class)))
                .thenThrow(new RuntimeException("PDF generation failed"));

        mockMvc.perform(post("/api/receipts/generate-report")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testReceipt)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());

        verify(pdfReportService, times(1)).generateReceiptReport(any(Receipt.class));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when receipt is null for PDF generation")
    void shouldReturn400WhenReceiptIsNullForPdfGeneration() throws Exception {
        mockMvc.perform(post("/api/receipts/generate-report")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest());

        verify(receiptService, never()).saveReceipt(any(), any());
        verify(pdfReportService, never()).generateReceiptReport(any());
    }
}
