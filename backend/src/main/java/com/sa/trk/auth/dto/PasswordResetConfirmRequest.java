package com.sa.trk.auth.dto;

public record PasswordResetConfirmRequest(String token, String password) {}
