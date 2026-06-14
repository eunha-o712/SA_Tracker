package com.sa.trk.stats.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.stats.dto.StatsResponseDto;
import com.sa.trk.stats.service.StatsService;

@RestController
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/api/stats")
    public StatsResponseDto getStats(@RequestParam("userName") String userName) {
        return statsService.getStats(userName);
    }
}