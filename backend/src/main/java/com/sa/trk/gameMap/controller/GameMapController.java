package com.sa.trk.gameMap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.gameMap.dto.GameMapResponseDto;
import com.sa.trk.gameMap.service.GameMapService;

@RestController
public class GameMapController {

    private final GameMapService gameMapService;

    public GameMapController(GameMapService gameMapService) {
        this.gameMapService = gameMapService;
    }

    @GetMapping("/api/map")
    public GameMapResponseDto getMapMatches(
            @RequestParam("userName") String userName,
            @RequestParam("mapName") String mapName,
            @RequestParam(value = "scope", defaultValue = "RECENT") String scope,
            @RequestParam("matchType") String matchType,
            @RequestParam(value = "page", defaultValue = "1") Integer page) {
        return gameMapService.getMapMatches(
                userName,
                mapName,
                page,
                scope,
                matchType
        );
    }
}
