package com.sa.trk.weapon.dto;

import lombok.Data;

@Data
public class WeaponStatsResponseDto {

    private String userName;
    private Double assaultRate;
    private Double sniperRate;
    private Double specialRate;
    private String primaryClass;
    private String combatType;
    private Double primaryGap;
    private Double specializationIndex;
    private Integer sampleMatchCount;
    private Integer totalKills;
    private Integer totalHeadshots;
    private Double headshotRate;
    private Double averageHeadshots;
    private String accuracyType;
}
