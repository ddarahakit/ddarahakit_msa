package com.ddarahakit.common.event;

/** 도메인 이벤트 타입 상수(envelope.type). */
public final class EventType {
    public static final String ORDER_PAID            = "OrderPaid";
    public static final String ORDER_REFUNDED        = "OrderRefunded";
    public static final String REVIEW_CREATED        = "ReviewCreated";
    public static final String REVIEW_UPDATED        = "ReviewUpdated";
    public static final String REVIEW_DELETED        = "ReviewDeleted";
    public static final String USER_REGISTERED       = "UserRegistered";
    public static final String USER_PROFILE_CHANGED  = "UserProfileChanged";
    public static final String USER_DELETED          = "UserDeleted";

    private EventType() {
    }
}
