package com.sa.trk.match.dto;

import java.util.List;

import com.sa.trk.nexon.dto.MatchDetailItemDto;

import lombok.Data;

@Data
public class MatchDetailResponseDto {

    private String matchId;
    private String matchType;
    private String matchMode;
    private String dateMatch;
    private String matchMap;
    private List<MatchDetailItemDto> matchDetail;
}