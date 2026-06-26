package com.ddarahakit.mentoring.domain.mentoring.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 핸드셰이크 시 게이트웨이가 주입한 X-User-Id 를 신뢰해 WS 세션에 사용자 식별자를 심는다.
 * 헤더가 없거나(=비로그인) 잘못된 경우 핸드셰이크를 거부(401)하여 인증된 사용자만 실시간 채널에 연결되게 한다.
 * (게이트웨이가 WS 프록시 특성상 클라이언트에 101 을 선응답할 수 있으나, 여기서 거부하면 다운스트림 연결은 성립하지 않는다.)
 */
public class MentoringHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MentoringHandshakeInterceptor.class);

    public static final String ATTR_USER_ID = "userId";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String userId = servletRequest.getServletRequest().getHeader(USER_ID_HEADER);
            if (StringUtils.hasText(userId)) {
                try {
                    attributes.put(ATTR_USER_ID, Long.parseLong(userId.trim()));
                    return true;
                } catch (NumberFormatException ignored) {
                    // 잘못된 헤더 → 거부
                }
            }
        }
        log.warn("멘토링 WS 핸드셰이크 거부: 인증 헤더(X-User-Id) 없음 — 미인증 연결");
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                              ServerHttpResponse response,
                              WebSocketHandler wsHandler,
                              Exception exception) {
        // no-op
    }
}
