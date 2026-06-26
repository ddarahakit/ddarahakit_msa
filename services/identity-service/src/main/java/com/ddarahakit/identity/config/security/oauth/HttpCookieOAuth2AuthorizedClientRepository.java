package com.ddarahakit.identity.config.security.oauth;

import com.ddarahakit.identity.utils.Aes256;
import com.ddarahakit.identity.utils.CookieUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

import static java.util.Objects.isNull;

@Slf4j
@Component
public class HttpCookieOAuth2AuthorizedClientRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_COOKIE_NAME = "OAUTH2_AUTHORIZATION_REQUEST";
    public static final Duration OAUTH_COOKIE_EXPIRY = Duration.ofMinutes(5);

    // Java 네이티브 직렬화(역직렬화 가젯 공격에 취약) 대신 Jackson JSON 사용.
    // SecurityJackson2Modules 가 OAuth2AuthorizationRequest 등의 허용 타입/mixin 을 등록해 안전하게 (역)직렬화한다.
    private static final ObjectMapper OBJECT_MAPPER = buildObjectMapper();

    private static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(
                HttpCookieOAuth2AuthorizedClientRepository.class.getClassLoader()));
        return mapper;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
                                         HttpServletResponse response) {
        if (isNull(authorizationRequest)) {
            removeAuthorizationRequest(request, response);
            return;
        }

        CookieUtil.addCookie(response, OAUTH2_COOKIE_NAME, encrypt(authorizationRequest), OAUTH_COOKIE_EXPIRY);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest oAuth2AuthorizationRequest = getCookie(request);
        CookieUtil.removeCookie(request, response, OAUTH2_COOKIE_NAME);
        return oAuth2AuthorizationRequest;
    }

    private OAuth2AuthorizationRequest getCookie(HttpServletRequest request) {
        return CookieUtil.getCookie(request, OAUTH2_COOKIE_NAME).map(this::decrypt).orElse(null);
    }

    private String encrypt(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            byte[] json = OBJECT_MAPPER.writeValueAsBytes(authorizationRequest);
            return Aes256.encrypt(json);
        } catch (IOException e) {
            throw new RuntimeException("OAuth2 인가요청 직렬화 실패", e);
        }
    }

    private OAuth2AuthorizationRequest decrypt(Cookie cookie) {
        try {
            byte[] json = Aes256.decrypt(cookie.getValue());
            return OBJECT_MAPPER.readValue(json, OAuth2AuthorizationRequest.class);
        } catch (Exception e) {
            // 손상·위조·구버전(Java 직렬화) 쿠키는 인가요청 없음(null)으로 처리해 로그인 흐름이 깨지지 않게 한다.
            log.warn("OAuth2 인가요청 쿠키 역직렬화 실패 — 무시", e);
            return null;
        }
    }

}

