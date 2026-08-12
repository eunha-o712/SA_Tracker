package com.sa.trk.favorite.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

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
import com.sa.trk.match.dto.MatchSummaryResponseDto;
import com.sa.trk.match.service.MatchService;
import com.sa.trk.player.dto.PlayerResponseDto;
import com.sa.trk.player.service.PlayerService;

@Service
public class FavoriteService {

    private static final Duration MATCH_QUERY_PROFILE_DURATION = Duration.ofDays(1);

    private final FavoriteRepository favoriteRepository;
    private final AuthSessionRepository sessionRepository;
    private final PlayerService playerService;
    private final MatchService matchService;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            AuthSessionRepository sessionRepository,
            PlayerService playerService,
            MatchService matchService) {
        this.favoriteRepository = favoriteRepository;
        this.sessionRepository = sessionRepository;
        this.playerService = playerService;
        this.matchService = matchService;
    }

    @Transactional
    public FavoriteResponseDto addFavorite(String rawToken, String userName) {
        AuthUser owner = currentUser(rawToken);
        String normalizedUserName = normalizeUserName(userName);
        PlayerResponseDto player = playerService.getPlayer(normalizedUserName);
        String ouid = player == null ? "" : player.getOuid();
        if (ouid == null || ouid.isBlank()) {
            throw new IllegalArgumentException("Player OUID could not be found.");
        }
        String currentNickname = player.getBasic() == null
                || player.getBasic().getUser_name() == null
                || player.getBasic().getUser_name().isBlank()
                ? normalizedUserName
                : player.getBasic().getUser_name().trim();
        Favorite favorite = favoriteRepository.findByOwnerAndOuid(owner, ouid)
                .or(() -> favoriteRepository.findByOwnerAndUserNameIgnoreCase(owner, normalizedUserName))
                .orElseGet(() -> {
                    Favorite newFavorite = new Favorite();
                    newFavorite.setOwner(owner);
                    return newFavorite;
                });
        favorite.setUserName(currentNickname);
        favorite.setOuid(ouid);
        favorite = favoriteRepository.save(favorite);

        return toResponse(favorite);
    }

    @Transactional
    public List<FavoriteResponseDto> getFavorites(String rawToken) {
        AuthUser owner = currentUser(rawToken);
        return favoriteRepository.findByOwnerOrderByIdDesc(owner).stream()
                .map(this::synchronizeIdentity)
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

    public MatchSummaryResponseDto refreshMatchSummary(String rawToken, Long id) {
        AuthUser owner = currentUser(rawToken);
        Favorite favorite = favoriteRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new IllegalArgumentException("Favorite was not found."));
        favorite = synchronizeIdentity(favorite);
        if (favorite.getOuid() == null || favorite.getOuid().isBlank()) {
            throw new IllegalArgumentException("Player OUID could not be found.");
        }

        boolean discoverAllQueries = favorite.getMatchQueryProfiledAt() == null
                || favorite.getMatchQueryProfiledAt()
                        .plus(MATCH_QUERY_PROFILE_DURATION)
                        .isBefore(Instant.now());
        MatchService.FavoriteMatchSummaryRefresh refresh =
                matchService.getFavoriteMatchSummaryByOuid(
                        favorite.getUserName(),
                        favorite.getOuid(),
                        parseQueryIndexes(favorite.getActiveMatchQueryIndexes()),
                        discoverAllQueries
                );

        favorite.setActiveMatchQueryIndexes(
                refresh.activeQueryIndexes().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","))
        );
        if (refresh.fullDiscovery()) {
            favorite.setMatchQueryProfiledAt(Instant.now());
        }
        favoriteRepository.save(favorite);
        return refresh.summary();
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
        responseDto.setOuid(favorite.getOuid());
        return responseDto;
    }

    private Favorite synchronizeIdentity(Favorite favorite) {
        if (favorite.getOuid() != null && !favorite.getOuid().isBlank()) {
            return favorite;
        }
        PlayerResponseDto player = playerService.getPlayer(favorite.getUserName());
        if (player != null && player.getOuid() != null && !player.getOuid().isBlank()) {
            favorite.setOuid(player.getOuid());
            if (player.getBasic() != null
                    && player.getBasic().getUser_name() != null
                    && !player.getBasic().getUser_name().isBlank()) {
                favorite.setUserName(player.getBasic().getUser_name().trim());
            }
        }
        return favoriteRepository.save(favorite);
    }

    private List<Integer> parseQueryIndexes(String storedIndexes) {
        if (storedIndexes == null || storedIndexes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(storedIndexes.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(this::parseInteger)
                .filter(value -> value != null)
                .toList();
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
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
