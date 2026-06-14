package com.sa.trk.weapon.service;

import org.springframework.stereotype.Service;

import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserRecentInfoDto;
import com.sa.trk.player.service.PlayerService;
import com.sa.trk.weapon.dto.WeaponStatsResponseDto;

@Service
public class WeaponService {

    private final PlayerService playerService;

    public WeaponService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public WeaponStatsResponseDto getWeaponStats(String userName) {
        OuidResponseDto ouidResponse = playerService.getOuid(userName);
        String ouid = ouidResponse.getOuid();

        UserRecentInfoDto recentInfo = playerService.getUserRecentInfo(ouid);

        WeaponStatsResponseDto responseDto = new WeaponStatsResponseDto();
        responseDto.setUserName(userName);
        responseDto.setAssaultRate(recentInfo.getRecent_assault_rate());
        responseDto.setSniperRate(recentInfo.getRecent_sniper_rate());
        responseDto.setSpecialRate(recentInfo.getRecent_special_rate());

        return responseDto;
    }
}