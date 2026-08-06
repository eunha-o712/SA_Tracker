package com.sa.trk.clan.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ClanMemberStatsDto {

    private Long id;
    private String userName;
    private String ouid;
    private Integer matchCount;
    private Integer winCount;
    private Integer drawCount;
    private Integer loseCount;
    private Double winRate;
    private Double averageKillDeathRatio;
    private Boolean available;
    private LocalDateTime statsUpdatedAt;
}
