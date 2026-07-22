package com.sa.trk.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.repository.AuthUserRepository;
import com.sa.trk.auth.service.AuthException;
import com.sa.trk.board.entity.BoardPost;
import com.sa.trk.board.entity.BoardType;
import com.sa.trk.board.repository.BoardPostRepository;

class BoardPostServiceTests {

    @Mock
    private BoardPostRepository boardPostRepository;

    @Mock
    private AuthUserRepository authUserRepository;

    private BoardPostService boardPostService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        boardPostService = new BoardPostService(boardPostRepository, authUserRepository);
    }

    @Test
    void regularUserOnlyLoadsOwnSupportPostsAndNotices() {
        AuthUserResponse viewer = viewer(7L, false);
        BoardPost privatePost = post(10L, BoardType.SUPPORT, 7L, false);
        when(boardPostRepository.findVisiblePosts(BoardType.SUPPORT, 7L)).thenReturn(List.of(privatePost));
        when(authUserRepository.findById(7L)).thenReturn(Optional.of(linkedUser(7L, false)));

        var response = boardPostService.getPosts("SUPPORT", viewer);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).privatePost()).isTrue();
        assertThat(response.get(0).authorLinked()).isTrue();
        verify(boardPostRepository).findVisiblePosts(BoardType.SUPPORT, 7L);
    }

    @Test
    void anotherUserCannotReadPrivateSupportPost() {
        BoardPost privatePost = post(10L, BoardType.SUPPORT, 7L, false);
        when(boardPostRepository.findById(10L)).thenReturn(Optional.of(privatePost));

        assertThatThrownBy(() -> boardPostService.getPost(10L, viewer(8L, false)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("작성자와 관리자");
    }

    @Test
    void adminCanReadPrivateSupportPost() {
        BoardPost privatePost = post(10L, BoardType.SUPPORT, 7L, false);
        when(boardPostRepository.findById(10L)).thenReturn(Optional.of(privatePost));
        when(authUserRepository.findById(7L)).thenReturn(Optional.of(linkedUser(7L, true)));

        var response = boardPostService.getPost(10L, viewer(1L, true));

        assertThat(response.privatePost()).isTrue();
        assertThat(response.authorVerified()).isTrue();
        assertThat(privatePost.getViewCount()).isEqualTo(1);
    }

    @Test
    void privateImageUsesSameOwnerOrAdminRule() {
        String imageUrl = "/api/board/images/test.png";
        BoardPost privatePost = post(10L, BoardType.SUPPORT, 7L, false);
        when(boardPostRepository.findByImageUrl(imageUrl)).thenReturn(Optional.of(privatePost));

        var access = boardPostService.getImageAccess(imageUrl);

        assertThat(access.privatePost()).isTrue();
        assertThatThrownBy(() -> boardPostService.requireImageAccess(access, viewer(8L, false)))
                .isInstanceOf(AuthException.class);
    }

    private BoardPost post(Long id, BoardType type, Long authorId, boolean notice) {
        BoardPost post = new BoardPost();
        post.setId(id);
        post.setType(type);
        post.setTitle("문의 제목");
        post.setContent("문의 내용");
        post.setAuthorId(authorId);
        post.setAuthorName("tracker");
        post.setNotice(notice);
        post.setImageUrls(List.of());
        return post;
    }

    private AuthUser linkedUser(Long id, boolean verified) {
        AuthUser user = new AuthUser();
        user.setId(id);
        user.setOuid("ouid-" + id);
        user.setNicknameVerified(verified);
        return user;
    }

    private AuthUserResponse viewer(Long id, boolean admin) {
        return new AuthUserResponse(
                id,
                "member@satrk.gg",
                "user%03d".formatted(id),
                "tracker",
                "tracker",
                "ouid-" + id,
                false,
                admin,
                false
        );
    }
}
