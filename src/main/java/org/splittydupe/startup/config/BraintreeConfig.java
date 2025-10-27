package org.splittydupe.startup.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;

@Configuration
public class BraintreeConfig {
    @Value("${braintree_merchant_id}")
    private String merchantId;

    @Value("${braintree_public_key}")
    private String publicKey;

    @Value("${braintree_private_key}")
    private String privateKey;

    @Bean
    public BraintreeGateway braintreeGateway() {
        return new BraintreeGateway(
            Environment.SANDBOX,
            merchantId,
            publicKey,
            privateKey
        );
    }
}