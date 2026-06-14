package com.sa.trk.stats.service;

import org.springframework.stereotype.Service;

import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserRecentInfoDto;
import com.sa.trk.player.service.PlayerService;
import com.sa.trk.stats.dto.StatsResponseDto;

@Service
public class StatsService {

    private final PlayerService playerService;

    public StatsService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public StatsResponseDto getStats(String userName) {
        OuidResponseDto ouidResponse = playerService.getOuid(userName);
        String ouid = ouidResponse.getOuid();

        UserRecentInfoDto recentInfo = playerService.getUserRecentInfo(ouid);

        StatsResponseDto responseDto = new StatsResponseDto();
        responseDto.setUserName(userName);
        responseDto.setWinRate(recentInfo.getRecent_win_rate());
        responseDto.setKillDeathRate(recentInfo.getRecent_kill_death_rate());
        responseDto.setAssaultRate(recentInfo.getRecent_assault_rate());
        responseDto.setSniperRate(recentInfo.getRecent_sniper_rate());
        responseDto.setSpecialRate(recentInfo.getRecent_special_rate());

        return responseDto;
    }
}