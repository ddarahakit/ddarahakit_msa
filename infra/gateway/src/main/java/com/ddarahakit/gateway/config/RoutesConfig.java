package com.ddarahakit.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 0단계(Strangler): 모든 요청을 기존 모놀리스로 정적 라우팅.
 * 서비스가 추출될 때마다 해당 경로 라우트를 lb://<service> 로 추가하면(아래 위에 둠) 점진 전환된다.
 */
@Configuration
public class RoutesConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder,
                                     @Value("${monolith.uri}") String monolithUri) {
        return builder.routes()
                // ── 1단계: identity-service (인증/프로필) ──
                // 주의: 마이페이지 집계(/user/ordered·myreview·mypost·myquestion·payments·study/weekly)는
                //       identity 에 없으므로 여기서 제외 → 아래 monolith 캐치올로 떨어진다.
                .route("identity", r -> r.path(
                                "/user/login", "/user/social/**", "/user/logout", "/user/logout/all",
                                "/user/token/**", "/user/signup", "/user/email/**", "/user/check",
                                "/user/uuid/**", "/user/password/**", "/user/profile",
                                "/oauth2/**", "/login/oauth2/**")
                        .uri("lb://identity-service"))

                // ── 2단계: community-service ──
                .route("community", r -> r.path("/community/**").uri("lb://community-service"))

                // ── 3단계: commerce-service (주문/장바구니) ──
                .route("commerce", r -> r.path("/orders/**", "/cart/**").uri("lb://commerce-service"))

                // ── 4단계: review-service (수강평) ──
                .route("review", r -> r.path("/review/**").uri("lb://review-service"))

                // ── 5단계: course-service (코어: 코스/강의/로드맵/통계) ──
                .route("course", r -> r.path("/course/**", "/roadmap/**", "/stats/**").uri("lb://course-service"))

                // 나머지 전부 모놀리스로 (Strangler 잔여)
                .route("monolith", r -> r.path("/**").uri(monolithUri))
                .build();
    }
}
