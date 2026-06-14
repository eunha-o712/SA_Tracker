package com.sa.trk.nexon.dto;

import lombok.Data;

@Data
public class MatchDto {

    private String match_id;
    private String match_type;
    private String match_mode;
    private String date_match;
    private String match_result;
    private Integer kill;
    private Integer death;
    private Integer assist;
}