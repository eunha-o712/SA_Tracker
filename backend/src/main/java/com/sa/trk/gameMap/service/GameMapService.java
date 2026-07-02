package com.sa.trk.gameMap.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sa.trk.gameMap.dto.GameMapResponseDto;
import com.sa.trk.match.dto.MatchDetailResponseDto;
import com.sa.trk.match.dto.MatchListResponseDto;
import com.sa.trk.match.service.MatchService;
import com.sa.trk.nexon.dto.MatchDto;

@Service
public class GameMapService {

    private final MatchService matchService;

    public GameMapService(MatchService matchService) {
        this.matchService = matchService;
    }

    public GameMapResponseDto getMapMatches(
            String userName,
            String mapName,
            Integer page,
            String scope,
            String matchType) {
        MatchListResponseDto matchListResponse = matchService.getMatches(
                userName,
                page,
                scope,
                matchType,
                "ALL",
                mapName
        );

        List<MatchDetailResponseDto> filteredMatches = new ArrayList<>();

        for (MatchDto match : matchListResponse.getMatches()) {
            MatchDetailResponseDto detail = matchService.getMatchDetail(match.getMatch_id());

            filteredMatches.add(detail);
        }

        GameMapResponseDto responseDto = new GameMapResponseDto();
        responseDto.setUserName(userName);
        responseDto.setMapName(mapName);
        responseDto.setMatches(filteredMatches);

        return responseDto;
    }
}
