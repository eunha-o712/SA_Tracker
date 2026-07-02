package com.sa.trk.match.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MatchSummaryItemDto {

    private String key;
    private String label;
    private String latestMatchDate;
    private Integer matchCount;
    private Double averageKill;
    private Double averageDeath;
    private Double averageAssist;
    private Integer winCount;
    private Integer drawCount;
    private Integer loseCount;
    private Double winRate;
    private List<String> recentResults = new ArrayList<>();
}
