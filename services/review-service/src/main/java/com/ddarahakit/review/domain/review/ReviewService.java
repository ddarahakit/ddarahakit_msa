package com.ddarahakit.review.domain.review;

import com.ddarahakit.common.event.EventType;
import com.ddarahakit.common.event.Topics;
import com.ddarahakit.common.event.payload.ReviewEvents;
import com.ddarahakit.review.client.CourseClient;
import com.ddarahakit.review.client.CourseNameClient;
import com.ddarahakit.review.client.IdentityClient;
import com.ddarahakit.review.common.exception.BaseException;
import com.ddarahakit.review.config.security.AuthUserDetails;
import com.ddarahakit.review.domain.review.model.Review;
import com.ddarahakit.review.domain.review.model.ReviewDto;
import com.ddarahakit.review.messaging.outbox.OutboxAppender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.ddarahakit.review.common.model.BaseResponseStatus.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final CourseClient courseClient;
    private final CourseNameClient courseNameClient;
    private final IdentityClient identityClient;
    private final OutboxAppender outboxAppender;

    /**
     * 내 리뷰: 현재 사용자가 작성한 리뷰 목록.
     * 모놀리스 UserService.getMyReviewList 이식. 코스명은 course-service Feign 으로 조회(실패 시 null 폴백),
     * 동일 코스 중복 호출을 피하기 위해 코스명을 캐시한다.
     */
    public java.util.List<ReviewDto.MyReviewRes> getMyReviewList(AuthUserDetails authUserDetails) {
        java.util.List<Review> reviews = reviewRepository.findByUserIdx(authUserDetails.getIdx());
        java.util.Map<Long, String> courseNameCache = new java.util.HashMap<>();
        return reviews.stream()
                .map(review -> ReviewDto.MyReviewRes.of(
                        review,
                        courseNameCache.computeIfAbsent(review.getCourseIdx(), this::resolveCourseName)))
                .toList();
    }

    /** course-service 에서 코스명 조회. 실패 시 null 폴백(목록 200 보장). */
    private String resolveCourseName(Long courseIdx) {
        try {
            CourseNameClient.CourseResponse res = courseNameClient.get(courseIdx);
            return res != null && res.results() != null ? res.results().name() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public ReviewDto.ReviewPageRes readReview(AuthUserDetails authUserDetails, Long courseIdx, Pageable pageable) {
        requireCourse(courseIdx);

        Slice<Review> reviewPage;
        if (authUserDetails != null) {
            reviewPage = reviewRepository.findByUserIdxNotAndCourseIdxOrderByCreatedAtDesc(
                    authUserDetails.getIdx(), courseIdx, pageable
            );
        } else {
            reviewPage = reviewRepository.findByCourseIdx(courseIdx, pageable);
        }

        return ReviewDto.ReviewPageRes.of(reviewPage);
    }

    /**
     * 리뷰 작성.
     * 코스 존재(Feign)·작성자명(Feign) 확인 후 리뷰를 저장하고,
     * 같은 트랜잭션에서 ReviewCreated 이벤트를 아웃박스에 적재한다(평점 갱신은 course 가 구독).
     */
    @Transactional
    public ReviewDto.ReviewRes createReview(AuthUserDetails authUserDetails, Long courseIdx, ReviewDto.ReviewReq dto) {
        requireCourse(courseIdx);

        Long userIdx = authUserDetails.getIdx();
        String authorName = resolveAuthorName(userIdx);

        Review review = reviewRepository.save(dto.toEntity(userIdx, courseIdx, authorName));

        outboxAppender.append(Topics.REVIEW_REVIEW, EventType.REVIEW_CREATED,
                String.valueOf(courseIdx),
                new ReviewEvents.ReviewCreated(review.getIdx(), courseIdx, userIdx, dto.getRating()));

        return ReviewDto.ReviewRes.of(review);
    }

    /**
     * 리뷰 수정.
     * 평점이 변경된 경우에만 ReviewUpdated(oldRating/newRating) 이벤트를 발행한다.
     */
    @Transactional
    public ReviewDto.ReviewRes updateReview(AuthUserDetails authUserDetails, Long courseIdx, ReviewDto.ReviewReq dto) {
        requireCourse(courseIdx);
        Review review = reviewRepository.findByUserIdxAndCourseIdx(authUserDetails.getIdx(), courseIdx).orElseThrow(
                () -> BaseException.of(REVIEW_NOT_FOUND)
        );

        int previousRating = review.getRating();
        int newRating = dto.getRating();

        review.setRating(newRating);
        review.setComment(dto.getComment());
        review = reviewRepository.save(review);

        if (previousRating != newRating) {
            outboxAppender.append(Topics.REVIEW_REVIEW, EventType.REVIEW_UPDATED,
                    String.valueOf(courseIdx),
                    new ReviewEvents.ReviewUpdated(review.getIdx(), courseIdx, previousRating, newRating));
        }

        return ReviewDto.ReviewRes.of(review);
    }

    /**
     * 리뷰 삭제.
     * 삭제 전 평점을 보관한 뒤 ReviewDeleted 이벤트를 발행한다.
     */
    @Transactional
    public ReviewDto.ReviewRes remove(AuthUserDetails authUserDetails, Long courseIdx) {
        requireCourse(courseIdx);
        Review review = reviewRepository.findByUserIdxAndCourseIdx(authUserDetails.getIdx(), courseIdx).orElseThrow(
                () -> BaseException.of(REVIEW_NOT_FOUND)
        );

        int removedRating = review.getRating();
        Long reviewId = review.getIdx();
        ReviewDto.ReviewRes response = ReviewDto.ReviewRes.of(review);

        reviewRepository.delete(review);

        outboxAppender.append(Topics.REVIEW_REVIEW, EventType.REVIEW_DELETED,
                String.valueOf(courseIdx),
                new ReviewEvents.ReviewDeleted(reviewId, courseIdx, removedRating));

        return response;
    }

    /** 코스 존재 확인 (모놀리스/course-service Feign). */
    private void requireCourse(Long courseIdx) {
        try {
            CourseClient.CourseResponse res = courseClient.get(courseIdx);
            if (res == null || res.results() == null) {
                throw BaseException.of(COURSE_NOT_FOUND);
            }
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw BaseException.of(COURSE_NOT_FOUND);
        }
    }

    /** 작성자 표시명 스냅샷 조회 (identity-service Feign). 실패 시 null 허용. */
    private String resolveAuthorName(Long userIdx) {
        try {
            IdentityClient.UserSummary user = identityClient.getUser(userIdx);
            return user != null ? user.name() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
