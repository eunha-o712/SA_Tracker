package com.sa.trk.auth.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.sa.trk.config.MailDeliveryProperties;

@Service
public class AccountEmailService {

    private static final Logger log = LoggerFactory.getLogger(AccountEmailService.class);
    private static final ClassPathResource EMAIL_VERIFICATION_TEMPLATE =
            new ClassPathResource("mail/email-verification.html");
    private static final ClassPathResource PASSWORD_RESET_TEMPLATE =
            new ClassPathResource("mail/password-reset.html");
    private static final ClassPathResource EMAIL_HEADER_IMAGE =
            new ClassPathResource("mail/images/sa-sub-header-ver2.png");
    private static final ClassPathResource EMAIL_FOOTER_IMAGE =
            new ClassPathResource("mail/images/sa-footer-logo.png");

    private final JavaMailSender mailSender;
    private final MailDeliveryProperties properties;
    private final MailDeliveryQuotaService quotaService;
    private final String frontendBaseUrl;
    private final Object deliveryLock = new Object();

    public AccountEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            MailDeliveryProperties properties,
            MailDeliveryQuotaService quotaService) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.properties = properties;
        this.quotaService = quotaService;
        String configuredBaseUrl = properties.getFrontendBaseUrl();
        this.frontendBaseUrl = configuredBaseUrl == null || configuredBaseUrl.isBlank()
                ? "http://localhost:5173"
                : configuredBaseUrl.replaceAll("/+$", "");
    }

    public boolean sendEmailVerificationLink(String email, String rawToken) {
        if (!isSafeFrontendBaseUrl()) {
            log.error("Email verification delivery blocked because the frontend URL is not HTTPS.");
            return false;
        }
        String verificationUrl = frontendBaseUrl + "/login?verifyEmailToken="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String plainBody = """
                SA-TRACKER 회원가입 이메일 인증을 요청하셨습니다.

                아래 주소에서 24시간 이내에 이메일 인증을 완료해 주세요.
                %s

                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                SA-TRACKER는 이메일이나 문의를 통해 비밀번호를 요구하지 않습니다.
                """.formatted(verificationUrl);
        try {
            String htmlBody = EMAIL_VERIFICATION_TEMPLATE
                    .getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{VERIFICATION_URL}}", HtmlUtils.htmlEscape(verificationUrl));
            return sendHtml(
                    email,
                    "[SA-TRACKER] 이메일 인증",
                    plainBody,
                    htmlBody
            );
        } catch (IOException exception) {
            log.error("Email verification template could not be loaded.");
            return false;
        }
    }

    public boolean sendResetLink(String email, String rawToken) {
        if (!isSafeFrontendBaseUrl()) {
            log.error("Password reset email delivery blocked because the frontend URL is not HTTPS.");
            return false;
        }
        String resetUrl = frontendBaseUrl + "/login?resetToken="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String plainBody = """
                SA-TRACKER 비밀번호 재설정 요청을 받았습니다.

                아래 주소에서 15분 이내에 새 비밀번호를 설정해 주세요.
                %s

                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                SA-TRACKER는 이메일이나 문의를 통해 비밀번호를 요구하지 않습니다.
                """.formatted(resetUrl);
        try {
            String htmlBody = PASSWORD_RESET_TEMPLATE
                    .getContentAsString(StandardCharsets.UTF_8)
                    .replace("{{RESET_URL}}", HtmlUtils.htmlEscape(resetUrl));
            return sendHtml(
                    email,
                    "[SA-TRACKER] 비밀번호 재설정",
                    plainBody,
                    htmlBody
            );
        } catch (IOException exception) {
            log.error("Password reset template could not be loaded.");
            return false;
        }
    }

    public boolean sendPasswordChangedNotice(String email) {
        return send(
                email,
                "[SA-TRACKER] 비밀번호 변경 완료",
                """
                SA-TRACKER 계정의 비밀번호가 변경되었습니다.

                본인이 변경하지 않았다면 즉시 고객센터로 문의해 주세요.
                보안을 위해 기존 로그인 세션은 모두 종료되었습니다.
                """
        );
    }

    public boolean isDailyLimitReached() {
        return isDailyLimitReachedSafely();
    }

    private boolean send(String recipient, String subject, String body) {
        if (!properties.isEnabled()) {
            log.info("Account email delivery skipped because mail is disabled.");
            return false;
        }
        if (mailSender == null || isBlank(properties.getFrom())) {
            log.error("Account email delivery is enabled but SMTP or sender configuration is incomplete.");
            return false;
        }

        synchronized (deliveryLock) {
            if (isDailyLimitReachedSafely()) {
                log.warn("Account email delivery skipped because the daily limit was reached.");
                return false;
            }
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(properties.getFrom().trim());
                message.setTo(recipient);
                if (!isBlank(properties.getReplyTo())) {
                    message.setReplyTo(properties.getReplyTo().trim());
                }
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                recordSuccessfulDeliverySafely();
                log.info("Account email accepted by the configured mail server.");
                return true;
            } catch (MailException | IllegalArgumentException exception) {
                log.error("Account email delivery failed: type={}", exception.getClass().getSimpleName());
                return false;
            }
        }
    }

    private boolean sendHtml(
            String recipient,
            String subject,
            String plainBody,
            String htmlBody) {
        if (!properties.isEnabled()) {
            log.info("Account email delivery skipped because mail is disabled.");
            return false;
        }
        if (mailSender == null || isBlank(properties.getFrom())) {
            log.error("Account email delivery is enabled but SMTP or sender configuration is incomplete.");
            return false;
        }

        synchronized (deliveryLock) {
            if (isDailyLimitReachedSafely()) {
                log.warn("Account email delivery skipped because the daily limit was reached.");
                return false;
            }
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(
                        message,
                        true,
                        StandardCharsets.UTF_8.name()
                );
                helper.setFrom(properties.getFrom().trim());
                helper.setTo(recipient);
                if (!isBlank(properties.getReplyTo())) {
                    helper.setReplyTo(properties.getReplyTo().trim());
                }
                helper.setSubject(subject);
                helper.setText(plainBody, htmlBody);
                helper.addInline("saHeader", EMAIL_HEADER_IMAGE, MediaType.IMAGE_PNG_VALUE);
                helper.addInline("saFooter", EMAIL_FOOTER_IMAGE, MediaType.IMAGE_PNG_VALUE);
                mailSender.send(message);
                recordSuccessfulDeliverySafely();
                log.info("Account HTML email accepted by the configured mail server.");
                return true;
            } catch (MessagingException | MailException | IllegalArgumentException exception) {
                log.error("Account HTML email delivery failed: type={}",
                        exception.getClass().getSimpleName());
                return false;
            }
        }
    }

    private boolean isDailyLimitReachedSafely() {
        try {
            return quotaService.isLimitReached();
        } catch (RuntimeException exception) {
            log.error("Mail quota lookup failed; delivery is blocked: type={}",
                    exception.getClass().getSimpleName());
            return true;
        }
    }

    private void recordSuccessfulDeliverySafely() {
        try {
            quotaService.recordSuccessfulDelivery();
        } catch (RuntimeException exception) {
            log.error("Mail delivery succeeded but usage recording failed: type={}",
                    exception.getClass().getSimpleName());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isSafeFrontendBaseUrl() {
        try {
            URI uri = new URI(frontendBaseUrl);
            String host = uri.getHost();
            if ("https".equalsIgnoreCase(uri.getScheme()) && host != null) {
                return true;
            }
            return "http".equalsIgnoreCase(uri.getScheme())
                    && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host));
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
