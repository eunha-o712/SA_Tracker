package com.sa.trk.common.error;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidParameter(
            Exception exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "요청 파라미터를 확인해주세요.",
                request
        );
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<ApiErrorResponse> handleNexonApiError(
            HttpStatusCodeException exception,
            HttpServletRequest request) {
        int externalStatus = exception.getStatusCode().value();
        HttpStatus status;
        String code;
        String message;

        switch (externalStatus) {
            case 400 -> {
                status = HttpStatus.BAD_REQUEST;
                code = "NEXON_BAD_REQUEST";
                message = "플레이어 또는 매치 정보를 확인해주세요.";
            }
            case 403 -> {
                status = HttpStatus.FORBIDDEN;
                code = "NEXON_FORBIDDEN";
                message = "외부 전적 서비스 인증에 실패했습니다.";
            }
            case 404 -> {
                status = HttpStatus.NOT_FOUND;
                code = "NEXON_NOT_FOUND";
                message = "요청한 전적 정보를 찾을 수 없습니다.";
            }
            case 429 -> {
                status = HttpStatus.TOO_MANY_REQUESTS;
                code = "NEXON_RATE_LIMITED";
                message = "조회 요청이 많습니다. 잠시 후 다시 시도해주세요.";
            }
            default -> {
                status = HttpStatus.BAD_GATEWAY;
                code = "NEXON_API_ERROR";
                message = "외부 전적 서비스 응답을 처리하지 못했습니다.";
            }
        }

        log.warn("Nexon API error: externalStatus={}, path={}", externalStatus, request.getRequestURI());
        return response(status, code, message, request);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleNexonApiUnavailable(
            ResourceAccessException exception,
            HttpServletRequest request) {
        log.warn("Nexon API unavailable: path={}", request.getRequestURI());
        return response(
                HttpStatus.BAD_GATEWAY,
                "NEXON_API_UNAVAILABLE",
                "외부 전적 서비스에 연결할 수 없습니다.",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedError(
            Exception exception,
            HttpServletRequest request) {
        log.error("Unhandled request error: path={}", request.getRequestURI(), exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "서버에서 요청을 처리하지 못했습니다.",
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
