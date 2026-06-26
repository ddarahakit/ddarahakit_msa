package com.ddarahakit.common.event.payload;

/** review → {@code review.review.v1} (key = courseId). */
public final class ReviewEvents {

    public record ReviewCreated(long reviewId, long courseId, long userId, int rating) {
    }

    public record ReviewUpdated(long reviewId, long courseId, int oldRating, int newRating) {
    }

    public record ReviewDeleted(long reviewId, long courseId, int rating) {
    }

    private ReviewEvents() {
    }
}
