package com.sa.trk.auth.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.sa.trk.config.MailDeliveryProperties;

@Component
public class BrevoEmailClient {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailClient.class);
    private static final String DEFAULT_SENDER_NAME = "SA-TRACKER";

    private final MailDeliveryProperties properties;
    private final RestClient restClient;

    public BrevoEmailClient(MailDeliveryProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public boolean isConfigured() {
        return !isBlank(properties.getBrevoApiKey())
                && !isBlank(properties.getBrevoApiUrl());
    }

    public boolean sendText(String recipient, String subject, String textBody) {
        return send(recipient, subject, "textContent", textBody);
    }

    public boolean sendHtml(String recipient, String subject, String htmlBody) {
        return send(recipient, subject, "htmlContent", htmlBody);
    }

    private boolean send(String recipient, String subject, String contentField, String content) {
        if (!isConfigured()) {
            return false;
        }

        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("sender", mailbox(properties.getFrom(), DEFAULT_SENDER_NAME));
            request.put("to", List.of(mailbox(recipient, null)));
            if (!isBlank(properties.getReplyTo())) {
                request.put("replyTo", mailbox(properties.getReplyTo(), null));
            }
            request.put("subject", subject);
            request.put(contentField, content);

            restClient.post()
                    .uri(properties.getBrevoApiUrl().trim())
                    .header("api-key", properties.getBrevoApiKey().trim())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Account email accepted by the Brevo API.");
            return true;
        } catch (RestClientResponseException exception) {
            log.error("Brevo email delivery failed: status={}", exception.getStatusCode().value());
            return false;
        } catch (RestClientException | AddressException | IllegalArgumentException exception) {
            log.error("Brevo email delivery failed: type={}", exception.getClass().getSimpleName());
            return false;
        }
    }

    private Map<String, String> mailbox(String rawAddress, String fallbackName)
            throws AddressException {
        InternetAddress parsed = new InternetAddress(rawAddress == null ? "" : rawAddress.trim(), true);
        Map<String, String> mailbox = new LinkedHashMap<>();
        mailbox.put("email", parsed.getAddress());
        String name = isBlank(parsed.getPersonal()) ? fallbackName : parsed.getPersonal();
        if (!isBlank(name)) {
            mailbox.put("name", name);
        }
        return mailbox;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
