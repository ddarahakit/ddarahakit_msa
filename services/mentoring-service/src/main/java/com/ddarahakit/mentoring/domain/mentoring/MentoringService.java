package com.ddarahakit.mentoring.domain.mentoring;

import com.ddarahakit.mentoring.client.IdentityClient;
import com.ddarahakit.mentoring.common.exception.BaseException;
import com.ddarahakit.mentoring.config.security.AuthUserDetails;
import com.ddarahakit.mentoring.domain.mentoring.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.ddarahakit.mentoring.common.model.BaseResponseStatus.*;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class MentoringService {
    private static final String UNKNOWN_NAME = "알 수 없음";

    private final MentoringSessionRepository sessionRepository;
    private final MentoringMessageRepository messageRepository;
    private final IdentityClient identityClient;

    public MentoringDto.SessionListRes list(AuthUserDetails authUserDetails, int page, int size, String keyword, MentoringStatus status) {
        Page<MentoringSession> sessions = sessionRepository.findForUser(
                authUserDetails.getIdx(),
                status,
                normalizeKeyword(keyword),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        return MentoringDto.SessionListRes.of(sessions);
    }

    public MentoringDto.SessionDetailRes detail(AuthUserDetails authUserDetails, Long sessionIdx) {
        MentoringSession session = findSessionForUser(authUserDetails, sessionIdx);
        Page<MentoringMessage> messages = messageRepository.findBySessionOrderByIdxDesc(
                session, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "idx"))
        );
        return MentoringDto.SessionDetailRes.of(session, messages);
    }

    public MentoringDto.MessagePageRes messages(AuthUserDetails authUserDetails, Long sessionIdx, int page, int size, Long beforeIdx) {
        MentoringSession session = findSessionForUser(authUserDetails, sessionIdx);
        Page<MentoringMessage> messages;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "idx"));
        if (beforeIdx != null && beforeIdx > 0) {
            messages = messageRepository.findBySessionAndIdxLessThanOrderByIdxDesc(session, beforeIdx, pageable);
        } else {
            messages = messageRepository.findBySessionOrderByIdxDesc(session, pageable);
        }
        return MentoringDto.MessagePageRes.of(messages);
    }

    @Transactional
    public MentoringDto.SessionRes createSession(AuthUserDetails authUserDetails, MentoringDto.CreateSessionReq dto) {
        if ("ROLE_MENTOR".equals(authUserDetails.getRole())) {
            throw BaseException.of(INVALID_USER_ROLE);
        }

        // 멘토/멘티 표시 스냅샷을 identity-service 에서 동기 조회 (실패 시 폴백)
        IdentityClient.UserSummary mentor = fetchUser(dto.getMentorIdx());
        IdentityClient.UserSummary mentee = fetchUser(authUserDetails.getIdx());

        MentoringSession session = MentoringSession.builder()
                .mentorIdx(dto.getMentorIdx())
                .mentorName(mentor.name())
                .mentorProfileImageUrl(mentor.profileImageUrl())
                .menteeIdx(authUserDetails.getIdx())
                .menteeName(mentee.name())
                .menteeProfileImageUrl(mentee.profileImageUrl())
                .subject(dto.getSubject())
                .scheduledAt(dto.getScheduledAt())
                .status(MentoringStatus.OPEN)
                .build();

        session = sessionRepository.save(session);

        if (dto.getMessage() != null && !dto.getMessage().isBlank()) {
            MentoringMessage message = MentoringMessage.builder()
                    .session(session)
                    .senderIdx(authUserDetails.getIdx())
                    .senderName(mentee.name())
                    .senderProfileImageUrl(mentee.profileImageUrl())
                    .message(dto.getMessage())
                    .build();
            messageRepository.save(message);
            session.updateLastMessage(dto.getMessage(), LocalDateTime.now());
        }

        session.markReadByMentee(LocalDateTime.now());
        sessionRepository.save(session);

        return MentoringDto.SessionRes.of(session);
    }

    @Transactional
    public MentoringDto.SessionRes markAsRead(AuthUserDetails authUserDetails, Long sessionIdx) {
        MentoringSession session = findSessionForUser(authUserDetails, sessionIdx);
        LocalDateTime now = LocalDateTime.now();
        if (session.getMentorIdx().equals(authUserDetails.getIdx())) {
            session.markReadByMentor(now);
        } else {
            session.markReadByMentee(now);
        }
        sessionRepository.save(session);
        return MentoringDto.SessionRes.of(session);
    }

    @Transactional
    public MentoringDto.MessageRes sendMessage(AuthUserDetails authUserDetails, Long sessionIdx, MentoringDto.SendMessageReq dto) {
        MentoringSession session = findSessionForUser(authUserDetails, sessionIdx);

        if (session.getStatus() == MentoringStatus.CLOSED) {
            throw BaseException.of(MENTORING_SESSION_CLOSED);
        }

        IdentityClient.UserSummary sender = fetchUser(authUserDetails.getIdx());

        MentoringMessage message = MentoringMessage.builder()
                .session(session)
                .senderIdx(authUserDetails.getIdx())
                .senderName(sender.name())
                .senderProfileImageUrl(sender.profileImageUrl())
                .message(dto.getMessage())
                .build();
        message = messageRepository.save(message);

        session.updateLastMessage(dto.getMessage(), LocalDateTime.now());
        sessionRepository.save(session);

        return MentoringDto.MessageRes.of(message);
    }

    @Transactional
    public MentoringDto.SessionRes close(AuthUserDetails authUserDetails, Long sessionIdx) {
        MentoringSession session = findSessionForUser(authUserDetails, sessionIdx);
        if (session.getStatus() != MentoringStatus.CLOSED) {
            session.close(LocalDateTime.now());
            sessionRepository.save(session);
        }
        return MentoringDto.SessionRes.of(session);
    }

    private MentoringSession findSessionForUser(AuthUserDetails authUserDetails, Long sessionIdx) {
        MentoringSession session = sessionRepository.findById(sessionIdx).orElseThrow(
                () -> BaseException.of(MENTORING_NOT_FOUND)
        );
        Long userIdx = authUserDetails.getIdx();
        if (!session.getMentorIdx().equals(userIdx) && !session.getMenteeIdx().equals(userIdx)) {
            throw BaseException.of(MENTORING_FORBIDDEN);
        }
        return session;
    }

    /**
     * identity-service 로 사용자 표시 스냅샷을 조회한다.
     * Feign 호출 실패 시 name="알 수 없음" 으로 폴백한다.
     */
    private IdentityClient.UserSummary fetchUser(Long idx) {
        try {
            IdentityClient.UserSummary user = identityClient.getUser(idx);
            if (user != null) {
                return user;
            }
        } catch (Exception e) {
            log.warn("[IDENTITY] 사용자 스냅샷 조회 실패 idx={}: {}", idx, e.getMessage());
        }
        return new IdentityClient.UserSummary(idx, UNKNOWN_NAME, null);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
