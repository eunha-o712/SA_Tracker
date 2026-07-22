package com.sa.trk.clan.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import com.sa.trk.auth.entity.AuthUser;

@Entity
@Table(
        name = "clan_roster_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_clan_roster_owner_user_name",
                columnNames = {"auth_user_id", "user_name"}
        )
)
@Getter
@Setter
public class ClanMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_user_id", nullable = false)
    private AuthUser owner;

    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;

    @Column(nullable = false, length = 50)
    private String clanName;

    @Column(length = 200)
    private String ouid;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
