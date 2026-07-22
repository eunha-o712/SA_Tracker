package com.sa.trk.clan.dto;

public record ClanBalancedMemberDto(
        Long id,
        String userName,
        String primaryClass,
        String combatType,
        double powerScore,
        int matchCount,
        double winRate,
        double killDeathRatio,
        double averageKill,
        boolean available) {
}
