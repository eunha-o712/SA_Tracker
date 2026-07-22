package com.sa.trk.ai.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_analysis_cache")
@Getter
@Setter
public class AiAnalysisCache {

    @Id
    @Column(length = 100)
    private String normalizedUserName;

    @Column(nullable = false, length = 64)
    private String snapshotHash;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String responseJson;

    @Column(nullable = false)
    private Instant updatedAt;
}
