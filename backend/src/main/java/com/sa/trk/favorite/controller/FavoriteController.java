package com.sa.trk.favorite.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.favorite.dto.FavoriteResponseDto;
import com.sa.trk.favorite.service.FavoriteService;

@RestController
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/api/favorite")
    public FavoriteResponseDto addFavorite(@RequestParam("userName") String userName) {
        return favoriteService.addFavorite(userName);
    }

    @GetMapping("/api/favorite")
    public List<FavoriteResponseDto> getFavorites() {
        return favoriteService.getFavorites();
    }

    @DeleteMapping("/api/favorite/{id}")
    public void deleteFavorite(@PathVariable("id") Long id) {
        favoriteService.deleteFavorite(id);
    }
}