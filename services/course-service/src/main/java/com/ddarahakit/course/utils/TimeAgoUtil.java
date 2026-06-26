package com.ddarahakit.course.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class TimeAgoUtil {
    public static String timeAgo(Date date) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        long years = ChronoUnit.YEARS.between(dateTime, now);
        if (years > 0) {
            return years + "년 전";
        }

        long months = ChronoUnit.MONTHS.between(dateTime, now);
        if (months > 0) {
            return months + "달 전";
        }

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days > 0) {
            return days + "일 전";
        }

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours > 0) {
            return hours + "시간 전";
        }

        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        if (minutes > 0) {
            return minutes + "분 전";
        }

        return "방금 전";
    }
}
