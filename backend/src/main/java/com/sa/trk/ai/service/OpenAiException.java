package com.sa.trk.ai.service;

import org.springframework.http.HttpStatus;

public class OpenAiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public OpenAiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
