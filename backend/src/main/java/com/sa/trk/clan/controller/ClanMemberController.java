package com.sa.trk.clan.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.service.AuthService;
import com.sa.trk.clan.dto.ClanMemberResponseDto;
import com.sa.trk.clan.service.ClanMemberService;

@RestController
public class ClanMemberController {

    private final ClanMemberService clanMemberService;
    private final AuthService authService;

    public ClanMemberController(ClanMemberService clanMemberService, AuthService authService) {
        this.clanMemberService = clanMemberService;
        this.authService = authService;
    }

    @PostMapping("/api/clan/members")
    public ClanMemberResponseDto addMember(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("userName") String userName) {
        AuthUserResponse owner = authService.currentUser(bearerToken(authorization));
        return clanMemberService.addMember(owner.id(), userName);
    }

    @GetMapping("/api/clan/members")
    public List<ClanMemberResponseDto> getMembers(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        AuthUserResponse owner = authService.currentUser(bearerToken(authorization));
        return clanMemberService.getMembers(owner.id());
    }

    @DeleteMapping("/api/clan/members/{id}")
    public void deleteMember(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") Long id) {
        AuthUserResponse owner = authService.currentUser(bearerToken(authorization));
        clanMemberService.deleteMember(owner.id(), id);
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
