package com.ddarahakit.identity.config.security;

import com.ddarahakit.identity.domain.user.model.entity.User;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.ddarahakit.identity.common.Constants.DEFAULT_USER_PROFILE_IMAGE_NAME;
import static com.ddarahakit.identity.common.Constants.DEFAULT_USER_ROLE;

@Getter
@Builder
public class AuthUserDetails implements UserDetails, OAuth2User {
    private Long idx;
    private String email;
    private String password;
    private String name;
    private String profileImageUrl;
    private String introduction;
    private String phoneNumber;
    private String role;
    private Boolean enabled;
    private String provider;
    private String providerId;
    private final Map<String, Object> attributes;

    public User toEntity() {
        return User.builder()
                .idx(idx)
                .email(email)
                .name(name)
                .password(password)
                .profileImageUrl(profileImageUrl)
                .role(role)
                .enabled(enabled != null ? enabled : false)
                .provider(provider)
                .providerId(providerId)
                .build();
    }

    public static AuthUserDetails of(User user) {

        return AuthUserDetails.builder()
                .idx(user.getIdx())
                .email(user.getEmail())
                .name(user.getName())
                .password(user.getPassword())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .build();
    }

    public static AuthUserDetails of(String email, String name, String provider, String providerId) {
        return AuthUserDetails.builder()
                .email(email)
                .name(name)
                .password(provider)
                .profileImageUrl(DEFAULT_USER_PROFILE_IMAGE_NAME)
                .role(DEFAULT_USER_ROLE)
                .enabled(true)
                .provider(provider)
                .providerId(providerId)
                .build();
    }

    public Long getIdx() {
        return idx;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }


    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getName() {
        return name;
    }
}
