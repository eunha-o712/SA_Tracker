package com.sa.trk.search.service;

import org.springframework.stereotype.Service;

import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.player.dto.PlayerResponseDto;
import com.sa.trk.player.service.PlayerService;

@Service
public class SearchService {

    private final PlayerService playerService;

    public SearchService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public PlayerResponseDto searchPlayer(String keyword) {
        OuidResponseDto ouidResponse = playerService.getOuid(keyword);
        String ouid = ouidResponse.getOuid();

        PlayerResponseDto responseDto = new PlayerResponseDto();
        responseDto.setUserName(keyword);
        responseDto.setOuid(ouid);
        responseDto.setBasic(playerService.getUserBasic(ouid));
        responseDto.setRank(playerService.getUserRank(ouid));
        responseDto.setTier(playerService.getUserTier(ouid));
        responseDto.setRecent(playerService.getUserRecentInfo(ouid));

        return responseDto;
    }
}