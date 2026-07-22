package com.sa.trk.board.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.repository.AuthUserRepository;
import com.sa.trk.auth.service.AuthException;
import com.sa.trk.board.dto.BoardPostCreateRequest;
import com.sa.trk.board.dto.BoardPostResponseDto;
import com.sa.trk.board.entity.BoardPost;
import com.sa.trk.board.entity.BoardType;
import com.sa.trk.board.repository.BoardPostRepository;

@Service
public class BoardPostService {

    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_CONTENT_LENGTH = 5_000;

    private final BoardPostRepository boardPostRepository;
    private final AuthUserRepository authUserRepository;

    public BoardPostService(
            BoardPostRepository boardPostRepository,
            AuthUserRepository authUserRepository) {
        this.boardPostRepository = boardPostRepository;
        this.authUserRepository = authUserRepository;
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
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BoardPostResponseDto getPost(Long id, AuthUserResponse viewer) {
        BoardPost post = findPost(id);
        requireReadAccess(post, viewer);
        post.setViewCount(post.getViewCount() + 1);
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public List<BoardPostResponseDto> getAllPosts() {
        return boardPostRepository.findAllByOrderByNoticeDescCreatedAtDesc().stream()
                .map(this::toResponse)
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
        post.setType(parseType(request.type()));
        post.setTitle(requireText(request.title(), "제목", MAX_TITLE_LENGTH));
        post.setContent(requireText(request.content(), "내용", MAX_CONTENT_LENGTH));
        post.setAuthorId(author.id());
        post.setAuthorName(firstNonBlank(author.suddenNickname(), author.displayName(), "회원"));
        post.setViewCount(0);
        post.setNotice(notice);
        post.setImageUrls(imageUrls == null ? List.of() : List.copyOf(imageUrls));

        return toResponse(boardPostRepository.save(post));
    }

    @Transactional
    public List<String> deletePost(Long id) {
        BoardPost post = findPost(id);
        List<String> imageUrls = List.copyOf(post.getImageUrls());
        boardPostRepository.delete(post);
        return imageUrls;
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

    private BoardPostResponseDto toResponse(BoardPost post) {
        AuthUser author = authUserRepository.findById(post.getAuthorId()).orElse(null);
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
                List.copyOf(post.getImageUrls()),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    public record BoardImageAccess(boolean privatePost, Long authorId) {
    }
}
