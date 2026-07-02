package com.sa.trk.match.dto;

import lombok.Data;

@Data
public class MatchTrendPointDto {

    private String dateMatch;
    private Integer kill;
    private Integer death;
    private Double killDeathRatio;
    private String result;
}
