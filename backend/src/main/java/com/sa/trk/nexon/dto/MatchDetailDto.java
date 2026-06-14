package com.sa.trk.nexon.dto;

import java.util.List;

import lombok.Data;

@Data
public class MatchDetailDto {

    private String match_id;
    private String match_type;
    private String match_mode;
    private String date_match;
    private String match_map;
    private List<MatchDetailItemDto> match_detail;
}