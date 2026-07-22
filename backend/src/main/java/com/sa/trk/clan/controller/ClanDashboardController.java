package com.sa.trk.clan.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.service.AuthService;
import com.sa.trk.clan.dto.ClanDashboardResponseDto;
import com.sa.trk.clan.service.ClanDashboardService;

@RestController
public class ClanDashboardController {

    private final ClanDashboardService clanDashboardService;
    private final AuthService authService;

    public ClanDashboardController(ClanDashboardService clanDashboardService, AuthService authService) {
        this.clanDashboardService = clanDashboardService;
        this.authService = authService;
    }

    @GetMapping("/api/clan/dashboard")
    public ClanDashboardResponseDto getDashboard(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("clanName") String clanName) {
        AuthUserResponse owner = authService.currentUser(bearerToken(authorization));
        return clanDashboardService.getDashboard(owner.id(), clanName);
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
