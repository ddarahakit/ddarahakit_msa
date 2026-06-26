package com.ddarahakit.mentoring.domain.mentoring.model;

import com.ddarahakit.mentoring.utils.TimeAgoUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class MentoringDto {

    @Getter
    public static class SendMessageReq {
        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(max = 2000, message = "메시지는 최대 2000자까지 가능합니다.")
        @Schema(description = "메시지 내용", required = true, example = "안녕하세요, 질문이 있습니다.")
        private String message;
    }

    @Getter
    public static class CreateSessionReq {
        @NotNull(message = "멘토 IDX는 필수 입력값입니다.")
        @Schema(description = "멘토 IDX", required = true, example = "1")
        private Long mentorIdx;

        @NotBlank(message = "주제는 필수 입력값입니다.")
        @Size(max = 100, message = "주제는 최대 100자까지 가능합니다.")
        @Schema(description = "주제", required = true, example = "프론트엔드 취업 준비")
        private String subject;

        @Schema(description = "예약 시간", example = "2026-02-10T19:00:00")
        private LocalDateTime scheduledAt;

        @Schema(description = "첫 메시지", example = "안녕하세요, 멘토링 신청합니다.")
        private String message;
    }

    @Getter
    @Builder
    public static class UserSummary {
        private Long idx;
        private String name;
        private String profileImageUrl;
    }

    @Getter
    @Builder
    public static class SessionRes {
        private Long sessionIdx;
        private String subject;
        private LocalDateTime scheduledAt;
        private MentoringStatus status;
        private UserSummary mentor;
        private UserSummary mentee;
        private LocalDateTime mentorReadAt;
        private LocalDateTime menteeReadAt;
        private LocalDateTime closedAt;
        private String lastMessage;
        private LocalDateTime lastMessageAt;

        public static SessionRes of(MentoringSession session) {
            return SessionRes.builder()
                    .sessionIdx(session.getIdx())
                    .subject(session.getSubject())
                    .scheduledAt(session.getScheduledAt())
                    .status(session.getStatus())
                    .mentor(UserSummary.builder()
                            .idx(session.getMentorIdx())
                            .name(session.getMentorName())
                            .profileImageUrl(session.getMentorProfileImageUrl())
                            .build())
                    .mentee(UserSummary.builder()
                            .idx(session.getMenteeIdx())
                            .name(session.getMenteeName())
                            .profileImageUrl(session.getMenteeProfileImageUrl())
                            .build())
                    .mentorReadAt(session.getMentorReadAt())
                    .menteeReadAt(session.getMenteeReadAt())
                    .closedAt(session.getClosedAt())
                    .lastMessage(session.getLastMessage())
                    .lastMessageAt(session.getLastMessageAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SessionListRes {
        private List<SessionRes> list;
        private boolean hasNext;

        public static SessionListRes of(Page<MentoringSession> page) {
            return SessionListRes.builder()
                    .list(page != null
                            ? page.getContent().stream().map(SessionRes::of).toList()
                            : Collections.emptyList())
                    .hasNext(page != null && page.hasNext())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MessageRes {
        private Long messageIdx;
        private Long sessionIdx;
        private String message;
        private String createdAt;
        private UserSummary sender;

        public static MessageRes of(MentoringMessage message) {
            return MessageRes.builder()
                    .messageIdx(message.getIdx())
                    .sessionIdx(message.getSession().getIdx())
                    .message(message.getMessage())
                    .createdAt(message.getCreatedAt() != null ? TimeAgoUtil.timeAgo(message.getCreatedAt()) : null)
                    .sender(UserSummary.builder()
                            .idx(message.getSenderIdx())
                            .name(message.getSenderName())
                            .profileImageUrl(message.getSenderProfileImageUrl())
                            .build())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MessagePageRes {
        private List<MessageRes> list;
        private boolean hasNext;

        public static MessagePageRes of(Page<MentoringMessage> page) {
            return MessagePageRes.builder()
                    .list(page != null
                            ? page.getContent().stream().map(MessageRes::of).toList()
                            : Collections.emptyList())
                    .hasNext(page != null && page.hasNext())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SessionDetailRes {
        private SessionRes session;
        private MessagePageRes messages;

        public static SessionDetailRes of(MentoringSession session, Page<MentoringMessage> messages) {
            return SessionDetailRes.builder()
                    .session(SessionRes.of(session))
                    .messages(MessagePageRes.of(messages))
                    .build();
        }
    }
}
