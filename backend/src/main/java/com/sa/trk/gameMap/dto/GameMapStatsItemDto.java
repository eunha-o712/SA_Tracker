package com.sa.trk.gameMap.dto;

import lombok.Data;

@Data
public class GameMapStatsItemDto {

    private String mapName;
    private Integer matchCount;
    private Integer winCount;
    private Integer drawCount;
    private Integer loseCount;
    private Double winRate;
    private Double averageKill;
    private Double averageDeath;
    private Double killDeathRatio;
}
