package com.sa.trk.board.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sa.trk.board.entity.BoardPost;
import com.sa.trk.board.entity.BoardType;

public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {

    List<BoardPost> findByTypeOrNoticeTrueOrderByNoticeDescCreatedAtDesc(BoardType type);

    @Query("""
            select post from BoardPost post
            where post.notice = true
               or (post.type = :type and post.authorId = :authorId)
            order by post.notice desc, post.createdAt desc
            """)
    List<BoardPost> findVisiblePosts(
            @Param("type") BoardType type,
            @Param("authorId") Long authorId);

    @Query("""
            select distinct post from BoardPost post
            join post.imageUrls imageUrl
            where imageUrl = :imageUrl
            """)
    Optional<BoardPost> findByImageUrl(@Param("imageUrl") String imageUrl);

    List<BoardPost> findAllByOrderByNoticeDescCreatedAtDesc();
}
