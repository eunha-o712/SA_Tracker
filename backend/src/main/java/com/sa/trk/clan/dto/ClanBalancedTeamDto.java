package com.sa.trk.clan.dto;

import java.util.List;
import java.util.Map;

public record ClanBalancedTeamDto(
        String key,
        String name,
        double averagePower,
        double averageWinRate,
        double averageKillDeathRatio,
        double averageKill,
        Map<String, Integer> roleCounts,
        List<ClanBalancedMemberDto> members) {
}
