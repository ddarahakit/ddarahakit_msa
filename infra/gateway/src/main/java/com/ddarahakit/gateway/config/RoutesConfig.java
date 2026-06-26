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
                // ── 1단계부터 추가될 예: identity-service ──
                // .route("identity", r -> r.path("/user/**", "/oauth2/**", "/login/oauth2/**")
                //         .uri("lb://identity-service"))

                // 0단계: 나머지 전부 모놀리스로
                .route("monolith", r -> r.path("/**").uri(monolithUri))
                .build();
    }
}
