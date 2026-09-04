package com.sa.trk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "satrk.mail")
public class MailDeliveryProperties {

    private boolean enabled;
    private String from;
    private String replyTo;
    private String brevoApiKey;
    private String brevoApiUrl = "https://api.brevo.com/v3/smtp/email";
    private String frontendBaseUrl = "http://localhost:5173";
    private int dailyLimit = 500;
    private String zoneId = "Asia/Seoul";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getBrevoApiKey() {
        return brevoApiKey;
    }

    public void setBrevoApiKey(String brevoApiKey) {
        this.brevoApiKey = brevoApiKey;
    }

    public String getBrevoApiUrl() {
        return brevoApiUrl;
    }

    public void setBrevoApiUrl(String brevoApiUrl) {
        this.brevoApiUrl = brevoApiUrl;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }
}
