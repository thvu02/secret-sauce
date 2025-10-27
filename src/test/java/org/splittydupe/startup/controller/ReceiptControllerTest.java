package org.splittydupe.startup.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.splittydupe.startup.Database.ReceiptTableDAL;
import org.splittydupe.startup.Database.Receipt;
import org.splittydupe.startup.Database.LineItem;
import org.splittydupe.startup.ImageParser.OCRService;
import org.splittydupe.startup.Payment.PaymentService;
import org.splittydupe.startup.Controller.ReceiptController;


import java.util.Map;
import java.util.List;
import java.util.HashMap;

public class ReceiptControllerTest {

    @Mock
    private OCRService ocrService;

    @Mock
    private ReceiptTableDAL receiptTableDAL;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private ReceiptController receiptController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testUploadAndParse_noFileProvided() {
        ResponseEntity<?> response = receiptController.uploadAndParse(null);
        assertEquals(400, response.getStatusCodeValue());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("No file provided", body.get("error"));
    }

    @Test
    public void testUploadAndParse_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", "dummy data".getBytes());

        when(ocrService.wrapper(anyString())).thenReturn(new Receipt());

        ResponseEntity<?> response = receiptController.uploadAndParse(file);
        assertEquals(200, response.getStatusCodeValue());
        verify(ocrService).wrapper(anyString());
    }

    @Test
    public void testSaveReceipt_success() {
        Receipt receipt = new Receipt();
        when(receiptTableDAL.saveReceipt(receipt)).thenReturn(true);

        ResponseEntity<?> response = receiptController.saveReceipt(receipt);
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Boolean> body = (Map<String, Boolean>) response.getBody();
        assertTrue(body.get("saved"));
    }

    @Test
    public void testSaveReceipt_failure() {
        Receipt receipt = new Receipt();
        when(receiptTableDAL.saveReceipt(receipt)).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = receiptController.saveReceipt(receipt);
        assertEquals(500, response.getStatusCodeValue());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertTrue(body.get("error").contains("DB error"));
    }

    @Test
    public void testComputeVenmoRequests_nullReceipt() {
        ResponseEntity<?> response = receiptController.computeVenmoRequests(null);
        assertEquals(400, response.getStatusCodeValue());
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Receipt is required", body.get("error"));
    }

    @Test
    public void testComputeVenmoRequests_success() {
        Receipt receipt = new Receipt();
        receipt.setVendor("TestVendor");
        LineItem li = new LineItem();
        li.setPrice(10.0);
        li.setQuantity(2);
        li.setAssignees(List.of("user1", "user2"));
        receipt.setLineItems(List.of(li));
        receipt.setSubtotal(20.0);
        receipt.setTax(1.6);
        receipt.setTip(1.4);

        Map<String, Object> fakePaymentResult = new HashMap<>();
        fakePaymentResult.put("status", "success");

        when(paymentService.requestPayment(anyString(), any(), anyString())).thenReturn(fakePaymentResult);

        ResponseEntity<?> response = receiptController.computeVenmoRequests(receipt);
        assertEquals(200, response.getStatusCodeValue());
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody();
        assertNotNull(results);
        assertFalse(results.isEmpty());
        Map<String, Object> first = results.get(0);
        assertEquals("user1", first.get("assignee"));
        assertEquals("venmo://paycharge?txn=pay&recipients=user1&amount=11.00", first.get("venmoApp"));
    }
}