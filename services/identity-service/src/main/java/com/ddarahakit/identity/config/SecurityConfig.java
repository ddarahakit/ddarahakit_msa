package com.ddarahakit.identity.config;

import com.ddarahakit.identity.config.security.JwtAuthenticationEntryPoint;
import com.ddarahakit.identity.config.security.JwtAuthenticationFilter;
import com.ddarahakit.identity.config.security.oauth.HttpCookieOAuth2AuthorizedClientRepository;
import com.ddarahakit.identity.config.security.oauth.OAuth2AuthenticationFailureHandler;
import com.ddarahakit.identity.config.security.oauth.OAuth2AuthenticationSuccessHandler;
import com.ddarahakit.identity.domain.user.service.OAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.http.HttpMethod.*;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // CORS 는 게이트웨이에서 일원화 처리한다(서비스별 CORS 제거). identity 는 게이트웨이를 통해서만
    // 외부에 노출되므로 자체 CORS 를 두면 ACAO 중복/Origin 불일치로 충돌한다.
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final HttpCookieOAuth2AuthorizedClientRepository httpCookieOAuth2AuthorizedClientRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authProvider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // CORS 비활성: 게이트웨이가 단일 CORS 주체. (다운스트림에서 ACAO 를 추가하면 중복되어 브라우저가 거부)
        http.cors(AbstractHttpConfigurer::disable);

        http.oauth2Login((config) -> {
            config.authorizationEndpoint(authorization -> authorization
                    .authorizationRequestRepository(httpCookieOAuth2AuthorizedClientRepository)
            );
            config.successHandler(oAuth2AuthenticationSuccessHandler);
            config.failureHandler(oAuth2AuthenticationFailureHandler);
            config.userInfoEndpoint((endpoint) -> endpoint.userService(oAuth2UserService));
        });

        // 기본 정책은 "인증 필요"(화이트리스트). 공개 엔드포인트만 명시적으로 permitAll 하고,
        // 목록에 없는(또는 신규로 추가되는) 엔드포인트는 자동으로 인증이 요구된다.
        http.authorizeHttpRequests((auth) ->
                auth
                        // CORS 프리플라이트는 항상 허용
                        .requestMatchers(OPTIONS, "/**").permitAll()

                        // Swagger / 에러 디스패치 / OAuth2 로그인 플로우 (공개)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // 서비스 간 내부 호출(내부망 전용, 게이트웨이 라우트에 미노출). 외부 비노출이라 permitAll.
                        .requestMatchers("/internal/**").permitAll()

                        // === 인증 필요 엔드포인트 (구체 규칙이 먼저 매칭되도록 아래 공개 규칙보다 위에 둔다) ===
                        .requestMatchers(POST, "/user/logout/all").authenticated()
                        .requestMatchers(PUT, "/user/password/update").authenticated()
                        .requestMatchers(GET, "/user/profile").authenticated()
                        .requestMatchers(PUT, "/user/profile").authenticated()
                        .requestMatchers(POST, "/user/profile").authenticated()

                        // === 공개(비로그인 허용) 엔드포인트 ===
                        .requestMatchers(POST, "/user/login", "/user/social/login", "/user/signup", "/user/email/verify", "/user/password/reset").permitAll()
                        // 로그아웃은 만료/무효 토큰 상태에서도 성공해야 하므로 permitAll. 상태변경이라 GET→POST (CSRF 방어).
                        .requestMatchers(POST, "/user/logout").permitAll()
                        .requestMatchers(PUT, "/user/password/reset").permitAll()
                        .requestMatchers(GET, "/user/token/refresh", "/user/email/duplicate", "/user/check", "/user/uuid/check").permitAll()

                        // === 그 외 전부 인증 필요 (화이트리스트 기본 정책) ===
                        .anyRequest().authenticated()
        );

        // 인증 실패(미인증/유효하지 않은 토큰으로 보호 엔드포인트 접근) 시 401(code 20001) 반환.
        // 공개 엔드포인트는 여기까지 오지 않으므로 영향 없다.
        http.exceptionHandling(handler ->
                handler.authenticationEntryPoint(jwtAuthenticationEntryPoint));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
