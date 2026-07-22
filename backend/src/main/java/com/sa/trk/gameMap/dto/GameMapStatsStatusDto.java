package com.sa.trk.gameMap.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameMapStatsStatusDto {

    private String version;
    private boolean upToDate;
}
