package com.sa.trk.auth.dto;

public record AuthLoginRequest(String email, String loginId, String password) {

    public String resolvedEmail() {
        return email == null || email.isBlank() ? loginId : email;
    }
}
