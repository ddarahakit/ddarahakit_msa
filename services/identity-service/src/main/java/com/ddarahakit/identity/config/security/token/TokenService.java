package com.ddarahakit.identity.config.security.token;

import com.ddarahakit.identity.common.exception.BaseException;
import com.ddarahakit.identity.config.security.AuthUserDetails;
import com.ddarahakit.identity.config.security.JwtProvider;
import com.ddarahakit.identity.domain.user.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.ddarahakit.identity.common.model.BaseResponseStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.token.refresh-max-age}")
    private int refreshMaxAge;

    /**
     * 토큰 쌍 생성 (로그인 시)
     * - 새로운 Token Family 생성
     */
    @Transactional
    public TokenPair createTokenPair(AuthUserDetails authUserDetails, HttpServletRequest request) {
        String tokenFamily = UUID.randomUUID().toString();
        return generateTokenPair(authUserDetails, tokenFamily, request);
    }

    /**
     * 토큰 갱신 (Refresh Token Rotation)
     * - 기존 리프레시 토큰을 사용 처리하고 새 토큰 쌍 발급
     * - 이미 사용된 토큰으로 갱신 시도 시 탈취로 간주하고 전체 무효화
     */
    @Transactional
    public TokenPair refreshTokenPair(String refreshTokenValue, HttpServletRequest request) {
        // 1. 토큰 검증
        if (!jwtProvider.validateToken(refreshTokenValue)) {
            throw BaseException.of(EXPIRED_JWT);
        }

        // 2. 토큰 타입 검증
        String tokenType = jwtProvider.extractTokenType(refreshTokenValue);
        if (!"refresh".equals(tokenType)) {
            log.warn("[TOKEN] 잘못된 토큰 타입으로 갱신 시도: {}", tokenType);
            throw BaseException.of(INVALID_JWT);
        }

        // 3. JTI로 DB에서 토큰 조회 (비관적 락 - Race Condition 방지)
        String tokenId = jwtProvider.extractTokenId(refreshTokenValue);
        RefreshToken storedToken = refreshTokenRepository.findByTokenIdWithLock(tokenId)
                .orElseThrow(() -> {
                    log.warn("[TOKEN] DB에 존재하지 않는 토큰으로 갱신 시도: {}", tokenId);
                    return BaseException.of(INVALID_JWT);
                });

        // 4. 토큰 재사용 감지 (탈취 의심)
        if (storedToken.isUsed()) {
            log.error("[SECURITY] 토큰 재사용 감지! Token Family 전체 무효화. tokenFamily={}, userIdx={}",
                    storedToken.getTokenFamily(), storedToken.getUser().getIdx());
            refreshTokenRepository.revokeAllByTokenFamily(storedToken.getTokenFamily());
            throw BaseException.of(INVALID_JWT);
        }

        // 5. 토큰 유효성 검증
        if (!storedToken.isValid()) {
            log.warn("[TOKEN] 무효화되었거나 만료된 토큰으로 갱신 시도");
            throw BaseException.of(INVALID_JWT);
        }

        // 6. 기존 토큰 사용 처리
        storedToken.markAsUsed();

        // 7. 새 토큰 쌍 발급 (같은 Token Family 유지)
        User user = storedToken.getUser();
        AuthUserDetails authUserDetails = AuthUserDetails.of(user);

        return generateTokenPair(authUserDetails, storedToken.getTokenFamily(), request);
    }

    /**
     * 로그아웃 처리 (현재 토큰만 무효화)
     */
    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null) {
            return;
        }

        try {
            String tokenId = jwtProvider.extractTokenId(refreshTokenValue);
            refreshTokenRepository.findByTokenId(tokenId)
                    .ifPresent(RefreshToken::revoke);
        } catch (Exception e) {
            log.warn("[TOKEN] 로그아웃 처리 중 오류: {}", e.getMessage());
        }
    }

    /**
     * 전체 로그아웃 (모든 기기에서 로그아웃)
     */
    @Transactional
    public void logoutAll(Long userIdx) {
        refreshTokenRepository.revokeAllByUserIdx(userIdx);
        log.info("[TOKEN] 사용자의 모든 토큰 무효화. userIdx={}", userIdx);
    }

    /**
     * 만료/무효화된 토큰 정리 (스케줄러에서 호출)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
        log.info("[TOKEN] 만료/무효화된 토큰 정리 완료");
    }

    // ================================
    // Private Methods
    // ================================

    private TokenPair generateTokenPair(AuthUserDetails authUserDetails, String tokenFamily, HttpServletRequest request) {
        String tokenId = UUID.randomUUID().toString();

        // 액세스 토큰 생성
        String accessToken = jwtProvider.generateAccessToken(authUserDetails);

        // 리프레시 토큰 생성
        String refreshToken = jwtProvider.generateRefreshToken(authUserDetails, tokenId);

        // 리프레시 토큰 DB 저장
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .tokenId(tokenId)
                .tokenFamily(tokenFamily)
                .user(authUserDetails.toEntity())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshMaxAge / 1000))
                .revoked(false)
                .used(false)
                .ipAddress(getClientIp(request))
                .userAgent(getUserAgent(request))
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return new TokenPair(accessToken, refreshToken);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;

        // 신뢰 프록시(nginx)가 Cloudflare CF-Connecting-IP 로 해석한 실제 클라이언트 IP 를 X-Real-IP 로 덮어쓴다.
        // X-Forwarded-For 의 "첫 번째 IP" 는 클라이언트가 임의로 위조할 수 있으므로 우선순위에서 제외하고,
        // 신뢰값인 X-Real-IP 를 우선 사용한다.
        String ip = request.getHeader("X-Real-IP");

        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            // 폴백: X-Forwarded-For 는 "client, proxy1, ... , 신뢰프록시" 순서로 신뢰 프록시가 마지막에 덧붙인다.
            // 따라서 위조 가능한 첫 IP 가 아니라 마지막(신뢰 프록시가 추가한) IP 를 사용한다.
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank() && !"unknown".equalsIgnoreCase(forwarded)) {
                String[] parts = forwarded.split(",");
                ip = parts[parts.length - 1].trim();
            }
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 500) {
            userAgent = userAgent.substring(0, 500);
        }
        return userAgent;
    }

    /**
     * 토큰 쌍 DTO
     */
    public record TokenPair(String accessToken, String refreshToken) {
    }
}
