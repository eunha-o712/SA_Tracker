package com.sa.trk.ranking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.ranking.dto.RankingResponseDto;
import com.sa.trk.ranking.service.RankingService;

@RestController
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/api/ranking")
    public RankingResponseDto getRanking(@RequestParam("userName") String userName) {
        return rankingService.getRanking(userName);
    }
}