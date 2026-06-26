package com.ddarahakit.common.event.payload;

/** identity → {@code identity.user.v1} (key = userId). */
public final class UserEvents {

    public record UserRegistered(long userId, String email, String name) {
    }

    public record UserProfileChanged(long userId, String name, String profileImageUrl) {
    }

    public record UserDeleted(long userId) {
    }

    private UserEvents() {
    }
}
