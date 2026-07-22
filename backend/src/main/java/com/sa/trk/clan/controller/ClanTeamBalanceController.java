package com.sa.trk.clan.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.service.AuthService;
import com.sa.trk.clan.dto.ClanTeamBalanceRequest;
import com.sa.trk.clan.dto.ClanTeamBalanceResponseDto;
import com.sa.trk.clan.service.ClanTeamBalanceService;

@RestController
@RequestMapping("/api/clan/team-balance")
public class ClanTeamBalanceController {

    private final ClanTeamBalanceService clanTeamBalanceService;
    private final AuthService authService;

    public ClanTeamBalanceController(
            ClanTeamBalanceService clanTeamBalanceService,
            AuthService authService) {
        this.clanTeamBalanceService = clanTeamBalanceService;
        this.authService = authService;
    }

    @PostMapping
    public ClanTeamBalanceResponseDto balanceTeams(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody ClanTeamBalanceRequest request) {
        AuthUserResponse owner = authService.currentUser(bearerToken(authorization));
        return clanTeamBalanceService.balance(owner.id(), request);
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
