package com.ddarahakit.identity.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증이 필요한 엔드포인트에 인증 없이(또는 유효하지 않은 토큰으로) 접근했을 때 401 을 반환한다.
 *
 * JwtAuthenticationFilter 는 토큰이 잘못돼도 요청을 막지 않고 익명으로 통과시키므로,
 * 보호된 엔드포인트의 거부 처리는 이 EntryPoint 가 담당한다.
 * code 20001 을 유지해 프론트엔드의 토큰 갱신(리프레시) 흐름이 그대로 동작하게 한다.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"code\":20001,\"message\":\"유효하지 않은 토큰입니다.\",\"results\":null}"
        );
    }
}
