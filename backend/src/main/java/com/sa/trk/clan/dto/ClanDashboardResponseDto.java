package com.sa.trk.clan.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ClanDashboardResponseDto {

    private String clanName;
    private Integer memberCount;
    private Integer analyzedMemberCount;
    private Integer totalMatchCount;
    private Double averageWinRate;
    private Double averageKillDeathRatio;
    private List<ClanMemberStatsDto> members = new ArrayList<>();
}
