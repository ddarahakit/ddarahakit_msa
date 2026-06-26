package com.ddarahakit.identity.config.security.oauth;

import com.ddarahakit.identity.config.security.AuthUserDetails;
import com.ddarahakit.identity.config.security.token.TokenService;
import com.ddarahakit.identity.config.security.token.TokenService.TokenPair;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenService tokenService;

    @Value("${app.domain.server}")
    private String domain;

    @Value("${app.token.access-format}")
    private String accessFormat;

    @Value("${app.token.refresh-format}")
    private String refreshFormat;

    @Value("${app.token.access-max-age}")
    private int accessMaxAge;

    @Value("${app.token.refresh-max-age}")
    private int refreshMaxAge;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        log.info("[OAuth2] 로그인 성공");

        AuthUserDetails authUserDetails = (AuthUserDetails) authentication.getPrincipal();

        TokenPair tokenPair = tokenService.createTokenPair(authUserDetails, request);

        // Set-Cookie 헤더 방식으로 SameSite 속성 포함 (Jakarta Cookie API는 SameSite 미지원)
        response.addHeader("Set-Cookie", String.format(accessFormat, tokenPair.accessToken(), accessMaxAge / 1000));
        response.addHeader("Set-Cookie", String.format(refreshFormat, tokenPair.refreshToken(), refreshMaxAge / 1000));

        String redirectUrl = domain + "/user/social/" + authUserDetails.getProvider();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
