package com.sa.trk.ranking.dto;

import com.sa.trk.common.dto.ImagesDto;

import lombok.Data;

@Data
public class RankingResponseDto {

    private String userName;
    private String grade;
    private Integer gradeRanking;
    private String seasonGrade;
    private Integer seasonGradeRanking;
    private String soloRankMatchTier;
    private Integer soloRankMatchScore;
    private String partyRankMatchTier;
    private Integer partyRankMatchScore;
    private ImagesDto images;
}
