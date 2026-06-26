package com.ddarahakit.community.config;

import com.ddarahakit.community.config.security.HeaderAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 게이트웨이가 검증한 헤더(X-User-Id/X-User-Role)를 신뢰하는 인증 필터
        http.addFilterBefore(new HeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(auth ->
                auth
                        // CORS 프리플라이트는 항상 허용 (CORS 자체는 게이트웨이에서 처리)
                        .requestMatchers(OPTIONS, "/**").permitAll()

                        // Swagger / 에러 디스패치 / 액추에이터 (공개)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // === 공개(비로그인 허용) 조회 엔드포인트 ===
                        .requestMatchers(GET, "/community/**").permitAll()

                        // === 인증 필요 엔드포인트 (글·댓글·스크랩 작성/수정/삭제·accept) ===
                        .requestMatchers(POST, "/community/**").authenticated()
                        .requestMatchers(PUT, "/community/**").authenticated()
                        .requestMatchers(DELETE, "/community/**").authenticated()

                        // === 그 외 전부 인증 필요 (화이트리스트 기본 정책) ===
                        .anyRequest().authenticated()
        );

        return http.build();
    }
}
