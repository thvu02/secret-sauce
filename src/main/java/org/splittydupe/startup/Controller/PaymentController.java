package org.splittydupe.startup.Controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class PaymentController {
    
    @Value("${payment_test_mode:true}")
    private boolean testMode;

    @Value("${payment_sandbox_accounts:testuser1,testuser2,testuser3}")
    private String sandboxAccounts;

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("testMode", testMode);
        if (testMode) {
            config.put("testAccounts", Arrays.asList(sandboxAccounts.split(",")));
        }
        return config;
    }
}
