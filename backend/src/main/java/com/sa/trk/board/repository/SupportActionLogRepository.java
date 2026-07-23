package com.sa.trk.board.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sa.trk.board.entity.SupportActionLog;

public interface SupportActionLogRepository extends JpaRepository<SupportActionLog, Long> {
    List<SupportActionLog> findByPostIdOrderByCreatedAtAsc(Long postId);
    void deleteByPostId(Long postId);
}
