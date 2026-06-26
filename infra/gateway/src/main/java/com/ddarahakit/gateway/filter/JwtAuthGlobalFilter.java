package com.ddarahakit.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

/**
 * 인증의 단일 관문.
 * 1) 클라이언트가 위조해 보낸 X-User-* 헤더를 항상 제거.
 * 2) ATOKEN 쿠키의 액세스 JWT 를 검증해 유효하면 X-User-Id / X-User-Role 주입.
 * 3) 무효/부재 토큰은 익명으로 통과(보호 여부는 다운스트림이 결정 — 심층방어).
 *
 * 모놀리스와 동일한 비밀키·issuer·클레임(idx/role/type=access) 규약을 따른다.
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String ISSUER = "ddarahakit";
    private static final String COOKIE_ACCESS = "ATOKEN";
    private static final String H_USER_ID = "X-User-Id";
    private static final String H_USER_ROLE = "X-User-Role";

    private final SecretKey key;

    public JwtAuthGlobalFilter(@Value("${jwt.secret.key}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        ServerHttpRequest.Builder mutated = request.mutate()
                .headers(h -> {
                    h.remove(H_USER_ID);     // 위조 헤더 제거
                    h.remove(H_USER_ROLE);
                });

        HttpCookie accessToken = request.getCookies().getFirst(COOKIE_ACCESS);
        if (accessToken != null) {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .requireIssuer(ISSUER)
                        .build()
                        .parseSignedClaims(accessToken.getValue())
                        .getPayload();

                if ("access".equals(claims.get("type", String.class))) {
                    mutated.header(H_USER_ID, String.valueOf(claims.get("idx", Long.class)))
                           .header(H_USER_ROLE, claims.get("role", String.class));
                }
            } catch (Exception ignore) {
                // 무효/만료 토큰 → 익명 통과(공개 엔드포인트 보장). 보호는 다운스트림이 401 처리.
            }
        }

        return chain.filter(exchange.mutate().request(mutated.build()).build());
    }

    @Override
    public int getOrder() {
        return -100; // 라우팅보다 먼저 실행
    }
}
