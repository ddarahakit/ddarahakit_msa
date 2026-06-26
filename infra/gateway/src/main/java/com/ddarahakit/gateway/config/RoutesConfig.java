package com.ddarahakit.gateway.config;

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
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // ── 1단계: identity-service (인증/프로필) ──
                .route("identity", r -> r.path(
                                "/user/login", "/user/social/**", "/user/logout", "/user/logout/all",
                                "/user/token/**", "/user/signup", "/user/email/**", "/user/check",
                                "/user/uuid/**", "/user/password/**", "/user/profile",
                                "/oauth2/**", "/login/oauth2/**")
                        .uri("lb://identity-service"))

                // ── 2단계: community-service (+마이페이지: 내 글/질문) ──
                .route("community", r -> r.path(
                                "/community/**", "/user/mypost", "/user/myquestion")
                        .uri("lb://community-service"))

                // ── 3단계: commerce-service (주문/장바구니 +마이페이지: 결제내역) ──
                .route("commerce", r -> r.path(
                                "/orders/**", "/cart/**", "/user/payments")
                        .uri("lb://commerce-service"))

                // ── 4단계: review-service (수강평 +마이페이지: 내 리뷰) ──
                .route("review", r -> r.path(
                                "/review/**", "/user/myreview")
                        .uri("lb://review-service"))

                // ── 5단계: course-service (코어 +마이페이지: 내 강의실/주간학습) ──
                .route("course", r -> r.path(
                                "/course/**", "/roadmap/**", "/stats/**",
                                "/user/ordered", "/user/study/**")
                        .uri("lb://course-service"))

                // ── 신규: mentoring-service (멘토링) ──
                .route("mentoring", r -> r.path("/mentoring/**").uri("lb://mentoring-service"))

                // 모놀리스 은퇴 완료 — 캐치올 제거. 미라우팅 경로는 404.
                .build();
    }
}
