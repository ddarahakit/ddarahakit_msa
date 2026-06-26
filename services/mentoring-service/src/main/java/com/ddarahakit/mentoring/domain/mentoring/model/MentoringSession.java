package com.ddarahakit.mentoring.domain.mentoring.model;

import com.ddarahakit.mentoring.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "mentoring_session")
public class MentoringSession extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MentoringStatus status = MentoringStatus.OPEN;

    // === 멘토(FK 평문화 + 표시 스냅샷) ===
    @Column(name = "mentor_idx", nullable = false)
    private Long mentorIdx;

    @Column(name = "mentor_name")
    private String mentorName;

    @Column(name = "mentor_profile_image_url")
    private String mentorProfileImageUrl;

    // === 멘티(FK 평문화 + 표시 스냅샷) ===
    @Column(name = "mentee_idx", nullable = false)
    private Long menteeIdx;

    @Column(name = "mentee_name")
    private String menteeName;

    @Column(name = "mentee_profile_image_url")
    private String menteeProfileImageUrl;

    @Column(name = "mentor_read_at")
    private LocalDateTime mentorReadAt;

    @Column(name = "mentee_read_at")
    private LocalDateTime menteeReadAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "last_message", length = 1000)
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Builder.Default
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MentoringMessage> messages = new ArrayList<>();

    public void close(LocalDateTime closedAt) {
        this.status = MentoringStatus.CLOSED;
        this.closedAt = closedAt;
    }

    public void updateLastMessage(String message, LocalDateTime at) {
        this.lastMessage = message;
        this.lastMessageAt = at;
    }

    public void markReadByMentor(LocalDateTime at) {
        this.mentorReadAt = at;
    }

    public void markReadByMentee(LocalDateTime at) {
        this.menteeReadAt = at;
    }
}
