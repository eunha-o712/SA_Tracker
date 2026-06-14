package com.sa.trk.nexon.dto;

import lombok.Data;

@Data
public class MatchDetailItemDto {

    private String team_id;
    private String match_result;
    private String user_name;
    private String season_grade;
    private String clan_name;
    private Integer kill;
    private Integer death;
    private Integer headshot;
    private Double damage;
    private Integer assist;
}