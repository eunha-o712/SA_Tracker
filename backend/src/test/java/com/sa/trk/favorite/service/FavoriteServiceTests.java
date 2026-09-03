package com.sa.trk.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.auth.entity.AuthSession;
import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.repository.AuthSessionRepository;
import com.sa.trk.favorite.entity.Favorite;
import com.sa.trk.favorite.repository.FavoriteRepository;
import com.sa.trk.match.dto.MatchSummaryResponseDto;
import com.sa.trk.match.service.MatchService;
import com.sa.trk.nexon.dto.UserBasicDto;
import com.sa.trk.player.dto.PlayerResponseDto;
import com.sa.trk.player.service.PlayerService;

class FavoriteServiceTests {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private AuthSessionRepository sessionRepository;

    @Mock
    private PlayerService playerService;

    @Mock
    private MatchService matchService;

    private AuthUser owner;
    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        owner = owner();
        favoriteService = new FavoriteService(
                favoriteRepository,
                sessionRepository,
                playerService,
                matchService
        );
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session(owner)));
        when(playerService.getPlayer("agent")).thenReturn(player("agent", "ouid-agent"));
        when(favoriteRepository.save(any(Favorite.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void trimsAndStoresANewFavoriteForCurrentUser() {
        when(favoriteRepository.findByOwnerAndUserNameIgnoreCase(owner, "agent")).thenReturn(Optional.empty());
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(invocation -> {
            Favorite saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        var result = favoriteService.addFavorite("session-token", "  agent  ");

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getUserName()).isEqualTo("agent");
        assertThat(result.getOuid()).isEqualTo("ouid-agent");
    }

    @Test
    void returnsExistingFavoriteWithoutDuplicatingIt() {
        Favorite existing = new Favorite();
        existing.setId(3L);
        existing.setOwner(owner);
        existing.setUserName("agent");
        when(favoriteRepository.findByOwnerAndUserNameIgnoreCase(owner, "agent")).thenReturn(Optional.of(existing));

        var result = favoriteService.addFavorite("session-token", "agent");

        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getOuid()).isEqualTo("ouid-agent");
        verify(favoriteRepository).save(existing);
    }

    @Test
    void rejectsBlankNicknames() {
        assertThatThrownBy(() -> favoriteService.addFavorite("session-token", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Enter a nickname.");
    }

    @Test
    void rejectsDeletingMissingFavoritesForCurrentUser() {
        when(favoriteRepository.findByIdAndOwner(99L, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.deleteFavorite("session-token", 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Favorite was not found.");
        verify(favoriteRepository, never()).delete(any(Favorite.class));
    }

    @Test
    void reusesActiveMatchQueriesUntilPeriodicDiscoveryIsDue() {
        Favorite favorite = new Favorite();
        favorite.setId(8L);
        favorite.setOwner(owner);
        favorite.setUserName("agent");
        favorite.setOuid("ouid-agent");
        favorite.setActiveMatchQueryIndexes("0,3,7");
        favorite.setMatchQueryProfiledAt(Instant.now());
        MatchSummaryResponseDto summary = new MatchSummaryResponseDto();
        when(favoriteRepository.findByIdAndOwner(8L, owner)).thenReturn(Optional.of(favorite));
        when(matchService.getFavoriteMatchSummaryByOuid(
                "agent",
                "ouid-agent",
                List.of(0, 3, 7),
                false
        )).thenReturn(new MatchService.FavoriteMatchSummaryRefresh(
                summary,
                List.of(0, 7),
                false
        ));

        var result = favoriteService.refreshMatchSummary("session-token", 8L);

        assertThat(result).isSameAs(summary);
        assertThat(favorite.getActiveMatchQueryIndexes()).isEqualTo("0,7");
        verify(favoriteRepository).save(favorite);
    }

    @Test
    void addsAViewedMatchTypeToTheFavoriteRefreshRanges() {
        Favorite favorite = new Favorite();
        favorite.setId(8L);
        favorite.setOwner(owner);
        favorite.setUserName("agent");
        favorite.setOuid("ouid-agent");
        favorite.setActiveMatchQueryIndexes("0,3,7");
        when(matchService.resolveFavoriteQueryIndexes(
                "CLAN",
                "폭파미션",
                "퀵매치 클랜전"
        )).thenReturn(List.of(5));
        when(favoriteRepository.findByOwnerAndOuid(owner, "ouid-agent"))
                .thenReturn(Optional.of(favorite));

        favoriteService.recordMatchQueryActivity(
                "session-token",
                "agent",
                "ouid-agent",
                "CLAN",
                "폭파미션",
                "퀵매치 클랜전"
        );

        assertThat(favorite.getActiveMatchQueryIndexes()).isEqualTo("0,3,5,7");
        verify(favoriteRepository).save(favorite);
    }

    @Test
    void refreshesProfileMetadataAndStoresARenamedFavorite() {
        Favorite favorite = new Favorite();
        favorite.setId(8L);
        favorite.setOwner(owner);
        favorite.setUserName("old-name");
        favorite.setOuid("ouid-agent");
        PlayerResponseDto refreshedPlayer = player("new-name", "ouid-agent");
        when(favoriteRepository.findByIdAndOwner(8L, owner)).thenReturn(Optional.of(favorite));
        when(playerService.getFavoritePlayerByOuid("ouid-agent")).thenReturn(refreshedPlayer);

        var result = favoriteService.refreshFavoriteProfile("session-token", 8L);

        assertThat(result).isSameAs(refreshedPlayer);
        assertThat(favorite.getUserName()).isEqualTo("new-name");
        verify(favoriteRepository).save(favorite);
    }

    private AuthUser owner() {
        AuthUser user = new AuthUser();
        user.setId(1L);
        user.setEmail("member@satrk.gg");
        user.setLoginId("user001");
        user.setDisplayName("user001");
        user.setPasswordSalt("salt");
        user.setPasswordHash("hash");
        user.setCreatedAt(Instant.now());
        return user;
    }

    private PlayerResponseDto player(String userName, String ouid) {
        UserBasicDto basic = new UserBasicDto();
        basic.setUser_name(userName);
        PlayerResponseDto player = new PlayerResponseDto();
        player.setUserName(userName);
        player.setOuid(ouid);
        player.setBasic(basic);
        return player;
    }

    private AuthSession session(AuthUser user) {
        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setExpiresAt(Instant.now().plusSeconds(60));
        return session;
    }
}
