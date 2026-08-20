package com.sa.trk.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "satrk.security.rate-limit")
public class SecurityRateLimitProperties {

    private boolean enabled = true;
    private int loginMaxRequests = 10;
    private Duration loginWindow = Duration.ofMinutes(15);
    private int registrationMaxRequests = 5;
    private Duration registrationWindow = Duration.ofHours(1);
    private int accountEmailMaxRequests = 10;
    private Duration accountEmailWindow = Duration.ofHours(1);
    private int aiAnalysisMaxRequests = 10;
    private Duration aiAnalysisWindow = Duration.ofHours(1);
    private int publicLookupMaxRequests = 120;
    private Duration publicLookupWindow = Duration.ofMinutes(1);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getLoginMaxRequests() { return loginMaxRequests; }
    public void setLoginMaxRequests(int loginMaxRequests) { this.loginMaxRequests = loginMaxRequests; }
    public Duration getLoginWindow() { return loginWindow; }
    public void setLoginWindow(Duration loginWindow) { this.loginWindow = loginWindow; }
    public int getRegistrationMaxRequests() { return registrationMaxRequests; }
    public void setRegistrationMaxRequests(int registrationMaxRequests) { this.registrationMaxRequests = registrationMaxRequests; }
    public Duration getRegistrationWindow() { return registrationWindow; }
    public void setRegistrationWindow(Duration registrationWindow) { this.registrationWindow = registrationWindow; }
    public int getAccountEmailMaxRequests() { return accountEmailMaxRequests; }
    public void setAccountEmailMaxRequests(int accountEmailMaxRequests) { this.accountEmailMaxRequests = accountEmailMaxRequests; }
    public Duration getAccountEmailWindow() { return accountEmailWindow; }
    public void setAccountEmailWindow(Duration accountEmailWindow) { this.accountEmailWindow = accountEmailWindow; }
    public int getAiAnalysisMaxRequests() { return aiAnalysisMaxRequests; }
    public void setAiAnalysisMaxRequests(int aiAnalysisMaxRequests) { this.aiAnalysisMaxRequests = aiAnalysisMaxRequests; }
    public Duration getAiAnalysisWindow() { return aiAnalysisWindow; }
    public void setAiAnalysisWindow(Duration aiAnalysisWindow) { this.aiAnalysisWindow = aiAnalysisWindow; }
    public int getPublicLookupMaxRequests() { return publicLookupMaxRequests; }
    public void setPublicLookupMaxRequests(int publicLookupMaxRequests) { this.publicLookupMaxRequests = publicLookupMaxRequests; }
    public Duration getPublicLookupWindow() { return publicLookupWindow; }
    public void setPublicLookupWindow(Duration publicLookupWindow) { this.publicLookupWindow = publicLookupWindow; }
}
