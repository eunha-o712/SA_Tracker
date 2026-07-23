package com.sa.trk.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.sa.trk.auth.entity.AccountStatus;
import com.sa.trk.auth.repository.AuthSessionRepository;
import com.sa.trk.auth.repository.AuthUserRepository;
import com.sa.trk.auth.service.AuthException;
import com.sa.trk.board.dto.BoardPostCreateRequest;
import com.sa.trk.board.dto.OuidDisputeResolutionRequest;
import com.sa.trk.board.entity.BoardPost;
import com.sa.trk.board.entity.BoardType;
import com.sa.trk.board.entity.SupportCategory;
import com.sa.trk.board.entity.SupportStatus;
import com.sa.trk.board.repository.BoardPostRepository;
import com.sa.trk.board.repository.SupportActionLogRepository;
import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserBasicDto;

class BoardPostServiceTests {

    @Mock
    private BoardPostRepository boardPostRepository;

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Mock
    private SupportActionLogRepository supportActionLogRepository;

    @Mock
    private NexonApiClient nexonApiClient;

    private BoardPostService boardPostService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        boardPostService = new BoardPostService(
                boardPostRepository,
                authUserRepository,
                authSessionRepository,
                supportActionLogRepository,
                nexonApiClient
        );
        when(supportActionLogRepository.findByPostIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
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

    @Test
    void createsPrivateOuidDisputeOnlyForAlreadyLinkedOuid() {
        AuthUser currentOwner = linkedUser(99L, false);
        OuidResponseDto ouidResponse = new OuidResponseDto();
        ouidResponse.setOuid("ouid-99");
        UserBasicDto basic = new UserBasicDto();
        basic.setUser_name("tracker");
        when(nexonApiClient.getOuid("tracker")).thenReturn(ouidResponse);
        when(nexonApiClient.getUserBasic("ouid-99")).thenReturn(basic);
        when(authUserRepository.findByOuid("ouid-99")).thenReturn(Optional.of(currentOwner));
        when(boardPostRepository.save(any(BoardPost.class))).thenAnswer(invocation -> {
            BoardPost saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });
        when(authUserRepository.findById(7L)).thenReturn(Optional.of(linkedUser(7L, false)));

        var response = boardPostService.createPost(
                new BoardPostCreateRequest("SUPPORT", "OUID 중복 문의", "본인 계정입니다.", "OUID_DISPUTE", "tracker"),
                viewer(7L, false),
                List.of("/api/board/images/evidence.png")
        );

        assertThat(response.supportCategory()).isEqualTo("OUID_DISPUTE");
        assertThat(response.supportStatus()).isEqualTo("OPEN");
        assertThat(response.claimedSuddenNickname()).isEqualTo("tracker");
        verify(supportActionLogRepository).save(any());
    }

    @Test
    void adminTransfersDisputedOuidAndSuspendsPreviousOwner() {
        BoardPost dispute = post(20L, BoardType.SUPPORT, 7L, false);
        dispute.setSupportCategory(SupportCategory.OUID_DISPUTE);
        dispute.setSupportStatus(SupportStatus.OPEN);
        dispute.setClaimedSuddenNickname("tracker");
        dispute.setClaimedOuid("ouid-99");
        dispute.setClaimedOwnerId(99L);

        AuthUser claimant = linkedUser(7L, false);
        claimant.setOuid(null);
        claimant.setSuddenNickname(null);
        claimant.setLoginId("user007");
        claimant.setDisplayName("user007");
        AuthUser previousOwner = linkedUser(99L, true);
        previousOwner.setLoginId("user099");
        previousOwner.setDisplayName("tracker");
        AuthUser admin = linkedUser(1L, true);
        admin.setDisplayName("operator");

        UserBasicDto basic = new UserBasicDto();
        basic.setUser_name("tracker");
        when(boardPostRepository.findById(20L)).thenReturn(Optional.of(dispute));
        when(authUserRepository.findById(7L)).thenReturn(Optional.of(claimant));
        when(authUserRepository.findById(99L)).thenReturn(Optional.of(previousOwner));
        when(authUserRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(authUserRepository.findByOuid("ouid-99")).thenReturn(Optional.of(previousOwner));
        when(authUserRepository.saveAndFlush(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nexonApiClient.getUserBasic("ouid-99")).thenReturn(basic);

        var response = boardPostService.resolveOuidDispute(
                20L,
                new OuidDisputeResolutionRequest(
                        "TRANSFER_TO_CLAIMANT",
                        "SUSPEND",
                        true,
                        "본인 확인 후 OUID 연결을 이전했습니다.",
                        "제출된 로그인 증빙과 닉네임을 확인했습니다."
                ),
                viewer(1L, true)
        );

        assertThat(claimant.getOuid()).isEqualTo("ouid-99");
        assertThat(claimant.getNicknameVerified()).isTrue();
        assertThat(previousOwner.getOuid()).isNull();
        assertThat(previousOwner.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(response.supportStatus()).isEqualTo("RESOLVED");
        assertThat(response.resolutionAction()).isEqualTo("TRANSFER_TO_CLAIMANT");
        verify(authSessionRepository).deleteByUser(previousOwner);
        verify(supportActionLogRepository).save(any());
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
        user.setLoginId("user%03d".formatted(id));
        user.setDisplayName("tracker");
        user.setOuid("ouid-" + id);
        user.setNicknameVerified(verified);
        user.setAccountStatus(AccountStatus.ACTIVE);
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
