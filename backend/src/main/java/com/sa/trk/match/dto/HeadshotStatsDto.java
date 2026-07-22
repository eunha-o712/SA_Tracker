package com.sa.trk.match.dto;

import lombok.Data;

@Data
public class HeadshotStatsDto {

    private int sampleMatchCount;
    private int totalKills;
    private int totalHeadshots;
    private double headshotRate;
    private double averageHeadshots;
}
