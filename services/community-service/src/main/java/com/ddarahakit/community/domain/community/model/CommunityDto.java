package com.ddarahakit.community.domain.community.model;

import com.ddarahakit.community.utils.HtmlSanitizer;
import com.ddarahakit.community.utils.TagUtils;
import com.ddarahakit.community.utils.TimeAgoUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CommunityDto {

    private CommunityDto() {
    }

    // ================================
    // Request DTOs
    // ================================

    @Getter
    @Schema(description = "게시글 작성 요청")
    public static class PostCreateRequest {

        @Schema(description = "게시글 타입", example = "QUESTION", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "게시글 타입을 선택해주세요.")
        private PostType postType;

        @Schema(description = "게시글 제목", example = "스프링 관련 질문있어요.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "게시글 제목을 입력해주세요.")
        @Size(min = 1, max = 100, message = "게시글 제목은 1자 이상 100자 이하로 입력해주세요.")
        private String title;

        @Schema(description = "게시글 요약 (미리보기용)", example = "스프링 부트 설정 관련...")
        @Size(max = 500, message = "게시글 요약은 500자 이하로 입력해주세요.")
        private String text;

        @Schema(description = "게시글 본문 내용 (HTML)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "게시글 내용을 입력해주세요.")
        private String content;

        @Schema(description = "관련 코스 ID (질문 타입일 때 선택사항)", example = "1")
        private Long courseIdx;

        @Schema(description = "관련 강의 ID (질문 타입일 때 선택사항)", example = "1")
        private Long lectureIdx;

        @Schema(description = "태그 목록 (최대 5개)", example = "[\"스프링\", \"JPA\"]")
        @Size(max = 5, message = "태그는 최대 5개까지 등록할 수 있습니다.")
        private List<@Size(max = 30, message = "태그는 30자 이하로 입력해주세요.") String> tags;

        /**
         * 작성자 표시명 스냅샷(authorName/authorProfileImageUrl)은 서비스에서 identity-service 를
         * Feign 으로 조회해 채운다. courseName/lectureName 스냅샷은 신규 작성 시 null (추후 이벤트 투영).
         */
        public Post toEntity(Long userIdx, String authorName, String authorProfileImageUrl) {
            return Post.builder()
                    .postType(this.postType)
                    .title(this.title)
                    .text(this.text)
                    .content(HtmlSanitizer.clean(this.content))
                    .userIdx(userIdx)
                    .authorName(authorName)
                    .authorProfileImageUrl(authorProfileImageUrl)
                    .courseIdx(this.courseIdx)
                    .lectureIdx(this.lectureIdx)
                    .tags(TagUtils.normalize(this.tags))
                    .build();
        }
    }

    @Getter
    @Schema(description = "게시글 수정 요청")
    public static class PostUpdateRequest {

        @Schema(description = "게시글 타입", example = "QUESTION", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "게시글 타입을 선택해주세요.")
        private PostType postType;

        @Schema(description = "게시글 제목", example = "스프링 관련 질문있어요.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "게시글 제목을 입력해주세요.")
        @Size(min = 1, max = 100, message = "게시글 제목은 1자 이상 100자 이하로 입력해주세요.")
        private String title;

        @Schema(description = "게시글 요약 (미리보기용)", example = "스프링 부트 설정 관련...")
        @Size(max = 500, message = "게시글 요약은 500자 이하로 입력해주세요.")
        private String text;

        @Schema(description = "게시글 본문 내용 (HTML)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "게시글 내용을 입력해주세요.")
        private String content;

        @Schema(description = "관련 코스 ID (질문 타입일 때 선택사항)", example = "1")
        private Long courseIdx;

        @Schema(description = "관련 강의 ID (질문 타입일 때 선택사항)", example = "1")
        private Long lectureIdx;

        @Schema(description = "태그 목록 (최대 5개)", example = "[\"스프링\", \"JPA\"]")
        @Size(max = 5, message = "태그는 최대 5개까지 등록할 수 있습니다.")
        private List<@Size(max = 30, message = "태그는 30자 이하로 입력해주세요.") String> tags;
    }

    @Getter
    @Schema(description = "댓글 수정 요청")
    public static class CommentUpdateRequest {

        @Schema(description = "댓글 내용 (텍스트)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        @Size(max = 2000, message = "댓글은 2000자 이하로 입력해주세요.")
        private String text;

        @Schema(description = "댓글 내용 (HTML)")
        private String content;
    }

    @Getter
    @Schema(description = "댓글 작성 요청")
    public static class CommentCreateRequest {

        @Schema(description = "댓글 내용 (텍스트)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        @Size(max = 2000, message = "댓글은 2000자 이하로 입력해주세요.")
        private String text;

        @Schema(description = "댓글 내용 (HTML)")
        private String content;

        @Schema(description = "게시글 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "게시글을 선택해주세요.")
        private Long postIdx;

        public Comment toEntity(Long userIdx, String authorName, String authorProfileImageUrl, Post post) {
            return Comment.builder()
                    .text(this.text)
                    .content(HtmlSanitizer.clean(this.content))
                    .userIdx(userIdx)
                    .authorName(authorName)
                    .authorProfileImageUrl(authorProfileImageUrl)
                    .post(post)
                    .build();
        }
    }

    // ================================
    // Response DTOs
    // ================================

    @Getter
    @Builder
    @Schema(description = "게시글 목록 페이징 응답")
    public static class PostPageResponse {
        @Schema(description = "현재 페이지 번호 (0부터 시작)")
        private final int page;

        @Schema(description = "페이지 크기")
        private final int size;

        @Schema(description = "다음 페이지 존재 여부")
        private final boolean hasNext;

        @Schema(description = "이전 페이지 존재 여부")
        private final boolean hasPrev;

        @Schema(description = "전체 페이지 수")
        private final int totalPages;

        @Schema(description = "전체 게시글 수")
        private final long totalPosts;

        @Schema(description = "게시글 목록")
        private final List<PostSummaryResponse> posts;

        public static PostPageResponse from(Page<Post> postPage) {
            List<PostSummaryResponse> posts = postPage.getContent().stream()
                    .map(PostSummaryResponse::from)
                    .toList();

            return PostPageResponse.builder()
                    .page(postPage.getNumber())
                    .size(postPage.getSize())
                    .hasNext(postPage.hasNext())
                    .hasPrev(postPage.hasPrevious())
                    .totalPages(postPage.getTotalPages())
                    .totalPosts(postPage.getTotalElements())
                    .posts(posts)
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "게시글 목록용 요약 응답 (댓글 미포함)")
    public static class PostSummaryResponse {
        @Schema(description = "게시글 ID")
        private final Long idx;

        @Schema(description = "게시글 타입")
        private final PostType postType;

        @Schema(description = "게시글 타입 설명")
        private final String postTypeDescription;

        @Schema(description = "게시글 제목")
        private final String title;

        @Schema(description = "게시글 요약")
        private final String text;

        @Schema(description = "작성자 이름")
        private final String userName;

        @Schema(description = "작성자 ID")
        private final Long userIdx;

        @Schema(description = "관련 코스명 (질문 타입일 때)")
        private final String courseName;

        @Schema(description = "조회 수")
        private final int viewCount;

        @Schema(description = "댓글 수")
        private final int commentCount;

        @Schema(description = "스크랩 수")
        private final long scrapCount;

        @Schema(description = "스크랩 여부")
        private final boolean scrapped;

        @Schema(description = "작성 시간")
        private final String createdAt;

        public static PostSummaryResponse from(Post post) {
            // commentCount 미지정 → comments 컬렉션 size (단건/이미 로딩된 경우에만 사용 권장)
            return from(post, 0L, false, Optional.ofNullable(post.getComments()).map(List::size).orElse(0));
        }

        public static PostSummaryResponse from(Post post, long scrapCount, boolean scrapped) {
            return from(post, scrapCount, scrapped,
                    Optional.ofNullable(post.getComments()).map(List::size).orElse(0));
        }

        /**
         * 댓글 수를 외부(일괄 COUNT 집계)에서 주입받는 목록용 변환.
         * 게시글마다 comments 컬렉션을 로딩하지 않아 목록 N+1/과대적재를 방지한다.
         */
        public static PostSummaryResponse from(Post post, long scrapCount, boolean scrapped, long commentCount) {
            return PostSummaryResponse.builder()
                    .idx(post.getIdx())
                    .postType(post.getPostType())
                    .postTypeDescription(post.getPostType().getDescription())
                    .title(post.getTitle())
                    .text(post.getText())
                    .userName(post.getAuthorName() != null ? post.getAuthorName() : "알 수 없음")
                    .userIdx(post.getUserIdx())
                    .courseName(post.getCourseName())
                    .viewCount(post.getViewCount())
                    .commentCount((int) commentCount)
                    .scrapCount(scrapCount)
                    .scrapped(scrapped)
                    .createdAt(TimeAgoUtil.timeAgo(post.getCreatedAt()))
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "게시글 상세 응답 (댓글 포함)")
    public static class PostDetailResponse {
        @Schema(description = "게시글 ID")
        private final Long idx;

        @Schema(description = "게시글 타입")
        private final PostType postType;

        @Schema(description = "게시글 타입 설명")
        private final String postTypeDescription;

        @Schema(description = "게시글 제목")
        private final String title;

        @Schema(description = "게시글 요약")
        private final String text;

        @Schema(description = "게시글 본문")
        private final String content;

        @Schema(description = "작성자 이름")
        private final String userName;

        @Schema(description = "작성자 ID")
        private final Long userIdx;

        @Schema(description = "작성자 프로필 이미지")
        private final String userProfileImageUrl;

        @Schema(description = "관련 코스 ID")
        private final Long courseIdx;

        @Schema(description = "관련 코스명")
        private final String courseName;

        @Schema(description = "관련 강의 ID")
        private final Long lectureIdx;

        @Schema(description = "관련 강의명")
        private final String lectureName;

        @Schema(description = "태그 목록")
        private final List<String> tags;

        @Schema(description = "조회 수")
        private final int viewCount;

        @Schema(description = "댓글 목록")
        private final List<CommentResponse> comments;

        @Schema(description = "스크랩 수")
        private final long scrapCount;

        @Schema(description = "스크랩 여부")
        private final boolean scrapped;

        @Schema(description = "작성 시간")
        private final String createdAt;

        @Schema(description = "수정 시간")
        private final String updatedAt;

        public static PostDetailResponse from(Post post) {
            return from(post, 0L, false);
        }

        public static PostDetailResponse from(Post post, long scrapCount, boolean scrapped) {
            List<CommentResponse> comments = Optional.ofNullable(post.getComments())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(CommentResponse::from)
                    .toList();

            return PostDetailResponse.builder()
                    .idx(post.getIdx())
                    .postType(post.getPostType())
                    .postTypeDescription(post.getPostType().getDescription())
                    .title(post.getTitle())
                    .text(post.getText())
                    .content(post.getContent())
                    .userName(post.getAuthorName() != null ? post.getAuthorName() : "알 수 없음")
                    .userIdx(post.getUserIdx())
                    .userProfileImageUrl(post.getAuthorProfileImageUrl())
                    .courseIdx(post.getCourseIdx())
                    .courseName(post.getCourseName())
                    .lectureIdx(post.getLectureIdx())
                    .lectureName(post.getLectureName())
                    .tags(new ArrayList<>(Optional.ofNullable(post.getTags()).orElse(Collections.emptySet())))
                    .viewCount(post.getViewCount())
                    .comments(comments)
                    .scrapCount(scrapCount)
                    .scrapped(scrapped)
                    .createdAt(TimeAgoUtil.timeAgo(post.getCreatedAt()))
                    .updatedAt(TimeAgoUtil.timeAgo(post.getUpdatedAt()))
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "명예의 전당(랭킹) 항목 응답")
    public static class PostRankingResponse {
        @Schema(description = "게시글 ID")
        private final Long idx;

        @Schema(description = "게시글 타입")
        private final PostType postType;

        @Schema(description = "게시글 타입 설명")
        private final String postTypeDescription;

        @Schema(description = "게시글 제목")
        private final String title;

        @Schema(description = "작성자 이름")
        private final String userName;

        @Schema(description = "조회 수")
        private final long viewCount;

        @Schema(description = "댓글 수")
        private final long commentCount;

        @Schema(description = "스크랩 수")
        private final long scrapCount;

        @Schema(description = "작성 시간")
        private final String createdAt;

        public static PostRankingResponse from(Post post, long commentCount, long scrapCount) {
            return PostRankingResponse.builder()
                    .idx(post.getIdx())
                    .postType(post.getPostType())
                    .postTypeDescription(post.getPostType().getDescription())
                    .title(post.getTitle())
                    .userName(post.getAuthorName() != null ? post.getAuthorName() : "알 수 없음")
                    .viewCount(post.getViewCount())
                    .commentCount(commentCount)
                    .scrapCount(scrapCount)
                    .createdAt(TimeAgoUtil.timeAgo(post.getCreatedAt()))
                    .build();
        }
    }

    @Getter
    @Builder
    @Schema(description = "댓글 응답")
    public static class CommentResponse {
        @Schema(description = "댓글 ID")
        private final Long idx;

        @Schema(description = "댓글 내용 (텍스트)")
        private final String text;

        @Schema(description = "댓글 내용 (HTML)")
        private final String content;

        @Schema(description = "작성자 이름")
        private final String userName;

        @Schema(description = "작성자 ID")
        private final Long userIdx;

        @Schema(description = "작성자 프로필 이미지")
        private final String userProfileImageUrl;

        @Schema(description = "채택된 베스트 답변 여부")
        private final boolean accepted;

        @Schema(description = "작성 시간")
        private final String createdAt;

        public static CommentResponse from(Comment comment) {
            return CommentResponse.builder()
                    .accepted(comment.isAccepted())
                    .idx(comment.getIdx())
                    .text(comment.getText())
                    .content(comment.getContent())
                    .userName(comment.getAuthorName() != null ? comment.getAuthorName() : "알 수 없음")
                    .userIdx(comment.getUserIdx())
                    .userProfileImageUrl(comment.getAuthorProfileImageUrl())
                    .createdAt(TimeAgoUtil.timeAgo(comment.getCreatedAt()))
                    .build();
        }
    }
}
