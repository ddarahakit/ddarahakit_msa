package com.ddarahakit.mentoring.domain.mentoring.ws;

import com.ddarahakit.mentoring.domain.mentoring.MentoringSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 멘토링 세션 단위 실시간 릴레이 핸들러.
 *
 * 프로토콜(프론트 useMentoringSocket 와 동일): {@code { type, sessionId, ... }} JSON.
 *  - join-session : 발신자가 해당 세션의 참가자(멘토/멘티)인지 검증 후 룸에 입장.
 *  - 그 외(chat-message / offer / answer / ice-candidate / share-stopped)
 *    : 같은 룸의 다른 참가자에게 그대로 전달(fan-out). 발신자에게는 에코하지 않는다.
 *
 * 채팅 영속화는 REST(/mentoring/{id}/messages)가 담당하고, 이 채널은 실시간 전달만 책임진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MentoringSignalHandler extends TextWebSocketHandler {

    private final MentoringSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    private static final String ATTR_ROOM_ID = "roomId";

    // sessionId(String) → 현재 입장 중인 소켓들
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        JsonNode node;
        try {
            node = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            return; // 잘못된 JSON 무시
        }

        String type = text(node, "type");
        String roomId = text(node, "sessionId");
        if (roomId == null) return;

        if ("join-session".equals(type)) {
            handleJoin(session, roomId);
            return;
        }

        relay(session, roomId, message);
    }

    /** 참가자 검증 후 룸 입장. 참가자가 아니면 입장시키지 않는다. */
    private void handleJoin(WebSocketSession session, String roomId) {
        Long userId = (Long) session.getAttributes().get(MentoringHandshakeInterceptor.ATTR_USER_ID);
        if (!isParticipant(roomId, userId)) {
            log.debug("멘토링 WS 입장 거부 sessionId={} userId={}", roomId, userId);
            return;
        }
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        session.getAttributes().put(ATTR_ROOM_ID, roomId);
    }

    /** 같은 룸의 다른 참가자에게 원본 메시지를 그대로 전달. 발신자는 룸 입장(join) 상태여야 한다. */
    private void relay(WebSocketSession session, String roomId, TextMessage message) {
        Set<WebSocketSession> peers = rooms.get(roomId);
        if (peers == null || !peers.contains(session)) return;

        for (WebSocketSession peer : peers) {
            if (peer.getId().equals(session.getId()) || !peer.isOpen()) continue;
            try {
                synchronized (peer) {
                    peer.sendMessage(message);
                }
            } catch (IOException e) {
                log.debug("멘토링 WS 릴레이 실패 sessionId={} peer={}", roomId, peer.getId(), e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomId = (String) session.getAttributes().get(ATTR_ROOM_ID);
        if (roomId == null) return;
        rooms.computeIfPresent(roomId, (k, peers) -> {
            peers.remove(session);
            return peers.isEmpty() ? null : peers;
        });
    }

    private boolean isParticipant(String roomId, Long userId) {
        if (userId == null) return false;
        try {
            Long id = Long.parseLong(roomId);
            return sessionRepository.findById(id)
                    .map(s -> userId.equals(s.getMentorIdx()) || userId.equals(s.getMenteeIdx()))
                    .orElse(false);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
