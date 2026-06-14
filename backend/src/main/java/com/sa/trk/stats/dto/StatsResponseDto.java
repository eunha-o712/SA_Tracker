package com.sa.trk.stats.dto;

import lombok.Data;

@Data
public class StatsResponseDto {

    private String userName;
    private Double winRate;
    private Double killDeathRate;
    private Double assaultRate;
    private Double sniperRate;
    private Double specialRate;
}