package com.sa.trk.favorite.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sa.trk.favorite.entity.Favorite;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserName(String userName);
}