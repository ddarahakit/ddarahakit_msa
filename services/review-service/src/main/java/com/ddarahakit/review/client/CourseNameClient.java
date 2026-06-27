package com.ddarahakit.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * course-service 호출용 Feign 클라이언트(코스명 조회 + 수강권 확인).
 * Eureka 서비스명(course-service)으로 디스커버리 라우팅(lb://course-service)된다.
 * course-service 는 BaseResponse&lt;CourseRes&gt;(success/results 래퍼)를 반환하므로 동일 구조로
 * 역직렬화하고 필요한 필드(idx/name)만 매핑한다(나머지는 fail-on-unknown-properties=false 로 무시).
 */
@FeignClient(name = "course-service")
public interface CourseNameClient {

    @GetMapping("/course/{idx}")
    CourseResponse get(@PathVariable("idx") Long idx);

    /** 리뷰 작성 전 구매 검증: userIdx 가 courseIdx 를 수강(구매)했는지. */
    @GetMapping("/internal/enrollments/exists")
    boolean enrollmentExists(@RequestParam("userIdx") Long userIdx, @RequestParam("courseIdx") Long courseIdx);

    record CourseResponse(boolean success, CourseInfo results) {
    }

    record CourseInfo(Long idx, String name) {
    }
}
