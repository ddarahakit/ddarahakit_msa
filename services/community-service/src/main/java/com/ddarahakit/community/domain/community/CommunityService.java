package com.ddarahakit.community.domain.community;

import com.ddarahakit.community.client.IdentityClient;
import com.ddarahakit.community.common.exception.BaseException;
import com.ddarahakit.community.config.security.AuthUserDetails;
import com.ddarahakit.community.domain.community.model.Comment;
import com.ddarahakit.community.domain.community.model.CommunityDto.CommentCreateRequest;
import com.ddarahakit.community.domain.community.model.CommunityDto.CommentResponse;
import com.ddarahakit.community.domain.community.model.CommunityDto.CommentUpdateRequest;
import com.ddarahakit.community.domain.community.model.CommunityDto.PostCreateRequest;
import com.ddarahakit.community.domain.community.model.CommunityDto.PostDetailResponse;
import com.ddarahakit.community.domain.community.model.CommunityDto.PostPageResponse;
import com.ddarahakit.community.domain.community.model.CommunityDto.PostRankingResponse;
import com.ddarahakit.community.domain.community.model.CommunityDto.PostSummaryResponse;
import com.ddarahakit.community.domain.community.model.CommunityDto.PostUpdateRequest;
import com.ddarahakit.community.domain.community.model.Post;
import com.ddarahakit.community.domain.community.model.PostScrap;
import com.ddarahakit.community.domain.community.model.PostType;
import com.ddarahakit.community.utils.TagUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ddarahakit.community.common.model.BaseResponseStatus.*;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CommunityService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String UNKNOWN_AUTHOR = "알 수 없음";

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostScrapRepository postScrapRepository;
    private final IdentityClient identityClient;

    /**
     * 게시글 목록 조회
     * - postType: 게시글 타입으로 필터링
     * - keyword: 제목/내용 검색
     */
    public PostPageResponse getPostList(AuthUserDetails authUserDetails, PostType postType, String keyword, Long courseIdx, Pageable pageable) {
        Page<Post> postPage = findPosts(postType, keyword, courseIdx, pageable);
        return buildPostPageResponse(postPage, authUserDetails);
    }

    /**
     * 게시글 상세 조회
     * - 조회 시 조회수 +1 (원자적 증가 후 최신 값으로 응답)
     */
    @Transactional
    public PostDetailResponse getPostDetail(AuthUserDetails authUserDetails, Long postIdx) {
        postRepository.incrementViewCount(postIdx);

        Post post = postRepository.findByIdWithUserAndComments(postIdx)
                .orElseThrow(() -> BaseException.of(POST_NOT_FOUND));

        long scrapCount = postScrapRepository.countByPost(post);
        boolean scrapped = authUserDetails != null &&
                postScrapRepository.existsByUserIdxAndPost(authUserDetails.getIdx(), post);
        return PostDetailResponse.from(post, scrapCount, scrapped);
    }

    /**
     * 게시글 작성
     * - 공지사항은 관리자만 작성 가능
     * - 질문 타입일 때 코스/강의 ID 선택 가능
     * - 작성자 표시명은 identity-service 를 Feign 으로 조회해 스냅샷에 저장
     */
    @Transactional
    public PostDetailResponse createPost(AuthUserDetails authUserDetails, PostCreateRequest request) {
        validateNoticePermission(authUserDetails, request.getPostType());

        Long userIdx = authUserDetails.getIdx();
        AuthorSnapshot author = resolveAuthor(userIdx);

        Post post = request.toEntity(userIdx, author.name(), author.profileImageUrl());
        Post savedPost = postRepository.save(post);

        return PostDetailResponse.from(savedPost);
    }

    /**
     * 게시글 수정
     * - 작성자 본인 또는 관리자(NOTICE 타입만)가 수정 가능
     */
    @Transactional
    public PostDetailResponse updatePost(AuthUserDetails authUserDetails, Long postIdx, PostUpdateRequest request) {
        Post post = postRepository.findByIdWithUserAndComments(postIdx)
                .orElseThrow(() -> BaseException.of(POST_NOT_FOUND));

        boolean isOwner = post.getUserIdx() != null && post.getUserIdx().equals(authUserDetails.getIdx());
        boolean isAdmin = isAdmin(authUserDetails);

        // 관리자는 NOTICE 타입 게시글 수정 가능, 일반 사용자는 본인 게시글만 수정 가능
        if (!isOwner && !(isAdmin && post.getPostType() == PostType.NOTICE)) {
            throw BaseException.of(POST_UNAUTHORIZED);
        }

        validateNoticePermission(authUserDetails, request.getPostType());

        post.update(request.getPostType(), request.getTitle(), request.getText(), request.getContent(),
                request.getCourseIdx(), request.getLectureIdx(),
                TagUtils.normalize(request.getTags()));

        long scrapCount = postScrapRepository.countByPost(post);
        boolean scrapped = postScrapRepository.existsByUserIdxAndPost(authUserDetails.getIdx(), post);
        return PostDetailResponse.from(post, scrapCount, scrapped);
    }

    /**
     * 게시글 삭제
     * - 작성자 본인 또는 관리자만 가능
     * - 연관된 PostScrap 먼저 삭제
     */
    @Transactional
    public void deletePost(AuthUserDetails authUserDetails, Long postIdx) {
        Post post = postRepository.findById(postIdx)
                .orElseThrow(() -> BaseException.of(POST_NOT_FOUND));

        boolean isOwner = post.getUserIdx() != null && post.getUserIdx().equals(authUserDetails.getIdx());
        boolean isAdmin = isAdmin(authUserDetails);
        if (!isOwner && !isAdmin) {
            throw BaseException.of(POST_UNAUTHORIZED);
        }

        postScrapRepository.deleteAllByPost(post);
        postRepository.delete(post);
    }

    /**
     * 댓글 수정
     * - 작성자 본인만 가능
     */
    @Transactional
    public CommentResponse updateComment(AuthUserDetails authUserDetails, Long commentIdx, CommentUpdateRequest request) {
        Comment comment = commentRepository.findById(commentIdx)
                .orElseThrow(() -> BaseException.of(COMMENT_NOT_FOUND));

        if (comment.getUserIdx() == null || !comment.getUserIdx().equals(authUserDetails.getIdx())) {
            throw BaseException.of(COMMENT_UNAUTHORIZED);
        }

        comment.update(request.getText(), request.getContent());
        return CommentResponse.from(comment);
    }

    /**
     * 댓글 삭제
     * - 작성자 본인 또는 관리자만 가능
     */
    @Transactional
    public void deleteComment(AuthUserDetails authUserDetails, Long commentIdx) {
        Comment comment = commentRepository.findById(commentIdx)
                .orElseThrow(() -> BaseException.of(COMMENT_NOT_FOUND));

        boolean isOwner = comment.getUserIdx() != null && comment.getUserIdx().equals(authUserDetails.getIdx());
        boolean isAdmin = isAdmin(authUserDetails);
        if (!isOwner && !isAdmin) {
            throw BaseException.of(COMMENT_UNAUTHORIZED);
        }

        commentRepository.delete(comment);
    }

    /**
     * 댓글 작성
     * - 작성자 표시명은 identity-service 를 Feign 으로 조회해 스냅샷에 저장
     */
    @Transactional
    public CommentResponse createComment(AuthUserDetails authUserDetails, CommentCreateRequest request) {
        Post post = postRepository.findById(request.getPostIdx())
                .orElseThrow(() -> BaseException.of(POST_NOT_FOUND));

        Long userIdx = authUserDetails.getIdx();
        AuthorSnapshot author = resolveAuthor(userIdx);

        Comment comment = request.toEntity(userIdx, author.name(), author.profileImageUrl(), post);
        Comment savedComment = commentRepository.save(comment);

        return CommentResponse.from(savedComment);
    }

    /**
     * 베스트 답변 채택 (토글)
     * - 질문(게시글) 작성자만 채택 가능
     * - 게시글당 채택 답변은 하나 (다른 채택은 자동 해제)
     * - 이미 채택된 답변을 다시 채택하면 채택 해제
     *
     * @return 토글 후 채택 여부(true=채택, false=해제)
     */
    @Transactional
    public boolean acceptComment(AuthUserDetails authUserDetails, Long commentIdx) {
        Comment comment = commentRepository.findById(commentIdx)
                .orElseThrow(() -> BaseException.of(COMMENT_NOT_FOUND));

        Post post = comment.getPost();
        if (post == null || post.getUserIdx() == null
                || !post.getUserIdx().equals(authUserDetails.getIdx())) {
            // 질문 작성자만 채택할 수 있다
            throw BaseException.of(COMMENT_UNAUTHORIZED);
        }

        boolean newState = !comment.isAccepted();

        // 한 게시글당 채택 답변은 하나만 → 형제 댓글을 모두 정리하고 토글 결과만 반영
        List<Comment> siblings = commentRepository.findByPost(post);
        for (Comment c : siblings) {
            c.setAccepted(c.getIdx().equals(commentIdx) && newState);
        }

        return newState;
    }

    /**
     * 게시글 스크랩
     */
    @Transactional
    public void scrap(AuthUserDetails authUserDetails, Long postIdx) {
        Post post = postRepository.findById(postIdx)
                .orElseThrow(() -> BaseException.of(POST_NOT_FOUND));

        boolean exists = postScrapRepository.existsByUserIdxAndPost(authUserDetails.getIdx(), post);
        if (exists) {
            throw BaseException.of(SCRAP_ALREADY_EXISTS);
        }

        PostScrap scrap = PostScrap.builder()
                .userIdx(authUserDetails.getIdx())
                .post(post)
                .build();
        postScrapRepository.save(scrap);
    }

    /**
     * 게시글 스크랩 취소
     */
    @Transactional
    public void unscrap(AuthUserDetails authUserDetails, Long postIdx) {
        Post post = postRepository.findById(postIdx)
                .orElseThrow(() -> BaseException.of(POST_NOT_FOUND));

        PostScrap scrap = postScrapRepository.findByUserIdxAndPost(authUserDetails.getIdx(), post)
                .orElseThrow(() -> BaseException.of(SCRAP_NOT_FOUND));
        postScrapRepository.delete(scrap);
    }

    /**
     * 게시글 스크랩 토글
     */
    @Transactional
    public boolean toggleScrap(AuthUserDetails authUserDetails, Long postIdx) {
        Post post = postRepository.findById(postIdx)
                .orElseThrow(() -> BaseException.of(POST_NOT_FOUND));

        return postScrapRepository.findByUserIdxAndPost(authUserDetails.getIdx(), post)
                .map(scrap -> {
                    postScrapRepository.delete(scrap);
                    return false;
                })
                .orElseGet(() -> {
                    PostScrap scrap = PostScrap.builder()
                            .userIdx(authUserDetails.getIdx())
                            .post(post)
                            .build();
                    postScrapRepository.save(scrap);
                    return true;
                });
    }

    /**
     * 명예의 전당(랭킹) 조회
     * - 인기도(조회수 + 댓글 수 + 스크랩 수) 내림차순, 동점 시 최신순으로 상위 limit 개.
     */
    public List<PostRankingResponse> getRanking(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return postRepository.findRanking(pageable).stream()
                .map(row -> PostRankingResponse.from(
                        (Post) row[0],
                        (Long) row[1],
                        (Long) row[2]
                ))
                .toList();
    }

    /**
     * 관련 게시글 조회
     * - 1순위: 태그가 겹치는 다른 게시글(공유 태그 수 → 최신순)
     * - 부족하면 같은 코스(없으면 같은 타입)의 최신 글로 보충
     * - 현재 글은 제외, 최대 limit 개
     */
    public List<PostSummaryResponse> getRelatedPosts(Long postIdx, int limit) {
        Post post = postRepository.findById(postIdx)
                .orElseThrow(() -> BaseException.of(POST_NOT_FOUND));

        Pageable pageable = PageRequest.of(0, limit);

        // 순서 유지 + 중복 제거
        LinkedHashMap<Long, Post> collected = new LinkedHashMap<>();

        // 1) 태그 기반
        Set<String> tags = post.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<Long> ids = postRepository.findRelatedPostIdsByTags(tags, postIdx, pageable);
            if (!ids.isEmpty()) {
                Map<Long, Post> byId = postRepository.findAllByIdInWithUser(ids).stream()
                        .collect(Collectors.toMap(Post::getIdx, p -> p));
                // 공유 태그 수 정렬 순서(ids) 유지
                ids.forEach(id -> {
                    Post p = byId.get(id);
                    if (p != null) {
                        collected.put(id, p);
                    }
                });
            }
        }

        // 2) 부족하면 같은 코스 → 같은 타입으로 보충
        if (collected.size() < limit) {
            List<Post> fallback = post.getCourseIdx() != null
                    ? postRepository.findRelatedByCourse(post.getCourseIdx(), postIdx, pageable)
                    : postRepository.findRelatedByPostType(post.getPostType(), postIdx, pageable);
            for (Post p : fallback) {
                if (collected.size() >= limit) {
                    break;
                }
                collected.putIfAbsent(p.getIdx(), p);
            }
        }

        return collected.values().stream()
                .limit(limit)
                .map(PostSummaryResponse::from)
                .toList();
    }

    /**
     * 스크랩한 게시글 목록
     */
    public PostPageResponse getScrapList(AuthUserDetails authUserDetails, Pageable pageable) {
        Page<Post> postPage = postRepository.findScrappedPostsByUserIdx(authUserDetails.getIdx(), pageable);
        return buildPostPageResponse(postPage, authUserDetails);
    }

    // ================================
    // Private Methods
    // ================================

    private Page<Post> findPosts(PostType postType, String keyword, Long courseIdx, Pageable pageable) {
        boolean hasPostType = postType != null;
        boolean hasKeyword = StringUtils.hasText(keyword);
        boolean hasCourse = courseIdx != null;

        if (hasCourse) {
            if (hasPostType && hasKeyword) {
                return postRepository.searchByPostTypeAndKeywordWithUserByCourse(postType, keyword, courseIdx, pageable);
            }
            if (hasPostType) {
                return postRepository.findAllByPostTypeWithUserByCourse(postType, courseIdx, pageable);
            }
            if (hasKeyword) {
                return postRepository.searchByKeywordWithUserByCourse(keyword, courseIdx, pageable);
            }
            return postRepository.findAllWithUserByCourse(courseIdx, pageable);
        }

        if (hasPostType && hasKeyword) {
            return postRepository.searchByPostTypeAndKeywordWithUser(postType, keyword, pageable);
        }
        if (hasPostType) {
            return postRepository.findAllByPostTypeWithUser(postType, pageable);
        }
        if (hasKeyword) {
            return postRepository.searchByKeywordWithUser(keyword, pageable);
        }
        return postRepository.findAllWithUser(pageable);
    }

    /**
     * identity-service 를 Feign 으로 동기 호출해 작성자 표시명 스냅샷을 조회한다.
     * 호출 실패 시 name="알 수 없음", image=null 로 폴백한다.
     */
    private AuthorSnapshot resolveAuthor(Long userIdx) {
        try {
            IdentityClient.UserSummary user = identityClient.getUser(userIdx);
            if (user != null) {
                String name = StringUtils.hasText(user.name()) ? user.name() : UNKNOWN_AUTHOR;
                return new AuthorSnapshot(name, user.profileImageUrl());
            }
        } catch (Exception e) {
            log.warn("작성자 정보 조회 실패(userIdx={}): {}", userIdx, e.getMessage());
        }
        return new AuthorSnapshot(UNKNOWN_AUTHOR, null);
    }

    private void validateNoticePermission(AuthUserDetails authUserDetails, PostType postType) {
        if (postType == PostType.NOTICE && !isAdmin(authUserDetails)) {
            throw BaseException.of(INVALID_USER_ROLE);
        }
    }

    private boolean isAdmin(AuthUserDetails authUserDetails) {
        return ROLE_ADMIN.equals(authUserDetails.getRole());
    }

    private PostPageResponse buildPostPageResponse(Page<Post> postPage, AuthUserDetails authUserDetails) {
        List<Post> posts = postPage.getContent();
        final Map<Long, Long> scrapCountMap = posts.isEmpty()
                ? Map.of()
                : postScrapRepository.countByPostIn(posts).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
        // 댓글 수를 일괄 COUNT 로 집계(게시글마다 comments 컬렉션을 로딩하던 N+1/과대적재 제거)
        final Map<Long, Long> commentCountMap = posts.isEmpty()
                ? Map.of()
                : commentRepository.countByPostIn(posts).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
        final Set<Long> scrappedPostIds = (authUserDetails != null && !posts.isEmpty())
                ? postScrapRepository.findPostIdxByUserIdxAndPostIn(authUserDetails.getIdx(), posts)
                .stream()
                .collect(Collectors.toSet())
                : Set.of();

        List<PostSummaryResponse> summaries = posts.stream()
                .map(post -> PostSummaryResponse.from(
                        post,
                        scrapCountMap.getOrDefault(post.getIdx(), 0L),
                        scrappedPostIds.contains(post.getIdx()),
                        commentCountMap.getOrDefault(post.getIdx(), 0L)
                ))
                .toList();

        return PostPageResponse.builder()
                .page(postPage.getNumber())
                .size(postPage.getSize())
                .hasNext(postPage.hasNext())
                .hasPrev(postPage.hasPrevious())
                .totalPages(postPage.getTotalPages())
                .totalPosts(postPage.getTotalElements())
                .posts(summaries)
                .build();
    }

    private record AuthorSnapshot(String name, String profileImageUrl) {
    }
}
