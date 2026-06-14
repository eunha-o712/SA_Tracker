package com.sa.trk.favorite.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sa.trk.favorite.dto.FavoriteResponseDto;
import com.sa.trk.favorite.entity.Favorite;
import com.sa.trk.favorite.repository.FavoriteRepository;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    public FavoriteResponseDto addFavorite(String userName) {
        Favorite favorite = favoriteRepository.findByUserName(userName)
                .orElseGet(() -> {
                    Favorite newFavorite = new Favorite();
                    newFavorite.setUserName(userName);
                    return favoriteRepository.save(newFavorite);
                });

        FavoriteResponseDto responseDto = new FavoriteResponseDto();
        responseDto.setId(favorite.getId());
        responseDto.setUserName(favorite.getUserName());

        return responseDto;
    }

    public List<FavoriteResponseDto> getFavorites() {
        return favoriteRepository.findAll().stream()
                .map(favorite -> {
                    FavoriteResponseDto responseDto = new FavoriteResponseDto();
                    responseDto.setId(favorite.getId());
                    responseDto.setUserName(favorite.getUserName());
                    return responseDto;
                })
                .toList();
    }

    public void deleteFavorite(Long id) {
        favoriteRepository.deleteById(id);
    }
}