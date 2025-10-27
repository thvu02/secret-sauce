package org.splittydupe.startup.Payment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.ClientTokenRequest;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;

import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
import java.util.List;
import java.util.Arrays;

@Service
public class PaymentService {
    
    @Value("${payment_test_mode:true}")
    private boolean testMode;

    @Value("${payment_sandbox_accounts:testuser1,testuser2,testuser3}")
    private String sandboxAccounts;

    @Autowired
    private BraintreeGateway braintreeGateway;

    private List<String> getTestAccounts() {
        return Arrays.asList(sandboxAccounts.split(","));
    }

    public Map<String, Object> requestPayment(String requesterHandle, BigDecimal amount, String note) {
        Map<String, Object> response = new HashMap<>();

        if (testMode) {
            List<String> testAccounts = getTestAccounts();
            if (!testAccounts.contains(requesterHandle)) {
                response.put("success", false);
                response.put("error", "In test mode, only test accounts are allowed: " + String.join(", ", testAccounts));
                return response;
            }

            response.put("success", true);
            response.put("transactionId", "TEST-" + System.currentTimeMillis());
            response.put("status", "SUBMITTED_FOR_SETTLEMENT");
            response.put("testMode", true);
            return response;
        }

        try {
            TransactionRequest request = new TransactionRequest()
                .amount(amount)
                .paymentMethodNonce(requesterHandle)
                .options()
                    .submitForSettlement(true)
                    .done();

            if (note != null && !note.isEmpty()) {
                request.customField("note", note);
            }

            Result<Transaction> result = braintreeGateway.transaction().sale(request);
            
            if (result.isSuccess()) {
                Transaction transaction = result.getTarget();
                response.put("success", true);
                response.put("transactionId", transaction.getId());
                response.put("status", transaction.getStatus());
            } else {
                response.put("success", false);
                response.put("errors", result.getErrors());
                response.put("message", "Payment request failed: " + result.getMessage());
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        response.put("testMode", false);
        return response;
    }

    public String generateClientToken() {
        try {
            ClientTokenRequest clientTokenRequest = new ClientTokenRequest();
            return braintreeGateway.clientToken().generate(clientTokenRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate client token: " + e.getMessage());
        }
    }
}