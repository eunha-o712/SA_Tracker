package com.sa.trk.security;

import java.time.Duration;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.sa.trk.auth.service.AuthException;
import com.sa.trk.config.SecurityRateLimitProperties;
import com.sa.trk.security.RequestRateLimiter.RateLimitDecision;

@Component
public class SecurityRequestInterceptor implements HandlerInterceptor {

    private static final Set<String> PUBLIC_LOOKUP_PREFIXES = Set.of(
            "/api/search",
            "/api/player",
            "/api/match",
            "/api/stats",
            "/api/map",
            "/api/weapon",
            "/api/ranking"
    );

    private final RequestRateLimiter rateLimiter;
    private final SecurityRateLimitProperties properties;

    public SecurityRequestInterceptor(
            RequestRateLimiter rateLimiter,
            SecurityRateLimitProperties properties) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        addSecurityHeaders(request, response);
        if (!properties.isEnabled() || HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        LimitRule rule = ruleFor(request.getMethod(), request.getRequestURI());
        if (rule == null) {
            return true;
        }

        RateLimitDecision decision = rateLimiter.tryAcquire(
                rule.bucket(),
                request.getRemoteAddr(),
                rule.limit(),
                rule.window()
        );
        response.setHeader("X-RateLimit-Remaining", Integer.toString(decision.remaining()));
        if (!decision.allowed()) {
            response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", rule.message());
        }
        return true;
    }

    private LimitRule ruleFor(String method, String path) {
        if (HttpMethod.POST.matches(method) && "/api/auth/login".equals(path)) {
            return new LimitRule(
                    "login",
                    properties.getLoginMaxRequests(),
                    properties.getLoginWindow(),
                    "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요."
            );
        }
        if (HttpMethod.POST.matches(method) && "/api/auth/register".equals(path)) {
            return new LimitRule(
                    "registration",
                    properties.getRegistrationMaxRequests(),
                    properties.getRegistrationWindow(),
                    "회원가입 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."
            );
        }
        if (HttpMethod.POST.matches(method)
                && ("/api/auth/password-reset/request".equals(path)
                || "/api/auth/email-verification/resend".equals(path))) {
            return new LimitRule(
                    "account-email",
                    properties.getAccountEmailMaxRequests(),
                    properties.getAccountEmailWindow(),
                    "이메일 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."
            );
        }
        if (HttpMethod.GET.matches(method) && "/api/ai/record-room".equals(path)) {
            return new LimitRule(
                    "ai-analysis",
                    properties.getAiAnalysisMaxRequests(),
                    properties.getAiAnalysisWindow(),
                    "AI 분석 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."
            );
        }
        if (HttpMethod.GET.matches(method)
                && ("/api/ai/record-room/status".equals(path) || isPublicLookup(path))) {
            return new LimitRule(
                    "public-lookup",
                    properties.getPublicLookupMaxRequests(),
                    properties.getPublicLookupWindow(),
                    "조회 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."
            );
        }
        return null;
    }

    private boolean isPublicLookup(String path) {
        return PUBLIC_LOOKUP_PREFIXES.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + '/'));
    }

    private void addSecurityHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        if (containsAccountOrPrivateData(request.getRequestURI())) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
        }
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
    }

    private boolean containsAccountOrPrivateData(String path) {
        return path.startsWith("/api/auth")
                || path.startsWith("/api/admin")
                || path.startsWith("/api/board")
                || path.startsWith("/api/favorite")
                || path.startsWith("/api/clan")
                || "/api/player/me".equals(path);
    }

    private record LimitRule(String bucket, int limit, Duration window, String message) {
    }
}
