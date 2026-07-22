package com.sa.trk.player.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.service.AuthException;
import com.sa.trk.auth.service.AuthService;
import com.sa.trk.player.dto.PlayerResponseDto;
import com.sa.trk.player.service.PlayerService;

@RestController
public class PlayerController {

    private final PlayerService playerService;
    private final AuthService authService;

    public PlayerController(PlayerService playerService, AuthService authService) {
        this.playerService = playerService;
        this.authService = authService;
    }

    @GetMapping("/api/player")
    public PlayerResponseDto getPlayer(@RequestParam("userName") String userName) {
        return playerService.getPlayer(userName);
    }

    @GetMapping("/api/player/me")
    public PlayerResponseDto getCurrentPlayer(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        AuthUserResponse user = authService.currentUser(bearerToken(authorization));
        if (user.ouid() == null || user.ouid().isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "OUID_NOT_LINKED", "서든어택 계정 연동 정보가 없습니다.");
        }
        return playerService.getPlayerByOuid(user.ouid());
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
