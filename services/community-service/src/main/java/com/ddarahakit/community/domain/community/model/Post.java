package com.ddarahakit.community.domain.community.model;

import com.ddarahakit.community.common.model.BaseEntity;
import com.ddarahakit.community.utils.HtmlSanitizer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType postType;

    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String text;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    // 조회수
    @Builder.Default
    @ColumnDefault("0")
    @Column(nullable = false)
    private int viewCount = 0;

    // === cross-domain FK 평문화 (MSA: user/course/lecture 는 별도 서비스) ===
    @Column(name = "user_idx")
    private Long userIdx;

    // 질문 타입일 때 선택적으로 연결 가능
    @Column(name = "course_idx")
    private Long courseIdx;

    @Column(name = "lecture_idx")
    private Long lectureIdx;

    // === 표시명 스냅샷 (작성 시점에 채움) ===
    // 작성자 스냅샷: 작성 시 identity-service 를 Feign 으로 조회해 채운다.
    private String authorName;

    private String authorProfileImageUrl;

    // 코스/강의명 스냅샷: 신규 작성 시 null. 추후 course-service 이벤트 투영으로 채운다. (TODO)
    private String courseName;

    private String lectureName;

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "post")
    private List<Comment> comments = new ArrayList<>();

    // 게시글 태그 (검색·관련 게시글 매칭용)
    @Builder.Default
    @BatchSize(size = 100)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "post_tag", joinColumns = @JoinColumn(name = "post_idx"))
    @Column(name = "tag", length = 30)
    private Set<String> tags = new LinkedHashSet<>();

    public void update(PostType postType, String title, String text, String content,
                       Long courseIdx, Long lectureIdx, Set<String> tags) {
        this.postType = postType;
        this.title = title;
        this.text = text;
        this.content = HtmlSanitizer.clean(content);
        this.courseIdx = courseIdx;
        this.lectureIdx = lectureIdx;
        // course/lecture 명 스냅샷은 추후 이벤트 투영으로 갱신한다. (TODO)
        // @ElementCollection은 참조 교체보다 in-place 변경이 안전
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }
}
