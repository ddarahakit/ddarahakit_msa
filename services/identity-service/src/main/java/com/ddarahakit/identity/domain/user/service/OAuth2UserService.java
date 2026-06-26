package com.ddarahakit.identity.domain.user.service;

import com.ddarahakit.identity.config.security.AuthUserDetails;
import com.ddarahakit.identity.domain.user.model.entity.User;
import com.ddarahakit.identity.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

import static com.ddarahakit.identity.common.Constants.DEFAULT_USER_PROFILE_IMAGE_NAME;
import static com.ddarahakit.identity.common.Constants.DEFAULT_USER_ROLE;

@RequiredArgsConstructor
@Service
public class OAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 1. 어떤 소셜 로그인인지 구분
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google" or "kakao"

        // 2. 사용자 정보 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = null;
        String name = null;
        String provider = null;
        String providerId = null;

        if ("google".equals(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            provider = "google";
            providerId = (String) attributes.get("sub");
        } else if ("kakao".equals(registrationId)) {
            Map properties = (Map) attributes.get("properties");
            providerId = ((Long) attributes.get("id")).toString();
            name = (String) properties.get("nickname");
            // 카카오는 기본 scope에 이메일이 없으므로 pseudo-email 생성 (이메일 형식 유지)
            email = providerId + "@kakao.local";
            provider = "kakao";
        }


        Optional<User> socialUserResult = userRepository.findByProviderIdAndProvider(providerId, provider);
        AuthUserDetails authUserDetails;
        if (!socialUserResult.isPresent()) {
            User user = userRepository.save(
                    User.builder()
                            .email(email)
                            .name(name)
                            .password(provider)
                            .profileImageUrl(DEFAULT_USER_PROFILE_IMAGE_NAME)
                            .role(DEFAULT_USER_ROLE)
                            .enabled(true)   // OAuth 사용자는 소셜 플랫폼에서 이미 인증됨 → 즉시 활성화
                            .provider(provider)
                            .providerId(providerId)
                            .build()
            );
            authUserDetails = AuthUserDetails.of(user);
        } else {
            authUserDetails = AuthUserDetails.of(socialUserResult.get());
        }


        return authUserDetails;
    }
}
