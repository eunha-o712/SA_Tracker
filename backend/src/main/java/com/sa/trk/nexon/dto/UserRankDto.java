package com.sa.trk.nexon.dto;

import lombok.Data;

@Data
public class UserRankDto {

    private String user_name;
    private String grade;
    private Integer grade_exp;
    private Integer grade_ranking;
    private String season_grade;
    private Integer season_grade_exp;
    private Integer season_grade_ranking;
}