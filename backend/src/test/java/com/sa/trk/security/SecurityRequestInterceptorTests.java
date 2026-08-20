package com.sa.trk.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.sa.trk.auth.service.AuthException;
import com.sa.trk.config.SecurityRateLimitProperties;

class SecurityRequestInterceptorTests {

    @Test
    void appliesSecurityHeadersAndBlocksRepeatedLogin() {
        SecurityRateLimitProperties properties = new SecurityRateLimitProperties();
        properties.setLoginMaxRequests(1);
        SecurityRequestInterceptor interceptor = new SecurityRequestInterceptor(
                new RequestRateLimiter(),
                properties
        );
        MockHttpServletRequest firstRequest = loginRequest();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(firstRequest, firstResponse, new Object())).isTrue();
        assertThat(firstResponse.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(firstResponse.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(firstResponse.getHeader("Cache-Control")).isEqualTo("no-store");

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        assertThatThrownBy(() -> interceptor.preHandle(loginRequest(), blockedResponse, new Object()))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> assertThat(((AuthException) exception).getStatus().value()).isEqualTo(429));
        assertThat(blockedResponse.getHeader("Retry-After")).isNotBlank();
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
