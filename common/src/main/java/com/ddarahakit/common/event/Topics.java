package com.ddarahakit.common.event;

/** Kafka 토픽명 상수. {@code <domain>.<aggregate>.v<major>} */
public final class Topics {
    public static final String COMMERCE_ORDER = "commerce.order.v1";
    public static final String REVIEW_REVIEW  = "review.review.v1";
    public static final String IDENTITY_USER  = "identity.user.v1";

    private Topics() {
    }
}
