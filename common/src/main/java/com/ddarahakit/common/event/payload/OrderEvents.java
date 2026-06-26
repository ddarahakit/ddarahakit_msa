package com.ddarahakit.common.event.payload;

import java.time.Instant;
import java.util.List;

/** commerce → {@code commerce.order.v1} (key = courseId). */
public final class OrderEvents {

    public record OrderPaid(long orderId, long userId, List<Long> courseIds, Instant paidAt) {
    }

    public record OrderRefunded(long orderId, long userId, List<Long> courseIds) {
    }

    private OrderEvents() {
    }
}
