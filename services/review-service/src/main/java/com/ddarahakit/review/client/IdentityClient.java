package com.ddarahakit.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * identity-service 내부 엔드포인트(/internal/users)를 동기 호출해 작성자 표시명 스냅샷을 조회한다.
 * Eureka 의 서비스명(identity-service)으로 디스커버리 라우팅된다.
 */
@FeignClient(name = "identity-service")
public interface IdentityClient {

    @GetMapping("/internal/users/{idx}")
    UserSummary getUser(@PathVariable("idx") Long idx);

    record UserSummary(Long idx, String name, String profileImageUrl) {
    }
}
