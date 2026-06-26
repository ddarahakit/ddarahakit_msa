package com.ddarahakit.course.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 게이트웨이가 JWT 검증 후 주입한 헤더(X-User-Id/X-User-Role)를 신뢰해 SecurityContext 를 구성한다.
 * JWT 파싱/검증은 게이트웨이가 담당하므로 여기서는 수행하지 않는다.
 * 헤더가 없으면 익명 상태로 통과한다.
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(USER_ID_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        if (StringUtils.hasText(userId) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long idx = Long.parseLong(userId.trim());
                String resolvedRole = StringUtils.hasText(role) ? role.trim() : "ROLE_USER";

                AuthUserDetails principal = AuthUserDetails.builder()
                        .idx(idx)
                        .role(resolvedRole)
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (NumberFormatException e) {
                // 잘못된 X-User-Id 헤더는 무시하고 익명으로 진행
            }
        }

        filterChain.doFilter(request, response);
    }
}
