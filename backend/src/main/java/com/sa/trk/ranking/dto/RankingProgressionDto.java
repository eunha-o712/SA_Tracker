package com.sa.trk.ranking.dto;

import lombok.Data;

@Data
public class RankingProgressionDto {

    private Integer currentIndex;
    private Integer totalCount;
    private String minimumName;
    private String minimumImage;
    private String maximumName;
    private String maximumImage;
    private String currentName;
    private String currentImage;
    private Long currentMinimumExperience;
    private Long currentMaximumExperience;
    private String nextName;
    private String nextImage;
    private Long nextMinimumExperience;
    private Integer nextBestRanking;
    private Integer nextWorstRanking;
}
