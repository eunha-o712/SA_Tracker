package com.sa.trk.favorite.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.trk.auth.entity.AuthSession;
import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.repository.AuthSessionRepository;
import com.sa.trk.auth.service.AuthException;
import com.sa.trk.favorite.dto.FavoriteResponseDto;
import com.sa.trk.favorite.entity.Favorite;
import com.sa.trk.favorite.repository.FavoriteRepository;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final AuthSessionRepository sessionRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, AuthSessionRepository sessionRepository) {
        this.favoriteRepository = favoriteRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public FavoriteResponseDto addFavorite(String rawToken, String userName) {
        AuthUser owner = currentUser(rawToken);
        String normalizedUserName = normalizeUserName(userName);
        Favorite favorite = favoriteRepository.findByOwnerAndUserNameIgnoreCase(owner, normalizedUserName)
                .orElseGet(() -> {
                    Favorite newFavorite = new Favorite();
                    newFavorite.setOwner(owner);
                    newFavorite.setUserName(normalizedUserName);
                    return favoriteRepository.save(newFavorite);
                });

        return toResponse(favorite);
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponseDto> getFavorites(String rawToken) {
        AuthUser owner = currentUser(rawToken);
        return favoriteRepository.findByOwnerOrderByIdDesc(owner).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteFavorite(String rawToken, Long id) {
        AuthUser owner = currentUser(rawToken);
        if (id == null || id < 1) {
            throw new IllegalArgumentException("Favorite id is invalid.");
        }

        Favorite favorite = favoriteRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("Favorite was not found."));
        favoriteRepository.delete(favorite);
    }

    private AuthUser currentUser(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw unauthorized();
        }

        AuthSession session = sessionRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(this::unauthorized);
        if (!session.getExpiresAt().isAfter(Instant.now())) {
            throw unauthorized();
        }

        return session.getUser();
    }

    private FavoriteResponseDto toResponse(Favorite favorite) {
        FavoriteResponseDto responseDto = new FavoriteResponseDto();
        responseDto.setId(favorite.getId());
        responseDto.setUserName(favorite.getUserName());
        return responseDto;
    }

    private String normalizeUserName(String userName) {
        if (userName == null || userName.isBlank()) {
            throw new IllegalArgumentException("Enter a nickname.");
        }
        return userName.trim();
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Session could not be verified.", exception);
        }
    }

    private AuthException unauthorized() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Login is required.");
    }
}
