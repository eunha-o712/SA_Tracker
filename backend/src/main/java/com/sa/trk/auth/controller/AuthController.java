package com.sa.trk.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.auth.dto.AuthLoginRequest;
import com.sa.trk.auth.dto.AuthRegisterRequest;
import com.sa.trk.auth.dto.AuthResponse;
import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.dto.ClanStatusUpdateRequest;
import com.sa.trk.auth.dto.EmailVerificationConfirmRequest;
import com.sa.trk.auth.dto.EmailVerificationRequest;
import com.sa.trk.auth.dto.EmailVerificationResponse;
import com.sa.trk.auth.dto.PasswordResetConfirmRequest;
import com.sa.trk.auth.dto.PasswordResetConfirmResponse;
import com.sa.trk.auth.dto.PasswordResetRequest;
import com.sa.trk.auth.dto.PasswordResetRequestResponse;
import com.sa.trk.auth.dto.SuddenAccountLinkRequest;
import com.sa.trk.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public EmailVerificationResponse register(@RequestBody AuthRegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthUserResponse currentUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return authService.currentUser(bearerToken(authorization));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<PasswordResetRequestResponse> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        return noStore(authService.requestPasswordReset(request));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<PasswordResetConfirmResponse> confirmPasswordReset(
            @RequestBody PasswordResetConfirmRequest request) {
        return noStore(authService.confirmPasswordReset(request));
    }

    @PostMapping("/email-verification/resend")
    public ResponseEntity<EmailVerificationResponse> resendEmailVerification(
            @RequestBody EmailVerificationRequest request) {
        return noStore(authService.resendEmailVerification(request));
    }

    @PostMapping("/email-verification/confirm")
    public ResponseEntity<EmailVerificationResponse> confirmEmailVerification(
            @RequestBody EmailVerificationConfirmRequest request) {
        return noStore(authService.confirmEmailVerification(request));
    }

    @PatchMapping("/sudden-nickname/sync")
    public AuthUserResponse syncSuddenNickname(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return authService.syncSuddenNickname(bearerToken(authorization));
    }

    @PatchMapping("/sudden-account/link")
    public AuthUserResponse linkSuddenAccount(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody SuddenAccountLinkRequest request) {
        return authService.linkSuddenAccount(bearerToken(authorization), request);
    }

    @PatchMapping("/clan-status")
    public AuthUserResponse updateClanStatus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody ClanStatusUpdateRequest request) {
        return authService.updateClanStatus(bearerToken(authorization), request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(bearerToken(authorization));
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    private <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .header("Referrer-Policy", "no-referrer")
                .body(body);
    }
}
