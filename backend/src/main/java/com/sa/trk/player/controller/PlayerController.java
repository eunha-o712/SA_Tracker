package com.sa.trk.player.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.player.dto.PlayerResponseDto;
import com.sa.trk.player.service.PlayerService;

@RestController
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/api/player")
    public PlayerResponseDto getPlayer(@RequestParam("userName") String userName) {
        OuidResponseDto ouidResponse = playerService.getOuid(userName);
        String ouid = ouidResponse.getOuid();

        PlayerResponseDto responseDto = new PlayerResponseDto();
        responseDto.setUserName(userName);
        responseDto.setOuid(ouid);
        responseDto.setBasic(playerService.getUserBasic(ouid));
        responseDto.setRank(playerService.getUserRank(ouid));
        responseDto.setTier(playerService.getUserTier(ouid));
        responseDto.setRecent(playerService.getUserRecentInfo(ouid));

        return responseDto;
    }
}