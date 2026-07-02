package com.sa.trk.match.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MatchSummaryResponseDto {

    private String userName;
    private String primaryMode;
    private String primaryType;
    private String playStyle;
    private List<MatchTrendPointDto> killDeathTrend = new ArrayList<>();
    private List<MatchSummaryItemDto> summaries = new ArrayList<>();
}
