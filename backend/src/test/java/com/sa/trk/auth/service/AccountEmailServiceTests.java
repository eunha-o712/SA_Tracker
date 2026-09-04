package com.sa.trk.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.sa.trk.config.MailDeliveryProperties;

class AccountEmailServiceTests {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private BrevoEmailClient brevoEmailClient;

    @Mock
    private ObjectProvider<BrevoEmailClient> brevoEmailClientProvider;

    @Mock
    private MailDeliveryQuotaService quotaService;

    private MailDeliveryProperties properties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new MailDeliveryProperties();
        properties.setEnabled(true);
        properties.setFrom("no-reply@satrk.example");
        properties.setReplyTo("support@satrk.example");
        properties.setFrontendBaseUrl("https://satrk.example/");
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(brevoEmailClientProvider.getIfAvailable()).thenReturn(brevoEmailClient);
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void sendsStyledPasswordResetWithFifteenMinuteLimit() throws Exception {
        AccountEmailService service = service();

        boolean sent = service.sendResetLink("member@example.com", "raw reset/token");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        verify(quotaService).recordSuccessfulDelivery();
        MimeMessage message = messageCaptor.getValue();
        message.saveChanges();
        String plainBody = findBody(message, "text/plain");
        String htmlBody = findBody(message, "text/html");
        assertThat(sent).isTrue();
        assertThat(message.getFrom()[0].toString()).isEqualTo("no-reply@satrk.example");
        assertThat(message.getReplyTo()[0].toString()).isEqualTo("support@satrk.example");
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString())
                .isEqualTo("member@example.com");
        assertThat(plainBody)
                .contains("https://satrk.example/login?resetToken=raw+reset%2Ftoken");
        assertThat(htmlBody)
                .contains("https://satrk.example/login?resetToken=raw+reset%2Ftoken")
                .contains("재설정 링크 유효시간")
                .contains("15분")
                .contains("src=\"cid:saHeader\"")
                .contains("src=\"cid:saFooter\"");
    }

    @Test
    void sendsStyledEmailVerificationWithIntentionalLineBreaks() throws Exception {
        AccountEmailService service = service();

        boolean sent = service.sendEmailVerificationLink("member@example.com", "verify/token");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage message = messageCaptor.getValue();
        message.saveChanges();
        String plainBody = findBody(message, "text/plain");
        String htmlBody = findBody(message, "text/html");
        assertThat(sent).isTrue();
        assertThat(message.getSubject()).contains("이메일 인증");
        assertThat(plainBody)
                .contains("https://satrk.example/login?verifyEmailToken=verify%2Ftoken");
        assertThat(htmlBody)
                .contains("https://satrk.example/login?verifyEmailToken=verify%2Ftoken")
                .contains("아래 버튼을 눌러<br>")
                .contains("이 메시지를 무시하셔도 됩니다.<br>")
                .contains("src=\"cid:saHeader\"")
                .contains("src=\"cid:saFooter\"");
    }

    @Test
    void disabledDeliveryDoesNotCallMailServer() {
        properties.setEnabled(false);
        AccountEmailService service = service();

        boolean sent = service.sendPasswordChangedNotice("member@example.com");

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void mailFailureDoesNotExposeRecipientOrTokenThroughException() {
        doThrow(new MailSendException("provider unavailable"))
                .when(mailSender).send(any(MimeMessage.class));
        AccountEmailService service = service();

        boolean sent = service.sendResetLink("member@example.com", "secret-token");

        assertThat(sent).isFalse();
    }

    @Test
    void blocksNonHttpsResetLinksOutsideLocalDevelopment() {
        properties.setFrontendBaseUrl("http://example.com");
        AccountEmailService service = service();

        boolean sent = service.sendResetLink("member@example.com", "secret-token");

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void dailyLimitPreventsDelivery() {
        when(quotaService.isLimitReached()).thenReturn(true);
        AccountEmailService service = service();

        boolean sent = service.sendResetLink("member@example.com", "secret-token");

        assertThat(sent).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(quotaService, never()).recordSuccessfulDelivery();
    }

    @Test
    void prefersBrevoApiAndUsesPublicTemplateImagesWhenConfigured() {
        when(brevoEmailClient.isConfigured()).thenReturn(true);
        when(brevoEmailClient.sendHtml(any(), any(), any())).thenReturn(true);
        AccountEmailService service = service();

        boolean sent = service.sendEmailVerificationLink("member@example.com", "verify-token");

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(brevoEmailClient).sendHtml(
                org.mockito.ArgumentMatchers.eq("member@example.com"),
                org.mockito.ArgumentMatchers.eq("[SA-TRACKER] 이메일 인증"),
                htmlCaptor.capture()
        );
        verify(mailSender, never()).createMimeMessage();
        verify(quotaService).recordSuccessfulDelivery();
        assertThat(sent).isTrue();
        assertThat(htmlCaptor.getValue())
                .contains("https://satrk.example/sa-assets/sample/sa-sub-header-ver2.png")
                .contains("https://satrk.example/sa-assets/sa-footer-logo.png")
                .doesNotContain("cid:saHeader", "cid:saFooter");
    }

    private AccountEmailService service() {
        return new AccountEmailService(
                mailSenderProvider,
                brevoEmailClientProvider,
                properties,
                quotaService
        );
    }

    private String findBody(Part part, String mimeType) throws Exception {
        if (part.isMimeType(mimeType)) {
            return part.getContent().toString();
        }
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                String body = findBody(multipart.getBodyPart(index), mimeType);
                if (!body.isEmpty()) {
                    return body;
                }
            }
        }
        return "";
    }
}
