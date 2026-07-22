package com.sa.trk.favorite.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public FavoriteResponseDto addFavorite(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("userName") String userName) {
        return favoriteService.addFavorite(bearerToken(authorization), userName);
    }

    @GetMapping("/api/favorite")
    public List<FavoriteResponseDto> getFavorites(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return favoriteService.getFavorites(bearerToken(authorization));
    }

    @DeleteMapping("/api/favorite/{id}")
    public void deleteFavorite(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") Long id) {
        favoriteService.deleteFavorite(bearerToken(authorization), id);
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
