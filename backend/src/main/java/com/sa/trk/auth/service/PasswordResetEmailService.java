package com.sa.trk.auth.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private final String frontendBaseUrl;

    public PasswordResetEmailService(
            @Value("${satrk.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl == null || frontendBaseUrl.isBlank()
                ? "http://localhost:5173"
                : frontendBaseUrl.replaceAll("/+$", "");
    }

    public PasswordResetDelivery sendResetLink(String email, String rawToken) {
        String resetUrl = frontendBaseUrl + "/login?resetToken="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        log.info("Password reset link for {}: {}", email, resetUrl);
        return new PasswordResetDelivery(resetUrl);
    }
}
