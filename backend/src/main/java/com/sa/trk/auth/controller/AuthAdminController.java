package com.sa.trk.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.dto.AccountStatusUpdateRequest;
import com.sa.trk.auth.dto.AdminAccountStatusResponse;
import com.sa.trk.auth.dto.ManualVerificationRequest;
import com.sa.trk.auth.service.AuthService;

@RestController
@RequestMapping("/api/admin/users")
public class AuthAdminController {

    private final AuthService authService;

    public AuthAdminController(AuthService authService) {
        this.authService = authService;
    }

    @PatchMapping("/{id}/manual-verification")
    public AuthUserResponse updateManualVerification(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") Long userId,
            @RequestBody ManualVerificationRequest request) {
        return authService.setManualVerification(
                bearerToken(authorization),
                userId,
                request == null ? null : request.verified()
        );
    }

    @PatchMapping("/{id}/account-status")
    public AdminAccountStatusResponse updateAccountStatus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") Long userId,
            @RequestBody AccountStatusUpdateRequest request) {
        return authService.setAccountStatus(bearerToken(authorization), userId, request);
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
