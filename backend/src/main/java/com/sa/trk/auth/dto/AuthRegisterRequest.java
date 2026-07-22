package com.sa.trk.auth.dto;

public record AuthRegisterRequest(
        String email,
        String loginId,
        String password,
        String suddenNickname,
        String displayName) {

    public String resolvedEmail() {
        return email == null || email.isBlank() ? loginId : email;
    }

    public String resolvedSuddenNickname() {
        return suddenNickname == null || suddenNickname.isBlank() ? displayName : suddenNickname;
    }
}
