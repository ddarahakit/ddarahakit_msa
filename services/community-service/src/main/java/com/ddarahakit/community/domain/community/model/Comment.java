package com.ddarahakit.community.domain.community.model;

import com.ddarahakit.community.common.model.BaseEntity;
import com.ddarahakit.community.utils.HtmlSanitizer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(columnDefinition = "LONGTEXT")
    private String text;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    // 질문 작성자가 채택한 베스트 답변 여부
    @Builder.Default
    @ColumnDefault("0")
    @Column(nullable = false)
    private boolean accepted = false;

    // === cross-domain FK 평문화 + 작성자 스냅샷 ===
    @Column(name = "user_idx")
    private Long userIdx;

    private String authorName;

    private String authorProfileImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_idx")
    private Post post;

    public void update(String text, String content) {
        this.text = text;
        this.content = HtmlSanitizer.clean(content);
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
