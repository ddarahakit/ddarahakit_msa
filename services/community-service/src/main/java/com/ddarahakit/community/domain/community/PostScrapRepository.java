package com.ddarahakit.community.domain.community;

import com.ddarahakit.community.domain.community.model.Post;
import com.ddarahakit.community.domain.community.model.PostScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostScrapRepository extends JpaRepository<PostScrap, Long> {
    boolean existsByUserIdxAndPost(Long userIdx, Post post);

    Optional<PostScrap> findByUserIdxAndPost(Long userIdx, Post post);

    long countByPost(Post post);

    @Query("select ps.post.idx, count(ps) from PostScrap ps where ps.post in :posts group by ps.post.idx")
    List<Object[]> countByPostIn(@Param("posts") List<Post> posts);

    @Query("select ps.post.idx from PostScrap ps where ps.userIdx = :userIdx and ps.post in :posts")
    List<Long> findPostIdxByUserIdxAndPostIn(@Param("userIdx") Long userIdx, @Param("posts") List<Post> posts);

    void deleteAllByPost(Post post);
}
