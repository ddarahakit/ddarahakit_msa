package com.ddarahakit.common.event;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * 모든 도메인 이벤트의 공통 봉투.
 * - eventId  : 멱등 키(소비자 중복 차단)
 * - type     : {@link EventType} 상수
 * - version  : 계약 버전(하위호환은 필드 추가만)
 * - traceId  : 분산추적 전파
 * - payload  : 이벤트별 본문(JsonNode → 소비 측에서 record 로 역직렬화)
 */
public record EventEnvelope(
        String eventId,
        String type,
        int version,
        Instant occurredAt,
        String traceId,
        JsonNode payload
) {
    public static EventEnvelope of(String eventId, String type, int version,
                                   Instant occurredAt, String traceId, JsonNode payload) {
        return new EventEnvelope(eventId, type, version, occurredAt, traceId, payload);
    }
}
