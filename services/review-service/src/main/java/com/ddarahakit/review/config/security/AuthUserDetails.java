package com.ddarahakit.review.config.security;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 게이트웨이가 주입한 헤더(X-User-Id/X-User-Role) 기반 인증 주체.
 * MSA 에서 review-service 는 User 엔티티를 갖지 않으므로 idx/role 만 보유한다.
 */
@Getter
@Builder
public class AuthUserDetails implements UserDetails {
    private Long idx;
    private String role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return idx != null ? String.valueOf(idx) : null;
    }
}
