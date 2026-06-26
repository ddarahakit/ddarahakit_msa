package com.ddarahakit.course.domain.course.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * CourseLevel(enum, 영문 코드) ↔ DB varchar(한글 라벨 초급/중급/고급) 변환기.
 */
@Converter
public class CourseLevelConverter implements AttributeConverter<CourseLevel, String> {

    @Override
    public String convertToDatabaseColumn(CourseLevel attribute) {
        return attribute == null ? null : attribute.getDescription();
    }

    @Override
    public CourseLevel convertToEntityAttribute(String dbData) {
        return CourseLevel.fromDbLabel(dbData);
    }
}
