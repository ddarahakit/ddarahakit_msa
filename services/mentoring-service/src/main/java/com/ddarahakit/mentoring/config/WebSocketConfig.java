package com.ddarahakit.mentoring.config;

import com.ddarahakit.mentoring.domain.mentoring.ws.MentoringHandshakeInterceptor;
import com.ddarahakit.mentoring.domain.mentoring.ws.MentoringSignalHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 멘토링 실시간 채널(raw WebSocket) 설정.
 *
 * 프론트(useMentoringSocket)는 STOMP 가 아니라 순수 WebSocket 으로 type 필드를 가진 JSON 을
 * 주고받는다(chat-message / WebRTC 시그널링 offer·answer·ice-candidate·share-stopped).
 * 따라서 서버도 STOMP 브로커가 아니라 sessionId 룸 단위 릴레이 핸들러로 구현한다.
 *
 * 엔드포인트는 /mentoring/ws/signal 로 두어 게이트웨이의 기존 /mentoring/** 라우트가
 * 그대로 인증(ATOKEN→X-User-Id)·프록시(WebSocket 업그레이드)하도록 한다.
 * (컨트롤러의 /mentoring/{sessionIdx} 등 단일·이중 세그먼트 매핑과 충돌하지 않도록 3세그먼트 경로 사용)
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final MentoringSignalHandler signalHandler;

    // 브라우저 Origin 검증(서버는 게이트웨이 뒤에 있으나 Origin 은 브라우저 출처가 그대로 전달됨).
    @Value("${app.ws.allowed-origins:http://localhost:8081}")
    private String[] allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalHandler, "/mentoring/ws/signal")
                .addInterceptors(new MentoringHandshakeInterceptor())
                .setAllowedOrigins(allowedOrigins);
    }
}
