package com.ddarahakit.identity.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import static io.jsonwebtoken.lang.Strings.EMPTY;
import static org.hibernate.type.descriptor.java.IntegerJavaType.ZERO;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CookieUtil {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String cookieName) {
        return Optional.ofNullable(request.getCookies())
                .flatMap(cookies -> Arrays.stream(cookies)
                        .filter(cookie -> cookie.getName().equals(cookieName))
                        .findFirst());
    }

    public static void addCookie(HttpServletResponse response, String cookieName, String cookieValue,
                                 Duration maxAge) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setPath("/");
        cookie.setHttpOnly(Boolean.TRUE);
        cookie.setSecure(Boolean.TRUE);
        cookie.setMaxAge((int)maxAge.toSeconds());

        response.addCookie(cookie);
    }

    public static void removeCookie(HttpServletRequest request, HttpServletResponse response, String cookieName) {
        // request.getCookies() 는 쿠키가 하나도 없으면 null 을 반환한다.
        // Optional.of(null) 은 NPE 를 던지므로 반드시 ofNullable 을 써야 한다.
        // (OAuth2 콜백 시 OAUTH2_AUTHORIZATION_REQUEST 쿠키가 없으면 여기서 500 이 났음)
        Optional.ofNullable(request.getCookies())
                .ifPresent(cookies ->
                        Arrays.stream(cookies)
                                .filter(cookie -> cookie.getName().equals(cookieName))
                                .forEach(cookie -> {
                                    cookie.setValue(EMPTY);
                                    cookie.setPath("/");
                                    cookie.setMaxAge(ZERO);
                                    response.addCookie(cookie);
                                })
                );
    }
}