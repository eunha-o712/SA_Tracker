package com.sa.trk.match.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.match.dto.MatchDetailResponseDto;
import com.sa.trk.match.dto.MatchListResponseDto;
import com.sa.trk.match.dto.MatchSummaryResponseDto;
import com.sa.trk.match.service.MatchService;

@RestController
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/api/match")
    public MatchListResponseDto getMatches(
            @RequestParam("userName") String userName,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "scope", defaultValue = "RECENT") String scope,
            @RequestParam("matchType") String matchType,
            @RequestParam(value = "matchMode", defaultValue = "ALL") String matchMode,
            @RequestParam(value = "matchMap", defaultValue = "ALL") String matchMap) {

        return matchService.getMatches(
                userName,
                page,
                scope,
                matchType,
                matchMode,
                matchMap
        );
    }

    @GetMapping("/api/match/detail")
    public MatchDetailResponseDto getMatchDetail(
            @RequestParam("matchId") String matchId) {

        return matchService.getMatchDetail(matchId);
    }

    @GetMapping("/api/match/summary")
    public MatchSummaryResponseDto getMatchSummary(
            @RequestParam("userName") String userName) {

        return matchService.getMatchSummary(userName);
    }
}
