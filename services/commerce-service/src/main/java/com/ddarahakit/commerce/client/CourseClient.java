package com.ddarahakit.commerce.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * course-service 가 아직 없으므로 모놀리스(/course/{courseIdx})를 직접 URL 로 호출해 가격을 검증한다.
 * 모놀리스는 BaseResponse&lt;CourseRes&gt;(success/results 래퍼)를 반환하므로 동일 구조로 역직렬화하고
 * 필요한 필드만 매핑한다(나머지는 application.yml 의 fail-on-unknown-properties=false 로 무시).
 */
@FeignClient(name = "course-pricing", url = "${monolith.url}")
public interface CourseClient {

    @GetMapping("/course/{courseIdx}")
    CourseResponse get(@PathVariable Long courseIdx);

    record CourseResponse(boolean success, CourseInfo results) {
    }

    record CourseInfo(Long idx, String name, String image, int salePrice, int originalPrice) {
    }
}
