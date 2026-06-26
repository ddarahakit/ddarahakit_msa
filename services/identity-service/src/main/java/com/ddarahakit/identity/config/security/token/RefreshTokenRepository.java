package com.ddarahakit.identity.config.security.token;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenId(String tokenId);

    /**
     * 토큰 갱신 시 Race Condition 방지를 위한 비관적 락 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenId = :tokenId")
    Optional<RefreshToken> findByTokenIdWithLock(@Param("tokenId") String tokenId);

    /**
     * 특정 토큰 패밀리의 모든 토큰 무효화 (탈취 감지 시)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.tokenFamily = :tokenFamily")
    void revokeAllByTokenFamily(@Param("tokenFamily") String tokenFamily);

    /**
     * 특정 사용자의 모든 토큰 무효화 (로그아웃, 비밀번호 변경 시)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.idx = :userIdx")
    void revokeAllByUserIdx(@Param("userIdx") Long userIdx);

    /**
     * 만료된 토큰 삭제 (배치 작업용)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    void deleteExpiredAndRevokedTokens(@Param("now") LocalDateTime now);

    /**
     * 사용자의 활성 토큰 수 조회 (동시 로그인 제한용)
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.idx = :userIdx AND rt.revoked = false AND rt.used = false AND rt.expiresAt > :now")
    long countActiveTokensByUserIdx(@Param("userIdx") Long userIdx, @Param("now") LocalDateTime now);
}
