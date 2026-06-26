package com.ddarahakit.course.domain.study;

import com.ddarahakit.course.config.security.AuthUserDetails;
import com.ddarahakit.course.domain.course.repository.LectureCompleteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 주간 학습활동 집계 (lecture_complete.completed_at 기반).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyService {

    private static final int WEEKLY_GOAL = 5;
    private static final int WEEK_DAYS = 7;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final LectureCompleteRepository lectureCompleteRepository;

    public StudyDto.WeeklyRes weekly(AuthUserDetails authUserDetails) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(WEEK_DAYS - 1L).atStartOfDay();

        List<LocalDateTime> completedAts = lectureCompleteRepository
                .findCompletedAtByUserSince(authUserDetails.getIdx(), from);

        Map<LocalDate, Long> countByDay = new TreeMap<>();
        for (int i = WEEK_DAYS - 1; i >= 0; i--) {
            countByDay.put(today.minusDays(i), 0L);
        }
        long weeklyCompleted = 0;
        Set<LocalDate> studiedDays = new HashSet<>();
        for (LocalDateTime at : completedAts) {
            LocalDate d = at.toLocalDate();
            if (countByDay.containsKey(d)) {
                countByDay.merge(d, 1L, Long::sum);
                weeklyCompleted++;
            }
            studiedDays.add(d);
        }

        List<StudyDto.DayCount> days = new ArrayList<>();
        countByDay.forEach((d, c) -> days.add(StudyDto.DayCount.builder()
                .date(d.format(DATE_FMT)).count(c).build()));

        int streak = 0;
        LocalDate cursor = studiedDays.contains(today) ? today : today.minusDays(1);
        while (studiedDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        int goalRate = (int) Math.min(100, Math.round(weeklyCompleted * 100.0 / WEEKLY_GOAL));

        return StudyDto.WeeklyRes.builder()
                .days(days)
                .streakDays(streak)
                .weeklyCompleted(weeklyCompleted)
                .goalRate(goalRate)
                .weeklyGoal(WEEKLY_GOAL)
                .build();
    }
}
