package org.splittydupe.startup.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.splittydupe.startup.dto.ErrorResponse;
import org.splittydupe.startup.dto.SaveReceiptResponse;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.service.JwtService;
import org.splittydupe.startup.service.OcrService;
import org.splittydupe.startup.service.PdfReportService;
import org.splittydupe.startup.service.ReceiptService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReceiptController {

    private final OcrService ocrService;
    private final ReceiptService receiptService;
    private final PdfReportService pdfReportService;
    private final JwtService jwtService;

    @PostMapping("/ocr/upload")
    public ResponseEntity<?> uploadAndParse(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("Upload request received with no file");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("No file provided"));
        }

        try {
            String original = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
            String suffix = "";
            int idx = original.lastIndexOf('.');
            if (idx != -1) {
                suffix = original.substring(idx);
            }

            Path tempFile = Files.createTempFile("receipt-upload-", suffix);
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            log.info("Processing uploaded file: {}", original);
            Receipt parsed = ocrService.processReceiptImage(tempFile.toString());

            return ResponseEntity.ok(parsed);
        } catch (Exception e) {
            log.error("Failed to process uploaded file", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to process file: " + e.getMessage()));
        }
    }

    @PostMapping("/receipts")
    public ResponseEntity<?> saveReceipt(
            @RequestBody Receipt receipt,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (receipt == null) {
            log.warn("Save request received with null receipt");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Receipt is required"));
        }

        try {
            String userId = extractUserIdFromHeader(authHeader);

            boolean saved = receiptService.saveReceipt(receipt, userId);
            log.info("Receipt saved successfully with UID: {}, userId: {}", receipt.getUid(), userId);
            return ResponseEntity.ok(new SaveReceiptResponse(saved));
        } catch (Exception e) {
            log.error("Failed to save receipt with UID: {}", receipt.getUid(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to save receipt: " + e.getMessage()));
        }
    }

    @PostMapping("/receipts/generate-report")
    public ResponseEntity<?> generateReport(
            @RequestBody Receipt receipt,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (receipt == null) {
            log.warn("Generate report request received with null receipt");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Receipt is required"));
        }

        try {
            String userId = extractUserIdFromHeader(authHeader);

            receiptService.saveReceipt(receipt, userId);
            log.info("Receipt saved before generating report. UID: {}, userId: {}", receipt.getUid(), userId);

            byte[] pdfBytes = pdfReportService.generateReceiptReport(receipt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                "receipt-report-" + receipt.getUid() + ".pdf");
            headers.setContentLength(pdfBytes.length);

            log.info("PDF report generated successfully for receipt UID: {}", receipt.getUid());
            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Failed to generate report for receipt UID: {}", receipt.getUid(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to generate report: " + e.getMessage()));
        }
    }

    private String extractUserIdFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                return jwtService.extractUsername(token); // Using email as userId
            } catch (Exception e) {
                log.warn("Failed to extract userId from token", e);
            }
        }
        return null; // Will be set to "anonymous" in service layer
    }
}
