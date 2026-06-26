package com.ddarahakit.course.domain.stats;

import com.ddarahakit.course.domain.course.model.Course;
import com.ddarahakit.course.domain.course.repository.CourseRepository;
import com.ddarahakit.course.domain.course.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 서비스 요약 통계(코어 도메인 자기 데이터 기반).
 * 가정:
 *  - studentCount = 수강권(enrollment)을 보유한 고유 사용자 수(distinct userIdx).
 *  - satisfactionRate = (4·5점 리뷰 수 / 전체 리뷰 수) * 100. course 의 rating1..5 버킷 합산(투영된 읽기모델) 기준.
 *    리뷰 0건이면 0. (리뷰 본문은 review-service 소유이나 평점 분포는 이벤트로 course 에 투영돼 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public StatsDto.SummaryRes summary() {
        long courseCount = courseRepository.count();
        long studentCount = enrollmentRepository.countDistinctStudents();

        List<Course> courses = courseRepository.findAll();
        long total = 0;
        long satisfied = 0;
        for (Course c : courses) {
            total += c.getRating1() + c.getRating2() + c.getRating3() + c.getRating4() + c.getRating5();
            satisfied += c.getRating4() + c.getRating5();
        }
        int satisfactionRate = total == 0 ? 0 : (int) Math.round(satisfied * 100.0 / total);

        return StatsDto.SummaryRes.builder()
                .courseCount(courseCount)
                .studentCount(studentCount)
                .satisfactionRate(satisfactionRate)
                .build();
    }
}
