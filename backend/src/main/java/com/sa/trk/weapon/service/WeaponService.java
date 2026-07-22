package com.sa.trk.weapon.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserRecentInfoDto;
import com.sa.trk.match.dto.HeadshotStatsDto;
import com.sa.trk.match.service.MatchService;
import com.sa.trk.player.service.PlayerService;
import com.sa.trk.weapon.dto.WeaponStatsResponseDto;

@Service
public class WeaponService {

    private final PlayerService playerService;
    private final MatchService matchService;

    public WeaponService(PlayerService playerService, MatchService matchService) {
        this.playerService = playerService;
        this.matchService = matchService;
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

        List<WeaponClassScore> scores = List.of(
                new WeaponClassScore("돌격", valueOrZero(responseDto.getAssaultRate())),
                new WeaponClassScore("저격", valueOrZero(responseDto.getSniperRate())),
                new WeaponClassScore("특수", valueOrZero(responseDto.getSpecialRate()))
        ).stream().sorted(Comparator.comparingDouble(WeaponClassScore::score).reversed()).toList();

        WeaponClassScore primary = scores.get(0);
        double primaryGap = primary.score() - scores.get(1).score();
        double minimum = scores.get(2).score();
        double specializationIndex = primary.score() == 0.0
                ? 0.0
                : (primary.score() - minimum) * 100.0 / primary.score();

        responseDto.setPrimaryClass(primary.label());
        responseDto.setPrimaryGap(roundOne(primaryGap));
        responseDto.setSpecializationIndex(roundOne(specializationIndex));
        responseDto.setCombatType(determineCombatType(primary.label(), primaryGap));

        HeadshotStatsDto headshotStats = matchService.getHeadshotStats(userName);
        responseDto.setSampleMatchCount(headshotStats.getSampleMatchCount());
        responseDto.setTotalKills(headshotStats.getTotalKills());
        responseDto.setTotalHeadshots(headshotStats.getTotalHeadshots());
        responseDto.setHeadshotRate(headshotStats.getHeadshotRate());
        responseDto.setAverageHeadshots(headshotStats.getAverageHeadshots());
        responseDto.setAccuracyType(determineAccuracyType(headshotStats));

        return responseDto;
    }

    private String determineCombatType(String primaryClass, double gap) {
        if (gap < 3.0) {
            return "균형형";
        }
        return primaryClass + (gap >= 8.0 ? " 특화형" : " 우세형");
    }

    private String determineAccuracyType(HeadshotStatsDto stats) {
        if (stats.getSampleMatchCount() == 0 || stats.getTotalKills() == 0) {
            return "분석 대기";
        }
        if (stats.getHeadshotRate() >= 35.0) {
            return "정밀형";
        }
        if (stats.getHeadshotRate() >= 20.0) {
            return "안정형";
        }
        return "교전형";
    }

    private double valueOrZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record WeaponClassScore(String label, double score) {
    }
}
