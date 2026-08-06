package com.sa.trk.auth.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.entity.EmailVerificationToken;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from EmailVerificationToken token where token.tokenHash = :tokenHash")
    Optional<EmailVerificationToken> findForUpdateByTokenHash(@Param("tokenHash") String tokenHash);

    Optional<EmailVerificationToken> findTopByUserOrderByCreatedAtDesc(AuthUser user);
    void deleteByUserAndUsedAtIsNull(AuthUser user);
}
