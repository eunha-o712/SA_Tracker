package com.sa.trk.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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

class FavoriteServiceTests {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private AuthSessionRepository sessionRepository;

    private AuthUser owner;
    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        owner = owner();
        favoriteService = new FavoriteService(favoriteRepository, sessionRepository);
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session(owner)));
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
        verify(favoriteRepository, never()).save(any(Favorite.class));
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

    private AuthSession session(AuthUser user) {
        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setExpiresAt(Instant.now().plusSeconds(60));
        return session;
    }
}
