package com.mahta.backend_gare_routiere.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@Service
public class QrSecurityService {

    @Value("${app.qr.secret:MY_SECRET_KEY_123}")
    private String secret;

    public String generateSignature(String data) {

        try {

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec key = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(key);

            byte[] bytes = mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(bytes);

        } catch (Exception e) {

            log.error("QR signature generation failed: {}", e.getMessage());

            throw new RuntimeException(
                    "Error generating QR signature",
                    e
            );
        }
    }

    public boolean isValid(String data, String signature) {

        String expected = generateSignature(data);

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }
}