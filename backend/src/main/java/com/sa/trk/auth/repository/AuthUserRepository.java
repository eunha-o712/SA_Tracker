package com.sa.trk.auth.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sa.trk.auth.entity.AuthUser;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
    Optional<AuthUser> findByLoginIdIgnoreCase(String loginId);
    boolean existsByLoginIdIgnoreCase(String loginId);
    Optional<AuthUser> findByEmailIgnoreCase(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from AuthUser user where lower(user.email) = lower(:email)")
    Optional<AuthUser> findForUpdateByEmailIgnoreCase(@Param("email") String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<AuthUser> findByOuid(String ouid);
    boolean existsByOuid(String ouid);
}
