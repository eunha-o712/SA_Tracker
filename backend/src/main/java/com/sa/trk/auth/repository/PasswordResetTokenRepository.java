package com.sa.trk.auth.repository;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token where token.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);

    Optional<PasswordResetToken> findTopByUserOrderByCreatedAtDesc(AuthUser user);
    long countByUserAndCreatedAtGreaterThanEqual(AuthUser user, Instant createdAt);

    @Modifying
    @Query("""
            update PasswordResetToken token
            set token.usedAt = :invalidatedAt
            where token.user = :user
              and token.usedAt is null
            """)
    int invalidateUnusedByUser(
            @Param("user") AuthUser user,
            @Param("invalidatedAt") Instant invalidatedAt);
}
