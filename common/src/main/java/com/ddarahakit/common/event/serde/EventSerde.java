package com.ddarahakit.common.event.serde;

import com.ddarahakit.common.event.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** 이벤트 봉투 ↔ JSON 직렬화 헬퍼. Spring 의존 없이 동작. */
public final class EventSerde {

    private static final ObjectMapper M = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private EventSerde() {
    }

    public static String toJson(EventEnvelope envelope) {
        try {
            return M.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException("event serialize 실패: " + envelope.type(), e);
        }
    }

    public static EventEnvelope fromJson(String json) {
        try {
            return M.readValue(json, EventEnvelope.class);
        } catch (Exception e) {
            throw new IllegalStateException("event deserialize 실패", e);
        }
    }

    /** envelope.payload(JsonNode) → 구체 payload record 로 변환. */
    public static <T> T payload(EventEnvelope envelope, Class<T> type) {
        return M.convertValue(envelope.payload(), type);
    }

    /** 임의 payload 객체 → JsonNode (아웃박스 적재용). */
    public static JsonNode toNode(Object payload) {
        return M.valueToTree(payload);
    }
}
