package com.ddarahakit.mentoring.domain.mentoring.model;

import com.ddarahakit.mentoring.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "mentoring_message")
public class MentoringMessage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_idx", nullable = false)
    private MentoringSession session;

    // === 발신자(FK 평문화 + 표시 스냅샷) ===
    @Column(name = "sender_idx", nullable = false)
    private Long senderIdx;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_profile_image_url")
    private String senderProfileImageUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
}
