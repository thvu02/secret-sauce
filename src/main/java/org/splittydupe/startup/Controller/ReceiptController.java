package org.splittydupe.startup.Controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CrossOrigin;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import org.splittydupe.startup.ImageParser.OCRService;
import org.splittydupe.startup.Database.ReceiptTableDAL;
import org.splittydupe.startup.Database.Receipt;
import org.splittydupe.startup.Payment.PaymentService;
import org.splittydupe.startup.Database.LineItem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class ReceiptController {

    @Autowired
    private OCRService ocrService;

    @Autowired
    private ReceiptTableDAL receiptTableDAL;

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/ocr/upload")
    public ResponseEntity<?> uploadAndParse(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "No file provided"));
        }

        try {
            String original = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
            String suffix = "";
            int idx = original.lastIndexOf('.');
            if (idx != -1) suffix = original.substring(idx);
            Path tempFile = Files.createTempFile("receipt-upload-", suffix);
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            Receipt parsed = ocrService.wrapper(tempFile.toString());

            return ResponseEntity.ok(parsed);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Failed to process file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @PostMapping("/receipts")
    public ResponseEntity<?> saveReceipt(@RequestBody Receipt receipt) {        
        try {
            boolean ok = receiptTableDAL.saveReceipt(receipt);
            System.out.println("Receipt saved successfully with UID: " + receipt.getUid());
            return ResponseEntity.ok(Map.of("saved", ok));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body(Map.of("error", "Failed to save receipt: " + e.getMessage()));
        }
    }

    @PostMapping("/venmo/requests")
    public ResponseEntity<?> computeVenmoRequests(@RequestBody Receipt receipt) {
        try {
            if (receipt == null) return ResponseEntity.badRequest().body(Map.of("error", "Receipt is required"));

            Map<String, Double> subtotals = new HashMap<>();

            double computedSubtotal = 0.0;
            if (receipt.getLineItems() != null) {
                for (LineItem li : receipt.getLineItems()) {
                    double itemTotal = li.getPrice() * li.getQuantity();
                    computedSubtotal += itemTotal;
                    List<String> ass = li.getAssignees();
                    int n = (ass == null) ? 0 : ass.size();
                    if (n <= 0) continue;
                    double share = itemTotal / n;
                    if (ass != null) {
                        for (int ai = 0; ai < ass.size(); ai++) {
                            String a = ass.get(ai);
                            subtotals.put(a, subtotals.getOrDefault(a, 0.0) + share);
                        }
                    }
                }
            }

            double baseSubtotal = (receipt.getSubtotal() > 0) ? receipt.getSubtotal() : computedSubtotal;

            double tax = receipt.getTax();
            double tip = receipt.getTip();

            List<Map<String, Object>> results = new ArrayList<>();

            for (Map.Entry<String, Double> e : subtotals.entrySet()) {
                String assignee = e.getKey();
                double personSubtotal = e.getValue();
                double taxShare = (baseSubtotal > 0) ? (personSubtotal / baseSubtotal) * tax : 0.0;
                double tipShare = (baseSubtotal > 0) ? (personSubtotal / baseSubtotal) * tip : 0.0;
                double amount = personSubtotal + taxShare + tipShare;
                BigDecimal bdAmount = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_EVEN);

                String handle = assignee;
                String note = String.format("Receipt payment request from %s", receipt.getVendor());
                
                Map<String, Object> paymentResult = paymentService.requestPayment(handle, bdAmount, note);

                // For UI/backup, also generate direct Venmo links
                String venmoApp = String.format("venmo://paycharge?txn=pay&recipients=%s&amount=%.2f", handle, amount);
                String venmoWeb = String.format("https://venmo.com/%s?txn=pay&amount=%.2f", handle, amount);

                Map<String, Object> item = new HashMap<>();
                item.put("assignee", assignee);
                item.put("amount", bdAmount);
                item.put("venmoApp", venmoApp);
                item.put("venmoWeb", venmoWeb);
                item.put("paymentResult", paymentResult);
                results.add(item);
            }

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
