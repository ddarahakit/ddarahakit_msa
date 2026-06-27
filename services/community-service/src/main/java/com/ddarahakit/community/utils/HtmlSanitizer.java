package com.ddarahakit.community.utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * 저장형 XSS 방지: 게시글/댓글의 HTML 본문(content)에서 위험한 태그·속성·스크립트를 제거한다.
 * 기본 서식(굵게/링크/이미지/표/목록 등)은 허용하되 {@code <script>}, {@code on*} 핸들러,
 * {@code javascript:} URL 등은 제거한다. content 는 프론트에서 v-html 로 렌더되므로 저장 시점에 정제한다.
 * (text 미리보기 필드는 {@code {{ }}} 로 자동 이스케이프되어 XSS 벡터가 아니므로 정제 대상이 아니다.)
 */
public final class HtmlSanitizer {

    private static final Safelist POLICY = Safelist.relaxed()
            .addAttributes("a", "target", "rel");

    private HtmlSanitizer() {
    }

    /** HTML 본문을 안전한 서식만 남기고 정제. null 은 그대로 통과. */
    public static String clean(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, POLICY);
    }
}
