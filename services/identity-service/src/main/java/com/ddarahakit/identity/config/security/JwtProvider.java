package com.ddarahakit.identity.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;


@Slf4j
@RequiredArgsConstructor
@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String TOKEN_ID_CLAIM = "jti";
    private static final String ISSUER = "ddarahakit";

    @Value("${app.token.access-max-age}")
    private int accessMaxAge;

    @Value("${app.token.refresh-max-age}")
    private int refreshMaxAge;

    @Value("${jwt.secret.key}")
    private String secretKey;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    /**
     * 액세스 토큰 생성
     * - 짧은 만료 시간
     * - 사용자 정보 포함 (idx, role)
     */
    public String generateAccessToken(AuthUserDetails authUserDetails) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessMaxAge);

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(authUserDetails.getEmail())
                .claim("idx", authUserDetails.getIdx())
                .claim("role", authUserDetails.getRole())
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 리프레시 토큰 생성
     * - 긴 만료 시간
     * - JTI(토큰 ID) 포함하여 DB와 연동
     * - 최소한의 정보만 포함
     */
    public String generateRefreshToken(AuthUserDetails authUserDetails, String tokenId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshMaxAge);

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(authUserDetails.getEmail())
                .claim("idx", authUserDetails.getIdx())
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH)
                .claim(TOKEN_ID_CLAIM, tokenId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * JWT에서 Claims 추출
     * - 만료된 토큰에서는 Claims를 추출하지 않음 (보안 강화)
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("[TOKEN] 만료된 토큰");
            throw e;
        } catch (JwtException e) {
            log.warn("[TOKEN] 유효하지 않은 토큰: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 만료된 토큰에서도 Claims 추출 (리프레시 토큰 검증용)
     * - 주의: 이 메서드는 토큰 갱신 시에만 사용
     */
    public Claims extractClaimsAllowExpired(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractIdx(String token) {
        return extractClaims(token).get("idx", Long.class);
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public String extractTokenType(String token) {
        try {
            return extractClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
        } catch (ExpiredJwtException e) {
            return e.getClaims().get(TOKEN_TYPE_CLAIM, String.class);
        }
    }

    public String extractTokenId(String token) {
        try {
            return extractClaims(token).get(TOKEN_ID_CLAIM, String.class);
        } catch (ExpiredJwtException e) {
            return e.getClaims().get(TOKEN_ID_CLAIM, String.class);
        }
    }

    /**
     * 토큰 유효성 검증
     * - 서명 검증
     * - 만료 여부 확인
     * - Issuer 검증
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 액세스 토큰 타입 검증
     */
    public boolean isAccessToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return TOKEN_TYPE_ACCESS.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 리프레시 토큰 타입 검증
     */
    public boolean isRefreshToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return TOKEN_TYPE_REFRESH.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

}
