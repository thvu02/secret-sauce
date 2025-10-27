package org.splittydupe.startup.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.List;
import org.splittydupe.startup.Controller.PaymentController;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(classes = PaymentController.class)
@TestPropertySource(properties = {
    "payment_test_mode=true",
    "payment_sandbox_accounts=testuserA,testuserB"
})
public class PaymentControllerTest {

    @Value("${payment_test_mode:true}")
    private boolean testMode;

    @Value("${payment_sandbox_accounts:testuser1,testuser2,testuser3}")
    private String sandboxAccounts;

    @Test
    public void testGetConfigWithTestModeEnabled() {
        PaymentController controller = new PaymentController();
        ReflectionTestUtils.setField(controller, "testMode", true);
        ReflectionTestUtils.setField(controller, "sandboxAccounts", "testuserA,testuserB");

        Map<String, Object> config = controller.getConfig();
        assertEquals(true, config.get("testMode"));
        assertTrue(config.containsKey("testAccounts"));
        List<String> accounts = (List<String>) config.get("testAccounts");
        assertEquals(2, accounts.size());
        assertTrue(accounts.contains("testuserA"));
        assertTrue(accounts.contains("testuserB"));
    }

    @Test
    public void testGetConfigWithTestModeDisabled() {
        PaymentController controller = new PaymentController();
        ReflectionTestUtils.setField(controller, "testMode", false);

        Map<String, Object> config = controller.getConfig();
        assertEquals(false, config.get("testMode"));
        assertFalse(config.containsKey("testAccounts"));
    }
}

