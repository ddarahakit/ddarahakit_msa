package com.ddarahakit.community.domain.community.model;

import com.ddarahakit.community.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "post_scrap",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_idx", "post_idx"})
        }
)
public class PostScrap extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    // === cross-domain FK 평문화 (MSA: user 는 별도 서비스) ===
    @Column(name = "user_idx", nullable = false)
    private Long userIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_idx", nullable = false)
    private Post post;
}
