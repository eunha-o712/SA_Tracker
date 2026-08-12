package com.sa.trk.ranking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "rank_progression_rule",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rank_progression_type_order",
                        columnNames = {"progression_type", "display_order"}
                ),
                @UniqueConstraint(
                        name = "uk_rank_progression_type_name",
                        columnNames = {"progression_type", "rank_name"}
                )
        }
)
@Getter
@Setter
public class RankProgressionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "progression_type", nullable = false, length = 24)
    private RankProgressionType progressionType;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "rank_group", nullable = false, length = 20)
    private String rankGroup;

    @Column(name = "rank_name", nullable = false, length = 40)
    private String rankName;

    private Long minimumExperience;
    private Long maximumExperience;
    private Integer bestRanking;
    private Integer worstRanking;
}
