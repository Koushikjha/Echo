package com.example.auth.service;


import com.example.auth.entity.OtpRecord;
import com.example.auth.repository.OtpRepository;
import com.example.auth.sms.SmsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final SmsClient smsClient;

    @Value("${Echo.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${Echo.otp.max-attempts:3}")
    private int maxAttempts;

    private static final SecureRandom RANDOM = new SecureRandom();


    @Transactional
    public void generateAndSend(String phone) {
        otpRepository.invalidateAllForPhone(phone);

        String otp = generateOtp();

        OtpRecord record = OtpRecord.builder()
                .phone(phone)
                .otp(otp)
                .expiresAt(LocalDateTime.now()
                        .plusMinutes(otpExpiryMinutes))
                .build();

        otpRepository.save(record);
        smsClient.sendOtp(phone, otp);

        log.info("OTP generated and sent to phone={}", phone);
    }


    @Transactional
    public void verify(String phone, String otp) {
        OtpRecord record = otpRepository.findLatestValidOtp(phone)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No valid OTP found for this number. " +
                                "Please request a new OTP."));


        if (record.getAttempts() >= maxAttempts) {
            record.setVerified(true);
            otpRepository.save(record);
            throw new IllegalStateException(
                    "Too many incorrect attempts. " +
                            "Please request a new OTP.");
        }

        if (!record.getOtp().equals(otp)) {
            record.setAttempts(record.getAttempts() + 1);
            otpRepository.save(record);
            int remaining = maxAttempts - record.getAttempts();
            throw new IllegalArgumentException(
                    "Incorrect OTP. " + remaining + " attempt(s) remaining.");
        }

        record.setVerified(true);
        otpRepository.save(record);
        log.info("OTP verified successfully for phone={}", phone);
    }


    private String generateOtp() {
        int otp = 1000 + RANDOM.nextInt(9000);
        return String.valueOf(otp);
    }
}