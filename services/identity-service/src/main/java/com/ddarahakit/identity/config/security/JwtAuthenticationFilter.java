package com.ddarahakit.identity.config.security;

import com.ddarahakit.identity.domain.user.model.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN_COOKIE = "ATOKEN";

    private final JwtProvider jwtProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/user/login") ||
                path.startsWith("/user/signup") ||
                path.startsWith("/user/token/refresh") ||
                // 정확히 '/user/logout' 만 스킵. '/user/logout/all' 은 인증 필요(authenticated)이므로
                // 필터를 거쳐 인증 컨텍스트가 설정돼야 한다(startsWith 면 logout/all 까지 스킵돼 항상 401).
                path.equals("/user/logout") ||
                path.startsWith("/oauth2/authorization/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = getTokenFromCookie(request);

        // 유효한 액세스 토큰일 때만 인증 정보를 설정한다.
        // 토큰이 없거나 만료/위조/타입오류인 경우에도 요청을 막지 않고 익명으로 진행한다.
        //  - 공개(permitAll) 엔드포인트: 잘못된 토큰이 있어도 정상 응답 (예: /course/list)
        //  - 보호(authenticated) 엔드포인트: 인증 정보가 없으므로 JwtAuthenticationEntryPoint 가 401(code 20001) 반환
        // 즉, 접근 거부 판단은 필터가 아니라 Spring Security 인가 규칙 + EntryPoint 에 위임한다.
        if (token != null && jwtProvider.validateToken(token) && jwtProvider.isAccessToken(token)) {
            try {
                setAuthentication(token);
            } catch (Exception e) {
                // 인증 정보 설정 실패 시에도 컨텍스트는 비운 채 진행(보호 엔드포인트는 EntryPoint 가 처리)
                log.error("[TOKEN] 인증 정보 설정 실패: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(String token) {
        User user = User.builder()
                .idx(jwtProvider.extractIdx(token))
                .email(jwtProvider.extractEmail(token))
                .role(jwtProvider.extractRole(token))
                .build();

        AuthUserDetails authUserDetails = AuthUserDetails.of(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authUserDetails,
                null,
                authUserDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
