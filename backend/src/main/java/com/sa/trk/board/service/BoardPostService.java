package com.sa.trk.board.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.dto.AccountStatusUpdateRequest;
import com.sa.trk.auth.entity.AccountStatus;
import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.repository.AuthSessionRepository;
import com.sa.trk.auth.repository.AuthUserRepository;
import com.sa.trk.auth.service.AuthException;
import com.sa.trk.board.dto.BoardPostCreateRequest;
import com.sa.trk.board.dto.BoardPostResponseDto;
import com.sa.trk.board.dto.OuidDisputeResolutionRequest;
import com.sa.trk.board.dto.SupportActionLogResponse;
import com.sa.trk.board.dto.SupportAdminUpdateRequest;
import com.sa.trk.board.entity.AccountSanctionAction;
import com.sa.trk.board.entity.BoardPost;
import com.sa.trk.board.entity.BoardType;
import com.sa.trk.board.entity.OuidDisputeResolution;
import com.sa.trk.board.entity.SupportActionLog;
import com.sa.trk.board.entity.SupportCategory;
import com.sa.trk.board.entity.SupportStatus;
import com.sa.trk.board.repository.BoardPostRepository;
import com.sa.trk.board.repository.SupportActionLogRepository;
import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserBasicDto;

@Service
public class BoardPostService {

    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_CONTENT_LENGTH = 5_000;
    private static final int MAX_RESPONSE_LENGTH = 5_000;
    private static final int MAX_REASON_LENGTH = 1_000;

    private final BoardPostRepository boardPostRepository;
    private final AuthUserRepository authUserRepository;
    private final AuthSessionRepository authSessionRepository;
    private final SupportActionLogRepository supportActionLogRepository;
    private final NexonApiClient nexonApiClient;

    public BoardPostService(
            BoardPostRepository boardPostRepository,
            AuthUserRepository authUserRepository,
            AuthSessionRepository authSessionRepository,
            SupportActionLogRepository supportActionLogRepository,
            NexonApiClient nexonApiClient) {
        this.boardPostRepository = boardPostRepository;
        this.authUserRepository = authUserRepository;
        this.authSessionRepository = authSessionRepository;
        this.supportActionLogRepository = supportActionLogRepository;
        this.nexonApiClient = nexonApiClient;
    }

    @Transactional(readOnly = true)
    public List<BoardPostResponseDto> getPosts(String rawType, AuthUserResponse viewer) {
        BoardType type = parseType(rawType);
        List<BoardPost> posts;
        if (type == BoardType.SUPPORT) {
            requireLoggedIn(viewer);
            posts = viewer.admin()
                    ? boardPostRepository.findByTypeOrNoticeTrueOrderByNoticeDescCreatedAtDesc(type)
                    : boardPostRepository.findVisiblePosts(type, viewer.id());
        } else {
            posts = boardPostRepository.findByTypeOrNoticeTrueOrderByNoticeDescCreatedAtDesc(type);
        }

        return posts.stream()
                .map(post -> toResponse(post, viewer != null && viewer.admin()))
                .toList();
    }

    @Transactional
    public BoardPostResponseDto getPost(Long id, AuthUserResponse viewer) {
        BoardPost post = findPost(id);
        requireReadAccess(post, viewer);
        post.setViewCount(post.getViewCount() + 1);
        return toResponse(post, viewer != null && viewer.admin());
    }

    @Transactional(readOnly = true)
    public List<BoardPostResponseDto> getAllPosts() {
        return boardPostRepository.findAllByOrderByNoticeDescCreatedAtDesc().stream()
                .map(post -> toResponse(post, true))
                .toList();
    }

    @Transactional
    public BoardPostResponseDto createPost(BoardPostCreateRequest request, AuthUserResponse author) {
        return createPost(request, author, List.of(), false);
    }

    @Transactional
    public BoardPostResponseDto createPost(
            BoardPostCreateRequest request,
            AuthUserResponse author,
            List<String> imageUrls) {
        return createPost(request, author, imageUrls, false);
    }

    @Transactional
    public BoardPostResponseDto createPost(
            BoardPostCreateRequest request,
            AuthUserResponse author,
            List<String> imageUrls,
            boolean notice) {
        if (request == null) {
            throw new IllegalArgumentException("게시글 내용을 입력해주세요.");
        }

        BoardPost post = new BoardPost();
        BoardType type = parseType(request.type());
        post.setType(type);
        post.setTitle(requireText(request.title(), "제목", MAX_TITLE_LENGTH));
        post.setContent(requireText(request.content(), "내용", MAX_CONTENT_LENGTH));
        post.setAuthorId(author.id());
        post.setAuthorName(firstNonBlank(author.suddenNickname(), author.displayName(), "회원"));
        post.setViewCount(0);
        post.setNotice(notice);
        post.setImageUrls(imageUrls == null ? List.of() : List.copyOf(imageUrls));

        if (type == BoardType.SUPPORT && !notice) {
            SupportCategory supportCategory = parseSupportCategory(request.supportCategory());
            post.setSupportCategory(supportCategory);
            post.setSupportStatus(SupportStatus.OPEN);
            if (supportCategory == SupportCategory.OUID_DISPUTE) {
                if (post.getImageUrls().isEmpty()) {
                    throw new IllegalArgumentException("OUID 분쟁 문의에는 로그인 상태를 확인할 수 있는 증빙 이미지를 한 장 이상 첨부해 주세요.");
                }
                configureOuidDispute(post, author, request.suddenNickname());
            }
        }

        BoardPost savedPost = boardPostRepository.save(post);
        if (type == BoardType.SUPPORT && !notice) {
            saveActionLog(
                    savedPost.getId(),
                    author.id(),
                    savedPost.getSupportCategory() == SupportCategory.OUID_DISPUTE
                            ? "OUID_DISPUTE_OPENED"
                            : "SUPPORT_OPENED",
                    "비공개 문의가 접수되었습니다."
            );
        }
        return toResponse(savedPost, author.admin());
    }

    @Transactional
    public List<String> deletePost(Long id) {
        BoardPost post = findPost(id);
        List<String> imageUrls = List.copyOf(post.getImageUrls());
        supportActionLogRepository.deleteByPostId(id);
        boardPostRepository.delete(post);
        return imageUrls;
    }

    @Transactional
    public BoardPostResponseDto updateSupportPost(
            Long id,
            SupportAdminUpdateRequest request,
            AuthUserResponse admin) {
        BoardPost post = requireSupportPost(id);
        SupportStatus status = parseSupportStatus(request == null ? null : request.status());
        String response = optionalText(request == null ? null : request.response(), "답변", MAX_RESPONSE_LENGTH);

        if (supportCategory(post) == SupportCategory.OUID_DISPUTE
                && (status == SupportStatus.RESOLVED || status == SupportStatus.REJECTED)
                && post.getResolutionAction() == null) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    "DISPUTE_RESOLUTION_REQUIRED",
                    "OUID 분쟁은 연결 처리 방법을 선택해 종결해 주세요."
            );
        }

        post.setSupportStatus(status);
        post.setAdminResponse(response);
        if (status == SupportStatus.RESOLVED || status == SupportStatus.REJECTED) {
            post.setHandledById(admin.id());
            post.setHandledAt(Instant.now());
        }
        saveActionLog(
                post.getId(),
                admin.id(),
                "SUPPORT_STATUS_UPDATED",
                status.name() + (response == null ? "" : " · 관리자 답변 등록")
        );
        return toResponse(post, true);
    }

    @Transactional
    public BoardPostResponseDto resolveOuidDispute(
            Long id,
            OuidDisputeResolutionRequest request,
            AuthUserResponse admin) {
        BoardPost post = requireSupportPost(id);
        if (supportCategory(post) != SupportCategory.OUID_DISPUTE) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "NOT_OUID_DISPUTE",
                    "OUID 분쟁 문의가 아닙니다."
            );
        }
        if (supportStatus(post) == SupportStatus.RESOLVED || supportStatus(post) == SupportStatus.REJECTED) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    "DISPUTE_ALREADY_CLOSED",
                    "이미 종결된 OUID 분쟁입니다."
            );
        }

        OuidDisputeResolution resolution = parseResolution(request == null ? null : request.resolution());
        AccountSanctionAction accountAction = parseAccountAction(request == null ? null : request.accountAction());
        boolean verifyClaimant = request != null && Boolean.TRUE.equals(request.verifyClaimant());
        String response = requireText(request == null ? null : request.response(), "처리 결과 안내", MAX_RESPONSE_LENGTH);
        String reason = requireReason(request == null ? null : request.reason());

        if ((resolution == OuidDisputeResolution.KEEP_EXISTING || resolution == OuidDisputeResolution.REJECT)
                && accountAction != AccountSanctionAction.KEEP
                && accountAction != AccountSanctionAction.ACTIVATE) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ACCOUNT_ACTION",
                    "기존 연결을 유지하거나 반려할 때는 기존 계정을 제재할 수 없습니다."
            );
        }
        if (verifyClaimant && resolution != OuidDisputeResolution.TRANSFER_TO_CLAIMANT) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CLAIMANT_VERIFICATION",
                    "신고자 수동 확인은 OUID를 신고자에게 이전할 때만 선택할 수 있습니다."
            );
        }

        AuthUser claimant = authUserRepository.findById(post.getAuthorId())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "문의 작성자를 찾을 수 없습니다."));
        AuthUser currentOwner = authUserRepository.findByOuid(post.getClaimedOuid()).orElse(null);
        if (currentOwner != null
                && Objects.equals(currentOwner.getId(), claimant.getId())
                && resolution != OuidDisputeResolution.TRANSFER_TO_CLAIMANT) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    "DISPUTE_OWNER_CHANGED",
                    "현재 OUID가 이미 문의 작성자에게 연결되어 있습니다. 정보를 다시 확인해 주세요."
            );
        }
        if ((accountAction == AccountSanctionAction.SUSPEND || accountAction == AccountSanctionAction.BAN)
                && (currentOwner == null || Objects.equals(currentOwner.getId(), claimant.getId()))) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    "CLAIMED_OWNER_NOT_FOUND",
                    "제재할 기존 연결 계정을 찾을 수 없습니다."
            );
        }

        if (resolution == OuidDisputeResolution.TRANSFER_TO_CLAIMANT) {
            if (currentOwner != null && !Objects.equals(currentOwner.getId(), claimant.getId())) {
                unlinkSuddenAccount(currentOwner);
                applyAccountAction(currentOwner, accountAction, reason, admin.id());
                authUserRepository.saveAndFlush(currentOwner);
            }
            if (claimant.getOuid() != null && !claimant.getOuid().isBlank()
                    && !claimant.getOuid().equals(post.getClaimedOuid())) {
                unlinkSuddenAccount(claimant);
                authUserRepository.saveAndFlush(claimant);
            }
            linkClaimedAccount(claimant, post, verifyClaimant);
            authUserRepository.saveAndFlush(claimant);
        } else if (resolution == OuidDisputeResolution.UNLINK_EXISTING) {
            if (currentOwner != null) {
                unlinkSuddenAccount(currentOwner);
                applyAccountAction(currentOwner, accountAction, reason, admin.id());
                authUserRepository.saveAndFlush(currentOwner);
            } else if (accountAction != AccountSanctionAction.KEEP) {
                throw new AuthException(
                        HttpStatus.CONFLICT,
                        "CLAIMED_OWNER_NOT_FOUND",
                        "현재 OUID 연결 계정을 찾을 수 없어 계정 상태를 변경하지 않았습니다."
                );
            }
        } else if (accountAction == AccountSanctionAction.ACTIVATE && currentOwner != null) {
            applyAccountAction(currentOwner, accountAction, reason, admin.id());
            authUserRepository.saveAndFlush(currentOwner);
        }

        post.setSupportStatus(resolution == OuidDisputeResolution.REJECT
                ? SupportStatus.REJECTED
                : SupportStatus.RESOLVED);
        post.setResolutionAction(resolution);
        post.setAccountSanctionAction(accountAction);
        post.setClaimantVerified(verifyClaimant);
        post.setAdminResponse(response);
        post.setHandledById(admin.id());
        post.setHandledAt(Instant.now());

        saveActionLog(
                post.getId(),
                admin.id(),
                "OUID_DISPUTE_RESOLVED",
                resolution.name() + " · " + accountAction.name() + " · " + reason
        );
        return toResponse(post, true);
    }

    @Transactional
    public BoardPostResponseDto updateClaimedOwnerStatus(
            Long id,
            AccountStatusUpdateRequest request,
            AuthUserResponse admin) {
        BoardPost post = requireSupportPost(id);
        if (supportCategory(post) != SupportCategory.OUID_DISPUTE || post.getClaimedOwnerId() == null) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "CLAIMED_OWNER_NOT_FOUND", "분쟁 대상 계정을 찾을 수 없습니다.");
        }
        if (request == null) {
            throw new IllegalArgumentException("계정 상태 정보를 확인해 주세요.");
        }

        AccountStatus status;
        try {
            status = AccountStatus.valueOf(String.valueOf(request.status()).trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("계정 상태를 확인해 주세요.");
        }
        if (Objects.equals(post.getClaimedOwnerId(), admin.id()) && status != AccountStatus.ACTIVE) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "ADMIN_SELF_SANCTION", "현재 로그인한 관리자 계정은 정지할 수 없습니다.");
        }

        AuthUser owner = authUserRepository.findById(post.getClaimedOwnerId())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "분쟁 대상 계정을 찾을 수 없습니다."));
        String reason = status == AccountStatus.ACTIVE ? "관리자가 계정을 정상 상태로 복구했습니다." : requireReason(request.reason());
        AccountSanctionAction action = switch (status) {
            case ACTIVE -> AccountSanctionAction.ACTIVATE;
            case SUSPENDED -> AccountSanctionAction.SUSPEND;
            case BANNED -> AccountSanctionAction.BAN;
        };
        applyAccountAction(owner, action, reason, admin.id());
        authUserRepository.saveAndFlush(owner);
        saveActionLog(
                post.getId(),
                admin.id(),
                "CLAIMED_OWNER_STATUS_UPDATED",
                status.name() + " · " + reason
        );
        return toResponse(post, true);
    }

    @Transactional(readOnly = true)
    public BoardImageAccess getImageAccess(String imageUrl) {
        BoardPost post = boardPostRepository.findByImageUrl(imageUrl)
                .orElseThrow(() -> new AuthException(
                        HttpStatus.NOT_FOUND,
                        "BOARD_IMAGE_NOT_FOUND",
                        "이미지를 찾을 수 없습니다."
                ));
        return new BoardImageAccess(isPrivate(post), post.getAuthorId());
    }

    public void requireImageAccess(BoardImageAccess access, AuthUserResponse viewer) {
        if (access.privatePost()) {
            requireOwnerOrAdmin(access.authorId(), viewer);
        }
    }

    private BoardType parseType(String rawType) {
        try {
            return BoardType.valueOf(String.valueOf(rawType).trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("게시판 종류를 선택해주세요.");
        }
    }

    private SupportCategory parseSupportCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return SupportCategory.GENERAL;
        }
        try {
            return SupportCategory.valueOf(rawCategory.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("문의 유형을 확인해 주세요.");
        }
    }

    private SupportStatus parseSupportStatus(String rawStatus) {
        try {
            return SupportStatus.valueOf(String.valueOf(rawStatus).trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("문의 처리 상태를 확인해 주세요.");
        }
    }

    private OuidDisputeResolution parseResolution(String rawResolution) {
        try {
            return OuidDisputeResolution.valueOf(String.valueOf(rawResolution).trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("OUID 분쟁 처리 방법을 확인해 주세요.");
        }
    }

    private AccountSanctionAction parseAccountAction(String rawAction) {
        if (rawAction == null || rawAction.isBlank()) {
            return AccountSanctionAction.KEEP;
        }
        try {
            return AccountSanctionAction.valueOf(rawAction.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("계정 처리 방법을 확인해 주세요.");
        }
    }

    private String requireText(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + "을 입력해주세요.");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + "은 " + maxLength + "자 이하로 입력해주세요.");
        }
        return normalized;
    }

    private String optionalText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) return null;
        return requireText(value, label, maxLength);
    }

    private String requireReason(String value) {
        String reason = requireText(value, "처리 사유", MAX_REASON_LENGTH);
        if (reason.length() < 5) {
            throw new IllegalArgumentException("처리 사유는 5자 이상 입력해 주세요.");
        }
        return reason;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "회원";
    }

    private BoardPost findPost(Long id) {
        if (id == null || id < 1) {
            throw new AuthException(HttpStatus.NOT_FOUND, "BOARD_POST_NOT_FOUND", "게시글을 찾을 수 없습니다.");
        }
        return boardPostRepository.findById(id)
                .orElseThrow(() -> new AuthException(
                        HttpStatus.NOT_FOUND,
                        "BOARD_POST_NOT_FOUND",
                        "게시글을 찾을 수 없습니다."
                ));
    }

    private BoardPost requireSupportPost(Long id) {
        BoardPost post = findPost(id);
        if (post.getType() != BoardType.SUPPORT || post.isNotice()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "NOT_SUPPORT_POST", "비공개 문의글이 아닙니다.");
        }
        return post;
    }

    private void configureOuidDispute(
            BoardPost post,
            AuthUserResponse author,
            String rawNickname) {
        String nickname = normalizeSuddenNickname(rawNickname);
        String ouid = resolveOuid(nickname);
        AuthUser currentOwner = authUserRepository.findByOuid(ouid)
                .orElseThrow(() -> new AuthException(
                        HttpStatus.CONFLICT,
                        "OUID_NOT_TAKEN",
                        "현재 다른 회원에게 연결된 OUID가 아닙니다. 마이페이지에서 계정 연결을 다시 시도해 주세요."
                ));
        if (Objects.equals(currentOwner.getId(), author.id())) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    "OUID_ALREADY_OWNED",
                    "이미 본인의 SA-TRACKER 계정에 연결된 서든 계정입니다."
            );
        }
        if (boardPostRepository.existsByAuthorIdAndSupportCategoryAndClaimedOuidAndSupportStatusIn(
                author.id(),
                SupportCategory.OUID_DISPUTE,
                ouid,
                List.of(SupportStatus.OPEN, SupportStatus.IN_PROGRESS))) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    "OUID_DISPUTE_ALREADY_OPEN",
                    "같은 서든 계정에 대해 처리 중인 OUID 분쟁 문의가 있습니다."
            );
        }
        post.setClaimedSuddenNickname(resolveCanonicalNickname(ouid, nickname));
        post.setClaimedOuid(ouid);
        post.setClaimedOwnerId(currentOwner.getId());
    }

    private String normalizeSuddenNickname(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() < 2 || normalized.length() > 20) {
            throw new IllegalArgumentException("연결하려는 서든 닉네임을 2~20자로 입력해 주세요.");
        }
        return normalized;
    }

    private String resolveOuid(String nickname) {
        try {
            OuidResponseDto response = nexonApiClient.getOuid(nickname);
            if (response == null || response.getOuid() == null || response.getOuid().isBlank()) {
                throw new IllegalArgumentException("서든 닉네임을 찾을 수 없습니다.");
            }
            return response.getOuid().trim();
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("서든 닉네임을 찾을 수 없습니다.");
        }
    }

    private String resolveCanonicalNickname(String ouid, String fallback) {
        try {
            UserBasicDto basic = nexonApiClient.getUserBasic(ouid);
            if (basic != null && basic.getUser_name() != null && !basic.getUser_name().isBlank()) {
                return basic.getUser_name().trim();
            }
        } catch (RuntimeException ignored) {
            // The submitted nickname remains usable if the secondary Nexon lookup fails.
        }
        return fallback;
    }

    private void linkClaimedAccount(AuthUser claimant, BoardPost post, boolean verified) {
        claimant.setOuid(post.getClaimedOuid());
        claimant.setSuddenNickname(resolveCanonicalNickname(post.getClaimedOuid(), post.getClaimedSuddenNickname()));
        claimant.setDisplayName(claimant.getSuddenNickname());
        claimant.setClanNone(false);
        claimant.setNicknameVerified(verified);
        claimant.setVerifiedAt(verified ? Instant.now() : null);
    }

    private void unlinkSuddenAccount(AuthUser user) {
        user.setOuid(null);
        user.setSuddenNickname(null);
        user.setDisplayName(user.getLoginId());
        user.setClanNone(false);
        user.setNicknameVerified(false);
        user.setVerifiedAt(null);
    }

    private void applyAccountAction(
            AuthUser user,
            AccountSanctionAction action,
            String reason,
            Long adminId) {
        if (action == AccountSanctionAction.KEEP) return;
        if (action == AccountSanctionAction.ACTIVATE) {
            user.setAccountStatus(AccountStatus.ACTIVE);
            user.setSanctionReason(null);
            user.setSanctionedAt(null);
            user.setSanctionedById(null);
            return;
        }

        user.setAccountStatus(action == AccountSanctionAction.SUSPEND
                ? AccountStatus.SUSPENDED
                : AccountStatus.BANNED);
        user.setSanctionReason(reason);
        user.setSanctionedAt(Instant.now());
        user.setSanctionedById(adminId);
        authSessionRepository.deleteByUser(user);
    }

    private void saveActionLog(
            Long postId,
            Long actorId,
            String action,
            String note) {
        SupportActionLog log = new SupportActionLog();
        log.setPostId(postId);
        log.setActorId(actorId);
        log.setAction(action);
        log.setNote(note == null || note.length() <= MAX_REASON_LENGTH
                ? note
                : note.substring(0, MAX_REASON_LENGTH));
        supportActionLogRepository.save(log);
    }

    private void requireReadAccess(BoardPost post, AuthUserResponse viewer) {
        if (isPrivate(post)) {
            requireOwnerOrAdmin(post.getAuthorId(), viewer);
        }
    }

    private void requireOwnerOrAdmin(Long authorId, AuthUserResponse viewer) {
        requireLoggedIn(viewer);
        if (!viewer.admin() && !Objects.equals(authorId, viewer.id())) {
            throw new AuthException(
                    HttpStatus.FORBIDDEN,
                    "PRIVATE_POST_FORBIDDEN",
                    "작성자와 관리자만 문의글을 확인할 수 있습니다."
            );
        }
    }

    private void requireLoggedIn(AuthUserResponse viewer) {
        if (viewer == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
    }

    private boolean isPrivate(BoardPost post) {
        return post.getType() == BoardType.SUPPORT && !post.isNotice();
    }

    private BoardPostResponseDto toResponse(BoardPost post, boolean includeAdminDetails) {
        AuthUser author = authUserRepository.findById(post.getAuthorId()).orElse(null);
        AuthUser claimedOwner = includeAdminDetails && post.getClaimedOwnerId() != null
                ? authUserRepository.findById(post.getClaimedOwnerId()).orElse(null)
                : null;
        AuthUser handler = post.getHandledById() == null
                ? null
                : authUserRepository.findById(post.getHandledById()).orElse(null);
        List<SupportActionLogResponse> actionLogs = post.getType() == BoardType.SUPPORT && post.getId() != null
                ? supportActionLogRepository.findByPostIdOrderByCreatedAtAsc(post.getId()).stream()
                        .map(log -> {
                            AuthUser actor = authUserRepository.findById(log.getActorId()).orElse(null);
                            return new SupportActionLogResponse(
                                    log.getId(),
                                    log.getAction(),
                                    includeAdminDetails ? log.getNote() : publicActionNote(log.getAction()),
                                    includeAdminDetails ? log.getActorId() : null,
                                    actorName(actor),
                                    log.getCreatedAt()
                            );
                        })
                        .toList()
                : List.of();
        return new BoardPostResponseDto(
                post.getId(),
                post.getType().name(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorId(),
                post.getAuthorName(),
                post.getViewCount(),
                post.isNotice(),
                isPrivate(post),
                author != null && author.getOuid() != null && !author.getOuid().isBlank(),
                author != null && Boolean.TRUE.equals(author.getNicknameVerified()),
                accountStatus(author).name(),
                post.getType() == BoardType.SUPPORT && !post.isNotice() ? supportCategory(post).name() : null,
                post.getType() == BoardType.SUPPORT && !post.isNotice() ? supportStatus(post).name() : null,
                post.getClaimedSuddenNickname(),
                includeAdminDetails ? post.getClaimedOwnerId() : null,
                includeAdminDetails ? actorName(claimedOwner) : null,
                includeAdminDetails && claimedOwner != null ? accountStatus(claimedOwner).name() : null,
                post.getAdminResponse(),
                post.getResolutionAction() == null ? null : post.getResolutionAction().name(),
                post.getAccountSanctionAction() == null ? null : post.getAccountSanctionAction().name(),
                Boolean.TRUE.equals(post.getClaimantVerified()),
                includeAdminDetails ? post.getHandledById() : null,
                actorName(handler),
                post.getHandledAt(),
                actionLogs,
                List.copyOf(post.getImageUrls()),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private SupportCategory supportCategory(BoardPost post) {
        return post.getSupportCategory() == null ? SupportCategory.GENERAL : post.getSupportCategory();
    }

    private SupportStatus supportStatus(BoardPost post) {
        return post.getSupportStatus() == null ? SupportStatus.OPEN : post.getSupportStatus();
    }

    private AccountStatus accountStatus(AuthUser user) {
        return user == null || user.getAccountStatus() == null ? AccountStatus.ACTIVE : user.getAccountStatus();
    }

    private String actorName(AuthUser user) {
        return user == null ? null : firstNonBlank(user.getSuddenNickname(), user.getDisplayName(), user.getLoginId(), "회원");
    }

    private String publicActionNote(String action) {
        return switch (action) {
            case "OUID_DISPUTE_OPENED" -> "OUID 연결 분쟁이 접수되었습니다.";
            case "OUID_DISPUTE_RESOLVED" -> "관리자가 OUID 연결 분쟁 처리를 완료했습니다.";
            case "SUPPORT_STATUS_UPDATED" -> "관리자가 문의 상태 또는 답변을 변경했습니다.";
            case "CLAIMED_OWNER_STATUS_UPDATED" -> "관리자가 분쟁 대상 계정 상태를 변경했습니다.";
            default -> "비공개 문의가 접수되었습니다.";
        };
    }

    public record BoardImageAccess(boolean privatePost, Long authorId) {
    }
}
