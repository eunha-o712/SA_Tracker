package com.sa.trk.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.trk.auth.dto.AuthLoginRequest;
import com.sa.trk.auth.dto.AuthRegisterRequest;
import com.sa.trk.auth.dto.AuthResponse;
import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.dto.ClanStatusUpdateRequest;
import com.sa.trk.auth.dto.PasswordResetConfirmRequest;
import com.sa.trk.auth.dto.PasswordResetRequest;
import com.sa.trk.auth.dto.PasswordResetRequestResponse;
import com.sa.trk.auth.dto.SuddenAccountLinkRequest;
import com.sa.trk.auth.entity.AuthSession;
import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.entity.PasswordResetToken;
import com.sa.trk.auth.repository.AuthSessionRepository;
import com.sa.trk.auth.repository.AuthUserRepository;
import com.sa.trk.auth.repository.PasswordResetTokenRepository;
import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserBasicDto;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Duration SESSION_DURATION = Duration.ofDays(30);
    private static final Duration PASSWORD_RESET_DURATION = Duration.ofMinutes(15);

    private final AuthUserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetEmailService passwordResetEmailService;
    private final PasswordHasher passwordHasher;
    private final NexonApiClient nexonApiClient;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            AuthUserRepository userRepository,
            AuthSessionRepository sessionRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetEmailService passwordResetEmailService,
            PasswordHasher passwordHasher,
            NexonApiClient nexonApiClient) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetEmailService = passwordResetEmailService;
        this.passwordHasher = passwordHasher;
        this.nexonApiClient = nexonApiClient;
    }

    @Transactional
    public AuthResponse register(AuthRegisterRequest request) {
        String email = normalizeEmail(request == null ? null : request.resolvedEmail());
        String password = request == null ? null : request.password();
        validatePassword(password);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AuthException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "이미 사용 중인 이메일입니다.");
        }

        String internalId = nextInternalUserId();
        String salt = passwordHasher.newSalt();

        AuthUser user = new AuthUser();
        user.setEmail(email);
        user.setLoginId(internalId);
        user.setDisplayName(internalId);
        user.setSuddenNickname(null);
        user.setOuid(null);
        user.setClanNone(false);
        user.setNicknameVerified(false);
        user.setVerifiedAt(null);
        user.setAdmin(false);
        user.setPasswordSalt(salt);
        user.setPasswordHash(passwordHasher.hash(password, salt));
        user.setCreatedAt(Instant.now());

        return createSession(userRepository.save(user));
    }

    @Transactional
    public AuthResponse login(AuthLoginRequest request) {
        String email = normalizeEmail(request == null ? null : request.resolvedEmail());
        String password = request == null ? null : request.password();

        AuthUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(this::invalidCredentials);
        if (password == null || !passwordHasher.matches(password, user.getPasswordSalt(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return createSession(user);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse currentUser(String rawToken) {
        return toUserResponse(findValidSession(rawToken).getUser());
    }

    @Transactional(readOnly = true)
    public AuthUserResponse requireAdmin(String rawToken) {
        AuthUserResponse user = currentUser(rawToken);
        if (!user.admin()) {
            throw new AuthException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "관리자 권한이 필요합니다.");
        }
        return user;
    }

    @Transactional
    public AuthUserResponse setManualVerification(String rawToken, Long userId, Boolean verified) {
        requireAdmin(rawToken);
        if (userId == null || userId < 1 || verified == null) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_REQUEST", "인증 처리 정보를 확인해주세요.");
        }

        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "회원을 찾을 수 없습니다."));
        if (verified && (user.getOuid() == null || user.getOuid().isBlank())) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    "OUID_NOT_LINKED",
                    "OUID가 연결된 회원만 수동 인증할 수 있습니다."
            );
        }

        user.setNicknameVerified(verified);
        user.setVerifiedAt(verified ? Instant.now() : null);
        return toUserResponse(user);
    }

    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(PasswordResetRequest request) {
        String email = normalizeEmail(request == null ? null : request.email());
        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> createPasswordResetToken(email, user))
                .orElseGet(() -> new PasswordResetRequestResponse(
                        "If the email exists, a password reset link has been sent.",
                        null
                ));
    }

    @Transactional
    public AuthResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        String rawToken = request == null ? null : request.token();
        String password = request == null ? null : request.password();
        validatePassword(password);

        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Password reset token is invalid.");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hashToken(rawToken.trim()))
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Password reset token is invalid."));
        Instant now = Instant.now();
        if (resetToken.getUsedAt() != null || !resetToken.getExpiresAt().isAfter(now)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "RESET_TOKEN_EXPIRED", "Password reset token has expired.");
        }

        AuthUser user = resetToken.getUser();
        String salt = passwordHasher.newSalt();
        user.setPasswordSalt(salt);
        user.setPasswordHash(passwordHasher.hash(password, salt));
        resetToken.setUsedAt(now);

        return createSession(user);
    }

    @Transactional
    public AuthUserResponse linkSuddenAccount(String rawToken, SuddenAccountLinkRequest request) {
        AuthUser user = findValidSession(rawToken).getUser();
        String suddenNickname = normalizeSuddenNickname(request == null ? null : request.suddenNickname());
        String ouid = resolveOuid(suddenNickname);

        userRepository.findByOuid(ouid).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(user.getId())) {
                throw new AuthException(
                        HttpStatus.CONFLICT,
                        "OUID_TAKEN",
                        "이미 다른 계정에 연결된 서든 계정입니다.\n본인이 인증 및 연결한 내역이 없다면 문의게시판으로 문의해 주세요."
                );
            }
        });

        if (user.getOuid() != null && !user.getOuid().isBlank() && !user.getOuid().equals(ouid)) {
            throw new AuthException(
                    HttpStatus.CONFLICT,
                    "OUID_ALREADY_LINKED",
                    "이미 서든 계정이 연결되어 있습니다. 다른 계정으로 변경하려면 문의게시판으로 문의해 주세요."
            );
        }

        String canonicalNickname = resolveCanonicalNickname(ouid, suddenNickname);
        user.setOuid(ouid);
        user.setSuddenNickname(canonicalNickname);
        user.setDisplayName(canonicalNickname);
        user.setClanNone(false);
        user.setNicknameVerified(false);
        user.setVerifiedAt(null);
        return toUserResponse(user);
    }

    @Transactional
    public AuthUserResponse updateClanStatus(String rawToken, ClanStatusUpdateRequest request) {
        AuthUser user = findValidSession(rawToken).getUser();
        if (user.getOuid() == null || user.getOuid().isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "OUID_NOT_LINKED", "서든 계정이 연결된 회원만 클랜 상태를 변경할 수 있습니다.");
        }
        if (request == null || request.noClan() == null) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_CLAN_STATUS", "변경할 클랜 상태를 확인해 주세요.");
        }

        user.setClanNone(request.noClan());
        return toUserResponse(user);
    }

    @Transactional
    public AuthUserResponse syncSuddenNickname(String rawToken) {
        AuthUser user = findValidSession(rawToken).getUser();
        if (user.getOuid() == null || user.getOuid().isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "OUID_NOT_LINKED", "서든 계정이 아직 연결되지 않았습니다.");
        }

        String latestNickname = resolveCanonicalNickname(user.getOuid(), user.getSuddenNickname());
        user.setSuddenNickname(latestNickname);
        user.setDisplayName(latestNickname);
        return toUserResponse(user);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        sessionRepository.deleteByTokenHash(hashToken(rawToken));
    }

    private AuthResponse createSession(AuthUser user) {
        String token = newToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(SESSION_DURATION);

        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setTokenHash(hashToken(token));
        session.setCreatedAt(now);
        session.setExpiresAt(expiresAt);
        sessionRepository.save(session);

        return new AuthResponse(token, expiresAt, toUserResponse(user));
    }

    private PasswordResetRequestResponse createPasswordResetToken(String email, AuthUser user) {
        passwordResetTokenRepository.deleteByUserAndUsedAtIsNull(user);

        String rawToken = newToken();
        Instant now = Instant.now();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setCreatedAt(now);
        resetToken.setExpiresAt(now.plus(PASSWORD_RESET_DURATION));
        passwordResetTokenRepository.save(resetToken);

        PasswordResetDelivery delivery = passwordResetEmailService.sendResetLink(email, rawToken);
        return new PasswordResetRequestResponse(
                "If the email exists, a password reset link has been sent.",
                delivery.resetUrl()
        );
    }

    private String newToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private AuthSession findValidSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw unauthorized();
        }
        AuthSession session = sessionRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(this::unauthorized);
        if (!session.getExpiresAt().isAfter(Instant.now())) {
            throw unauthorized();
        }
        return session;
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "올바른 이메일을 입력해 주세요.");
        }
        return normalized;
    }

    private String normalizeSuddenNickname(String suddenNickname) {
        String normalized = suddenNickname == null ? "" : suddenNickname.trim();
        if (normalized.length() < 2 || normalized.length() > 20) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_SUDDEN_NICKNAME", "서든 닉네임은 2~20자로 입력해 주세요.");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "비밀번호는 8~72자로 입력해 주세요.");
        }
    }

    private String nextInternalUserId() {
        long nextNumber = 1L;
        String candidate = formatInternalUserId(nextNumber);
        while (userRepository.existsByLoginIdIgnoreCase(candidate)) {
            nextNumber++;
            candidate = formatInternalUserId(nextNumber);
        }
        return candidate;
    }

    private String formatInternalUserId(long number) {
        return "user" + String.format(Locale.ROOT, "%03d", number);
    }

    private String resolveOuid(String suddenNickname) {
        try {
            OuidResponseDto response = nexonApiClient.getOuid(suddenNickname);
            if (response == null || response.getOuid() == null || response.getOuid().isBlank()) {
                throw new AuthException(HttpStatus.BAD_REQUEST, "SUDDEN_ACCOUNT_NOT_FOUND", "서든 닉네임을 찾을 수 없습니다.");
            }
            return response.getOuid().trim();
        } catch (AuthException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "SUDDEN_ACCOUNT_NOT_FOUND", "서든 닉네임을 찾을 수 없습니다.");
        }
    }

    private String resolveCanonicalNickname(String ouid, String fallbackNickname) {
        try {
            UserBasicDto basic = nexonApiClient.getUserBasic(ouid);
            if (basic != null && basic.getUser_name() != null && !basic.getUser_name().isBlank()) {
                return basic.getUser_name().trim();
            }
        } catch (RuntimeException exception) {
            return fallbackNickname;
        }
        return fallbackNickname;
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Session could not be verified.", exception);
        }
    }

    private AuthUserResponse toUserResponse(AuthUser user) {
        String displayName = firstNonBlank(user.getSuddenNickname(), user.getDisplayName());
        String email = firstNonBlank(user.getEmail(), user.getLoginId());
        return new AuthUserResponse(
                user.getId(),
                email,
                user.getLoginId(),
                user.getSuddenNickname(),
                displayName,
                user.getOuid(),
                Boolean.TRUE.equals(user.getNicknameVerified()),
                user.isAdmin(),
                Boolean.TRUE.equals(user.getClanNone())
        );
    }

    private AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    private AuthException unauthorized() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요하거나 세션이 만료되었습니다.");
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
