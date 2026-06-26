package com.ddarahakit.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS 를 게이트웨이에서 일원화(서비스별 CORS 제거). 쿠키 인증이라 allowCredentials=true.
 *
 * 0단계에서는 비활성(기본 off): 모든 트래픽이 모놀리스로 가고 모놀리스가 CORS 를 처리하므로,
 * 게이트웨이까지 CORS 헤더를 추가하면 ACAO 헤더가 중복된다. 서비스가 추출돼 모놀리스 CORS 를
 * 제거하는 시점에 {@code gateway.cors.enabled=true} 로 게이트웨이 CORS 를 승격한다.
 */
@Configuration
@ConditionalOnProperty(prefix = "gateway.cors", name = "enabled", havingValue = "true")
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(@Value("${app.allowed-origins}") String allowedOrigins) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(allowedOrigins.split("\\s*,\\s*")));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return new CorsWebFilter(source);
    }
}
