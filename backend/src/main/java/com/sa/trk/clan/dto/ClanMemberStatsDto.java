package com.sa.trk.clan.dto;

import lombok.Data;

@Data
public class ClanMemberStatsDto {

    private Long id;
    private String userName;
    private Integer matchCount;
    private Integer winCount;
    private Integer drawCount;
    private Integer loseCount;
    private Double winRate;
    private Double averageKillDeathRatio;
    private Boolean available;
}
