package com.sa.trk.board.entity;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "board_posts")
@Getter
@Setter
public class BoardPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoardType type;

    @Column(nullable = false, length = 120)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 20)
    private String authorName;

    @Column(nullable = false)
    private long viewCount;

    @Column(nullable = false)
    private boolean notice;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SupportCategory supportCategory;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SupportStatus supportStatus;

    @Column(length = 20)
    private String claimedSuddenNickname;

    @Column(length = 80)
    private String claimedOuid;

    @Column
    private Long claimedOwnerId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String adminResponse;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OuidDisputeResolution resolutionAction;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AccountSanctionAction accountSanctionAction;

    @Column
    private Boolean claimantVerified;

    @Column
    private Long handledById;

    @Column
    private Instant handledAt;

    @ElementCollection
    @CollectionTable(name = "board_post_images", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "image_url", nullable = false, length = 255)
    private List<String> imageUrls = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
