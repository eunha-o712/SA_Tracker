package com.sa.trk.search.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.player.dto.PlayerResponseDto;
import com.sa.trk.search.service.SearchService;

@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search")
    public PlayerResponseDto searchPlayer(@RequestParam("keyword") String keyword) {
        return searchService.searchPlayer(keyword);
    }
}