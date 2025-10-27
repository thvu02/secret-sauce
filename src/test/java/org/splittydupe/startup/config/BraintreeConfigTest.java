package org.splittydupe.startup.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.braintreegateway.BraintreeGateway;

import static org.junit.jupiter.api.Assertions.*;

public class BraintreeConfigTest {

    @Test
    public void testBraintreeGatewayBean() {
        BraintreeConfig config = new BraintreeConfig();
        ReflectionTestUtils.setField(config, "merchantId", "testMerchantId");
        ReflectionTestUtils.setField(config, "publicKey", "testPublicKey");
        ReflectionTestUtils.setField(config, "privateKey", "testPrivateKey");

        BraintreeGateway gateway = config.braintreeGateway();

        assertNotNull(gateway);
    }
}
