package com.ddarahakit.identity.domain.user.model.entity;

import com.ddarahakit.identity.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmailVerify extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IDX")
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_idx")
    private User user;

    @Column(name = "UUID", nullable = false, length = 200, unique = true)
    private String uuid;

    @Column(name = "TYPE", nullable = false, length = 50)
    private String type;
}
