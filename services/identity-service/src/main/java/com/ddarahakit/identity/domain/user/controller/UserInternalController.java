package com.ddarahakit.identity.domain.user.controller;

import com.ddarahakit.identity.common.exception.BaseException;
import com.ddarahakit.identity.common.model.BaseResponseStatus;
import com.ddarahakit.identity.domain.user.model.entity.User;
import com.ddarahakit.identity.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 간(내부망 전용) 사용자 조회 엔드포인트.
 * 게이트웨이 라우트에 노출되지 않으며, community-service 등이 Feign 으로 작성자 표시명 스냅샷을 조회할 때 사용한다.
 */
@Hidden
@RequiredArgsConstructor
@RestController
@RequestMapping("/internal/users")
public class UserInternalController {

    private final UserRepository userRepository;

    @GetMapping("/{idx}")
    public UserSummary getUser(@PathVariable Long idx) {
        User user = userRepository.findById(idx)
                .orElseThrow(() -> BaseException.of(BaseResponseStatus.USER_NOT_FOUND));
        return new UserSummary(user.getIdx(), user.getName(), user.getProfileImageUrl());
    }

    public record UserSummary(Long idx, String name, String profileImageUrl) {
    }
}
