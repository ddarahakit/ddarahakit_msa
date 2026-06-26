package com.ddarahakit.community.utils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 게시글 태그 정규화 유틸.
 * - 앞뒤 공백 제거, 선행 '#' 제거
 * - 빈 값 제거, 30자 초과 시 잘라냄
 * - 입력 순서를 유지하며 중복 제거(대소문자 무시 비교)
 */
public final class TagUtils {

    private static final int MAX_TAG_LENGTH = 30;
    private static final int MAX_TAG_COUNT = 5;

    private TagUtils() {
    }

    public static Set<String> normalize(List<String> rawTags) {
        Set<String> result = new LinkedHashSet<>();
        if (rawTags == null) {
            return result;
        }

        Set<String> seenLower = new LinkedHashSet<>();
        for (String raw : rawTags) {
            if (raw == null) {
                continue;
            }
            String tag = raw.trim();
            while (tag.startsWith("#")) {
                tag = tag.substring(1).trim();
            }
            if (tag.isEmpty()) {
                continue;
            }
            if (tag.length() > MAX_TAG_LENGTH) {
                tag = tag.substring(0, MAX_TAG_LENGTH);
            }
            String lower = tag.toLowerCase();
            if (seenLower.add(lower)) {
                result.add(tag);
            }
            if (result.size() >= MAX_TAG_COUNT) {
                break;
            }
        }
        return result;
    }
}
