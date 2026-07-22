package com.sa.trk.favorite.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.favorite.entity.Favorite;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByOwnerAndUserNameIgnoreCase(AuthUser owner, String userName);
    Optional<Favorite> findByIdAndOwner(Long id, AuthUser owner);
    List<Favorite> findByOwnerOrderByIdDesc(AuthUser owner);
}
