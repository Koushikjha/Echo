package com.example.auth.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class SmsClient {

    private final RestClient restClient;

    @Value("${Echo.sms.mock:true}")
    private boolean mockMode;

    @Value("${Echo.sms.provider-url:}")
    private String providerUrl;

    @Value("${Echo.sms.api-key:}")
    private String apiKey;

    @Value("${Echo.sms.template-id:}")
    private String templateId;

    public SmsClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public void sendOtp(String phone, String otp) {

        if (mockMode) {
            log.info("╔══════════════════════════════╗");
            log.info("║  [MOCK SMS] To: +91{}       ║", phone);
            log.info("║  OTP: {}                    ║", otp);
            log.info("╚══════════════════════════════╝");
            return;
        }

        if (providerUrl.isBlank() || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "SMS provider not configured. " +
                            "Set Echo.sms.provider-url and Echo.sms.api-key"
            );
        }

        try {
            String response = restClient.post()
                    .uri(providerUrl)
                    .header("authkey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .body(String.class);

            log.info("OTP SMS request successful | response={}", response);

        } catch (RestClientException e) {
            log.error("Failed to send OTP SMS to +91{}", phone, e);
            throw new IllegalStateException("Failed to send OTP SMS", e);
        }
    }
}