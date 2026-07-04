package com.sa.trk.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.HttpClientErrorException;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsStructuredBadRequestForInvalidArguments() {
        MockHttpServletRequest request = request("/api/match");

        var response = handler.handleBadRequest(
                new IllegalArgumentException("사용자 이름 값이 필요합니다."),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().message()).isEqualTo("사용자 이름 값이 필요합니다.");
        assertThat(response.getBody().path()).isEqualTo("/api/match");
    }

    @Test
    void returnsRateLimitResponseForNexon429() {
        MockHttpServletRequest request = request("/api/player");
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8
        );

        var response = handler.handleNexonApiError(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NEXON_RATE_LIMITED");
        assertThat(response.getBody().message()).doesNotContain("Too Many Requests");
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        MockHttpServletRequest request = request("/api/stats");

        var response = handler.handleUnexpectedError(
                new RuntimeException("sensitive internal detail"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().message()).doesNotContain("sensitive internal detail");
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
