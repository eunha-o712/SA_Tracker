package com.sa.trk.match.dto;

import java.util.List;

import com.sa.trk.nexon.dto.MatchDto;

import lombok.Data;

@Data
public class MatchListResponseDto {

    private String userName;
    private Integer page;
    private Integer size;
    private Integer totalCount;
    private List<MatchDto> matches;
}