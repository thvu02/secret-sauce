package org.splittydupe.startup.Payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.braintreegateway.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

public class PaymentServiceTest {

    @Mock
    private BraintreeGateway braintreeGateway;

    @Mock
    private TransactionGateway transactionGateway;

    @Mock
    private Transaction transaction;

    @Mock
    private Result<Transaction> successResult;

    @Mock
    private Result<Transaction> failureResult;

    @Mock
    private ClientTokenGateway clientTokenGateway;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(braintreeGateway.transaction()).thenReturn(transactionGateway);
        when(braintreeGateway.clientToken()).thenReturn(clientTokenGateway);
        ReflectionTestUtils.setField(paymentService, "testMode", true);
        ReflectionTestUtils.setField(paymentService, "sandboxAccounts", "testuser1,testuser2,testuser3");
    }

    @Test
    public void testRequestPayment_testMode_nonTestUser() {
        Map<String, Object> response = paymentService.requestPayment("nonTestUser", BigDecimal.TEN, "note");
        assertFalse((Boolean)response.get("success"));
        assertTrue(((String)response.get("error")).contains("only test accounts are allowed"));
    }

    @Test
    public void testRequestPayment_testMode_testUser() {
        Map<String, Object> response = paymentService.requestPayment("testuser1", BigDecimal.TEN, "note");

        assertTrue((Boolean)response.get("success"));
        assertNotNull(response.get("transactionId"));
        assertEquals("SUBMITTED_FOR_SETTLEMENT", response.get("status"));
        assertTrue((Boolean)response.get("testMode"));
    }

    @Test
    public void testRequestPayment_liveMode_success() throws Exception {
        ReflectionTestUtils.setField(paymentService, "testMode", false);

        when(transactionGateway.sale(any(TransactionRequest.class))).thenReturn(successResult);
        when(successResult.isSuccess()).thenReturn(true);
        when(successResult.getTarget()).thenReturn(transaction);
        when(transaction.getId()).thenReturn("txn123");
        when(transaction.getStatus()).thenReturn(Transaction.Status.SUBMITTED_FOR_SETTLEMENT);

        Map<String, Object> response = paymentService.requestPayment("validUser", BigDecimal.TEN, "note");

        assertTrue((Boolean)response.get("success"));
        assertEquals("txn123", response.get("transactionId"));
        assertEquals(Transaction.Status.SUBMITTED_FOR_SETTLEMENT, response.get("status"));
    }

    @Test
    public void testRequestPayment_liveMode_failure() throws Exception {
        ReflectionTestUtils.setField(paymentService, "testMode", false);

        when(transactionGateway.sale(any(TransactionRequest.class))).thenReturn(failureResult);
        when(failureResult.isSuccess()).thenReturn(false);
        when(failureResult.getMessage()).thenReturn("Failure reason");
        when(failureResult.getErrors()).thenReturn(mock(ValidationErrors.class));

        Map<String, Object> response = paymentService.requestPayment("validUser", BigDecimal.TEN, "note");

        assertFalse((Boolean)response.get("success"));
        assertEquals("Failure reason", response.get("message"));
        assertNotNull(response.get("errors"));
    }

    @Test
    public void testRequestPayment_liveMode_exception() throws Exception {
        ReflectionTestUtils.setField(paymentService, "testMode", false);

        when(transactionGateway.sale(any(TransactionRequest.class))).thenThrow(new RuntimeException("Error"));

        Map<String, Object> response = paymentService.requestPayment("validUser", BigDecimal.TEN, "note");

        assertFalse((Boolean)response.get("success"));
        assertEquals("Error", response.get("error"));
        assertFalse((Boolean)response.get("testMode"));
    }

    @Test
    public void testGenerateClientToken_success() {
        when(clientTokenGateway.generate(any(ClientTokenRequest.class))).thenReturn("token123");

        String token = paymentService.generateClientToken();

        assertEquals("token123", token);
    }

    @Test
    public void testGenerateClientToken_failure() {
        when(clientTokenGateway.generate(any(ClientTokenRequest.class))).thenThrow(new RuntimeException("fail"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            paymentService.generateClientToken();
        });

        assertTrue(thrown.getMessage().contains("Failed to generate client token"));
    }
}