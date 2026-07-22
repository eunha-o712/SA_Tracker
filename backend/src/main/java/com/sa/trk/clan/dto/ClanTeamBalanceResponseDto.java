package com.sa.trk.clan.dto;

import java.util.List;

public record ClanTeamBalanceResponseDto(
        int selectedCount,
        int analyzedCount,
        int teamSize,
        int balanceScore,
        double powerDifference,
        int variant,
        String basis,
        List<ClanBalancedTeamDto> teams) {
}
