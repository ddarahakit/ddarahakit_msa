package com.ddarahakit.community.domain.community.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostType {
    QUESTION("질문"),
    FREE("자유주제"),
    NOTICE("공지사항");

    private final String description;
}
