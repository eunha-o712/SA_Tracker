package com.sa.trk.gameMap.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class GameMapStatsResponseDto {

    private String userName;
    private String version;
    private Integer sampleSize;
    private List<GameMapStatsItemDto> maps = new ArrayList<>();
}
