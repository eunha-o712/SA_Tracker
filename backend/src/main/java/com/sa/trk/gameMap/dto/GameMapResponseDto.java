package com.sa.trk.gameMap.dto;

import java.util.List;

import com.sa.trk.match.dto.MatchDetailResponseDto;

import lombok.Data;

@Data
public class GameMapResponseDto {

    private String userName;
    private String mapName;
    private List<MatchDetailResponseDto> matches;
}