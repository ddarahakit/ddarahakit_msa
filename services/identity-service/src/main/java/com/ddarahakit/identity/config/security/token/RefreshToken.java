package com.ddarahakit.identity.config.security.token;

import com.ddarahakit.identity.common.model.BaseEntity;
import com.ddarahakit.identity.domain.user.model.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 리프레시 토큰 엔티티
 * - DB에 저장하여 토큰 무효화 및 탈취 감지 가능
 * - Token Family를 통해 토큰 탈취 시 전체 무효화
 */
@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_token_id", columnList = "tokenId"),
        @Index(name = "idx_token_family", columnList = "tokenFamily"),
        @Index(name = "idx_user_idx", columnList = "user_idx")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(nullable = false, unique = true, length = 36)
    private String tokenId;  // JTI (JWT ID)

    @Column(nullable = false, length = 36)
    private String tokenFamily;  // 토큰 패밀리 (최초 로그인 시 생성, 갱신 시 유지)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;  // 무효화 여부

    @Column(nullable = false)
    private boolean used;  // 사용 여부 (Rotation 감지용)

    @Column(length = 45)
    private String ipAddress;  // 발급 시 IP

    @Column(length = 500)
    private String userAgent;  // 발급 시 User-Agent

    public void markAsUsed() {
        this.used = true;
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isValid() {
        return !revoked && !used && expiresAt.isAfter(LocalDateTime.now());
    }
}
