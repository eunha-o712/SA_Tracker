package com.sa.trk.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.trk.auth.dto.AuthLoginRequest;
import com.sa.trk.auth.dto.AccountStatusUpdateRequest;
import com.sa.trk.auth.dto.AdminAccountStatusResponse;
import com.sa.trk.auth.dto.AuthRegisterRequest;
import com.sa.trk.auth.dto.AuthResponse;
import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.dto.ClanStatusUpdateRequest;
import com.sa.trk.auth.dto.EmailVerificationConfirmRequest;
import com.sa.trk.auth.dto.EmailVerificationRequest;
import com.sa.trk.auth.dto.EmailVerificationResponse;
import com.sa.trk.auth.dto.PasswordResetConfirmRequest;
import com.sa.trk.auth.dto.PasswordResetConfirmResponse;
import com.sa.trk.auth.dto.PasswordResetRequest;
import com.sa.trk.auth.dto.PasswordResetRequestResponse;
import com.sa.trk.auth.dto.SuddenAccountLinkRequest;
import com.sa.trk.auth.entity.AuthSession;
import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.entity.AccountStatus;
import com.sa.trk.auth.entity.EmailVerificationToken;
import com.sa.trk.auth.entity.PasswordResetToken;
import com.sa.trk.auth.repository.AuthSessionRepository;
import com.sa.trk.auth.repository.AuthUserRepository;
import com.sa.trk.auth.repository.EmailVerificationTokenRepository;
import com.sa.trk.auth.repository.PasswordResetTokenRepository;
import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserBasicDto;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PROFILE_IMAGE_URL_PATTERN = Pattern.compile(
            "^(?:/api/profile-images/[0-9a-f-]{36}\\.(?:jpg|png|webp)|"
                    + "https://res\\.cloudinary\\.com/[A-Za-z0-9_-]+/image/upload/"
                    + "(?:v\\d+/)?satrk/profile/[0-9a-f-]{36}\\.(?:jpe?g|png|webp))$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Duration SESSION_DURATION = Duration.ofDays(30);
    private static final Duration EMAIL_VERIFICATION_DURATION = Duration.ofHours(24);
    private static final Duration EMAIL_VERIFICATION_REQUEST_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration PASSWORD_RESET_DURATION = Duration.ofMinutes(15);
    private static final Duration PASSWORD_RESET_REQUEST_COOLDOWN = Duration.ofSeconds(60);
    private static final int PASSWORD_RESET_DAILY_LIMIT = 3;
    private static final ZoneId PASSWORD_RESET_LIMIT_ZONE = ZoneId.of("Asia/Seoul");
    private static final String PASSWORD_RESET_REQUEST_MESSAGE =
            "입력한 이메일로 가입된 계정이 있다면 비밀번호 재설정 링크를 전송했습니다.";
    private static final String PASSWORD_RESET_DAILY_LIMIT_MESSAGE =
            "비밀번호 재설정 메일은 하루 최대 3회까지 요청할 수 있습니다. 내일 다시 시도해 주세요.";
    private static final String EMAIL_VERIFICATION_REQUEST_MESSAGE =
            "인증이 필요한 계정이라면 이메일 인증 링크를 전송했습니다.";
    private static final String REGISTRATION_EMAIL_SENT_MESSAGE =
            "입력한 이메일로 인증 링크를 전송했습니다. 이메일 인증 후 로그인해 주세요.";
    private static final String MAIL_DAILY_LIMIT_MESSAGE =
            "죄송합니다, 당일 상한 초과로 명일 이용 부탁드립니다.";

    private final AuthUserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AccountEmailService accountEmailService;
    private final PasswordHasher passwordHasher;
    private final NexonApiClient nexonApiClient;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            AuthUserRepository userRepository,
            AuthSessionRepository sessionRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            AccountEmailService accountEmailService,
            PasswordHasher passwordHasher,
            NexonApiClient nexonApiClient) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.accountEmailService = accountEmailService;
        this.passwordHasher = passwordHasher;
        this.nexonApiClient = nexonApiClient;
    }

    @Transactional
    public EmailVerificationResponse register(AuthRegisterRequest request) {
        if (accountEmailService.isDailyLimitReached()) {
            throw mailDailyLimitException();
        }
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
        user.setEmailVerificationPending(true);
        user.setEmailVerifiedAt(null);
        user.setAdmin(false);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setSanctionReason(null);
        user.setSanctionedAt(null);
        user.setSanctionedById(null);
        user.setPasswordSalt(salt);
        user.setPasswordHash(passwordHasher.hash(password, salt));
        user.setCreatedAt(Instant.now());

        AuthUser savedUser = userRepository.save(user);
        return createEmailVerificationToken(savedUser, REGISTRATION_EMAIL_SENT_MESSAGE, true);
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
        requireActiveAccount(user);
        requireEmailVerified(user);
        return createSession(user);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse currentUser(String rawToken) {
        return toUserResponse(findValidSession(rawToken).getUser());
    }

    @Transactional
    public AuthUserResponse updateProfileImage(String rawToken, String profileImageUrl) {
        String normalizedUrl = profileImageUrl == null ? "" : profileImageUrl.trim();
        if (!PROFILE_IMAGE_URL_PATTERN.matcher(normalizedUrl).matches()) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PROFILE_IMAGE",
                    "올바르지 않은 프로필 이미지입니다."
            );
        }
        AuthUser user = findValidSession(rawToken).getUser();
        user.setProfileImageUrl(normalizedUrl);
        return toUserResponse(user);
    }

    @Transactional
    public AuthUserResponse clearProfileImage(String rawToken) {
        AuthUser user = findValidSession(rawToken).getUser();
        user.setProfileImageUrl(null);
        return toUserResponse(user);
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
    public AdminAccountStatusResponse setAccountStatus(
            String rawToken,
            Long userId,
            AccountStatusUpdateRequest request) {
        AuthUserResponse admin = requireAdmin(rawToken);
        if (userId == null || userId < 1 || request == null) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT_STATUS_REQUEST", "계정 상태 정보를 확인해 주세요.");
        }

        AccountStatus status;
        try {
            status = AccountStatus.valueOf(String.valueOf(request.status()).trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT_STATUS", "계정 상태를 확인해 주세요.");
        }
        if (Objects.equals(admin.id(), userId) && status != AccountStatus.ACTIVE) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "ADMIN_SELF_SANCTION", "현재 로그인한 관리자 계정은 정지할 수 없습니다.");
        }

        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "회원을 찾을 수 없습니다."));
        if (status == AccountStatus.ACTIVE) {
            user.setAccountStatus(AccountStatus.ACTIVE);
            user.setSanctionReason(null);
            user.setSanctionedAt(null);
            user.setSanctionedById(null);
        } else {
            String reason = request.reason() == null ? "" : request.reason().trim();
            if (reason.length() < 5 || reason.length() > 500) {
                throw new AuthException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_SANCTION_REASON",
                        "정지 또는 차단 사유를 5~500자로 입력해 주세요."
                );
            }
            user.setAccountStatus(status);
            user.setSanctionReason(reason);
            user.setSanctionedAt(Instant.now());
            user.setSanctionedById(admin.id());
            sessionRepository.deleteByUser(user);
        }
        return toAdminAccountStatusResponse(user);
    }

    @Transactional
    public PasswordResetRequestResponse requestPasswordReset(PasswordResetRequest request) {
        if (accountEmailService.isDailyLimitReached()) {
            return new PasswordResetRequestResponse(MAIL_DAILY_LIMIT_MESSAGE);
        }
        String email = normalizeEmail(request == null ? null : request.email());
        return userRepository.findForUpdateByEmailIgnoreCase(email)
                .map(user -> createPasswordResetToken(email, user))
                .orElseGet(this::passwordResetRequestResponse);
    }

    @Transactional
    public PasswordResetConfirmResponse confirmPasswordReset(PasswordResetConfirmRequest request) {
        String rawToken = request == null ? null : request.token();
        String password = request == null ? null : request.password();
        String passwordConfirm = request == null ? null : request.passwordConfirm();
        validatePassword(password);
        if (!Objects.equals(password, passwordConfirm)) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_CONFIRMATION_MISMATCH",
                    "새 비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        if (rawToken == null || rawToken.isBlank()) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "비밀번호 재설정 주소가 올바르지 않습니다.");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findForUpdateByTokenHash(hashToken(rawToken.trim()))
                .orElseThrow(() -> new AuthException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_RESET_TOKEN",
                        "비밀번호 재설정 주소가 올바르지 않습니다."
                ));
        Instant now = Instant.now();
        if (resetToken.getUsedAt() != null || !resetToken.getExpiresAt().isAfter(now)) {
            throw new AuthException(
                    HttpStatus.BAD_REQUEST,
                    "RESET_TOKEN_EXPIRED",
                    "비밀번호 재설정 주소가 만료되었거나 이미 사용되었습니다."
            );
        }

        AuthUser user = resetToken.getUser();
        String salt = passwordHasher.newSalt();
        user.setPasswordSalt(salt);
        user.setPasswordHash(passwordHasher.hash(password, salt));
        resetToken.setUsedAt(now);
        passwordResetTokenRepository.saveAndFlush(resetToken);
        passwordResetTokenRepository.invalidateUnusedByUser(user, now);
        sessionRepository.deleteByUser(user);
        accountEmailService.sendPasswordChangedNotice(user.getEmail());

        return new PasswordResetConfirmResponse(
                "비밀번호가 변경되었습니다. 새 비밀번호로 다시 로그인해 주세요."
        );
    }

    @Transactional
    public EmailVerificationResponse resendEmailVerification(EmailVerificationRequest request) {
        if (accountEmailService.isDailyLimitReached()) {
            return new EmailVerificationResponse(MAIL_DAILY_LIMIT_MESSAGE);
        }
        String email = normalizeEmail(request == null ? null : request.email());
        return userRepository.findByEmailIgnoreCase(email)
                .filter(user -> Boolean.TRUE.equals(user.getEmailVerificationPending()))
                .map(user -> createEmailVerificationToken(
                        user,
                        EMAIL_VERIFICATION_REQUEST_MESSAGE,
                        false
                ))
                .orElseGet(this::emailVerificationRequestResponse);
    }

    @Transactional
    public EmailVerificationResponse confirmEmailVerification(EmailVerificationConfirmRequest request) {
        String rawToken = request == null ? null : request.token();
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidEmailVerificationToken();
        }

        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findForUpdateByTokenHash(hashToken(rawToken.trim()))
                .orElseThrow(this::invalidEmailVerificationToken);
        Instant now = Instant.now();
        if (verificationToken.getUsedAt() != null || !verificationToken.getExpiresAt().isAfter(now)) {
            throw invalidEmailVerificationToken();
        }

        AuthUser user = verificationToken.getUser();
        if (!Boolean.TRUE.equals(user.getEmailVerificationPending())) {
            throw invalidEmailVerificationToken();
        }

        user.setEmailVerificationPending(false);
        user.setEmailVerifiedAt(now);
        verificationToken.setUsedAt(now);
        emailVerificationTokenRepository.saveAndFlush(verificationToken);
        emailVerificationTokenRepository.deleteByUserAndUsedAtIsNull(user);

        return new EmailVerificationResponse("이메일 인증이 완료되었습니다. 로그인해 주세요.");
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

    private EmailVerificationResponse createEmailVerificationToken(
            AuthUser user,
            String responseMessage,
            boolean deliveryRequired) {
        Instant now = Instant.now();
        boolean coolingDown = emailVerificationTokenRepository.findTopByUserOrderByCreatedAtDesc(user)
                .map(token -> token.getCreatedAt() != null
                        && token.getCreatedAt().isAfter(now.minus(EMAIL_VERIFICATION_REQUEST_COOLDOWN)))
                .orElse(false);
        if (coolingDown) {
            return new EmailVerificationResponse(responseMessage);
        }

        emailVerificationTokenRepository.deleteByUserAndUsedAtIsNull(user);

        String rawToken = newToken();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setTokenHash(hashToken(rawToken));
        verificationToken.setCreatedAt(now);
        verificationToken.setExpiresAt(now.plus(EMAIL_VERIFICATION_DURATION));
        emailVerificationTokenRepository.save(verificationToken);

        boolean sent = accountEmailService.sendEmailVerificationLink(user.getEmail(), rawToken);
        if (!sent && accountEmailService.isDailyLimitReached()) {
            if (deliveryRequired) {
                throw mailDailyLimitException();
            }
            return new EmailVerificationResponse(MAIL_DAILY_LIMIT_MESSAGE);
        }
        if (!sent && deliveryRequired) {
            throw new AuthException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "EMAIL_DELIVERY_FAILED",
                    "인증메일을 전송하지 못했습니다. 잠시 후 다시 시도해 주세요."
            );
        }
        return new EmailVerificationResponse(responseMessage);
    }

    private EmailVerificationResponse emailVerificationRequestResponse() {
        return new EmailVerificationResponse(EMAIL_VERIFICATION_REQUEST_MESSAGE);
    }

    private PasswordResetRequestResponse createPasswordResetToken(String email, AuthUser user) {
        Instant now = Instant.now();
        Instant startOfToday = now.atZone(PASSWORD_RESET_LIMIT_ZONE)
                .toLocalDate()
                .atStartOfDay(PASSWORD_RESET_LIMIT_ZONE)
                .toInstant();
        long issuedToday = passwordResetTokenRepository
                .countByUserAndCreatedAtGreaterThanEqual(user, startOfToday);
        if (issuedToday >= PASSWORD_RESET_DAILY_LIMIT) {
            throw passwordResetDailyLimitException();
        }

        boolean coolingDown = passwordResetTokenRepository.findTopByUserOrderByCreatedAtDesc(user)
                .map(token -> token.getCreatedAt() != null
                        && token.getCreatedAt().isAfter(now.minus(PASSWORD_RESET_REQUEST_COOLDOWN)))
                .orElse(false);
        if (coolingDown) {
            return passwordResetRequestResponse();
        }

        passwordResetTokenRepository.invalidateUnusedByUser(user, now);

        String rawToken = newToken();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setCreatedAt(now);
        resetToken.setExpiresAt(now.plus(PASSWORD_RESET_DURATION));
        passwordResetTokenRepository.save(resetToken);

        boolean sent = accountEmailService.sendResetLink(email, rawToken);
        if (!sent && accountEmailService.isDailyLimitReached()) {
            return new PasswordResetRequestResponse(MAIL_DAILY_LIMIT_MESSAGE);
        }
        return passwordResetRequestResponse();
    }

    private PasswordResetRequestResponse passwordResetRequestResponse() {
        return new PasswordResetRequestResponse(PASSWORD_RESET_REQUEST_MESSAGE);
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
        requireActiveAccount(session.getUser());
        requireEmailVerified(session.getUser());
        return session;
    }

    private void requireActiveAccount(AuthUser user) {
        AccountStatus status = user.getAccountStatus() == null ? AccountStatus.ACTIVE : user.getAccountStatus();
        if (status == AccountStatus.SUSPENDED) {
            throw new AuthException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_SUSPENDED",
                    "운영자 확인을 위해 계정 이용이 일시 정지되었습니다. 문의게시판으로 문의해 주세요."
            );
        }
        if (status == AccountStatus.BANNED) {
            throw new AuthException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_BANNED",
                    "운영 정책에 따라 계정 이용이 제한되었습니다."
            );
        }
    }

    private void requireEmailVerified(AuthUser user) {
        if (Boolean.TRUE.equals(user.getEmailVerificationPending())) {
            throw new AuthException(
                    HttpStatus.FORBIDDEN,
                    "EMAIL_NOT_VERIFIED",
                    "이메일 인증이 완료되지 않았습니다. 받은편지함을 확인해 주세요."
            );
        }
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
                throw suddenAccountNotFound();
            }
            return response.getOuid().trim();
        } catch (AuthException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw suddenAccountNotFound();
        }
    }

    private AuthException suddenAccountNotFound() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "SUDDEN_ACCOUNT_NOT_FOUND",
                "입력한 닉네임으로 서든 계정을 찾을 수 없습니다. 게임 내 현재 닉네임을 띄어쓰기와 특수문자까지 정확히 확인해 주세요."
        );
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
                Boolean.TRUE.equals(user.getClanNone()),
                user.getProfileImageUrl()
        );
    }

    private AdminAccountStatusResponse toAdminAccountStatusResponse(AuthUser user) {
        AccountStatus status = user.getAccountStatus() == null ? AccountStatus.ACTIVE : user.getAccountStatus();
        return new AdminAccountStatusResponse(
                user.getId(),
                firstNonBlank(user.getSuddenNickname(), user.getDisplayName()),
                status.name(),
                user.getSanctionReason(),
                user.getSanctionedAt(),
                user.getSanctionedById()
        );
    }

    private AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    private AuthException invalidEmailVerificationToken() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "INVALID_EMAIL_VERIFICATION_TOKEN",
                "이메일 인증 주소가 만료되었거나 이미 사용되었습니다."
        );
    }

    private AuthException mailDailyLimitException() {
        return new AuthException(
                HttpStatus.TOO_MANY_REQUESTS,
                "MAIL_DAILY_LIMIT_REACHED",
                MAIL_DAILY_LIMIT_MESSAGE
        );
    }

    private AuthException passwordResetDailyLimitException() {
        return new AuthException(
                HttpStatus.TOO_MANY_REQUESTS,
                "PASSWORD_RESET_DAILY_LIMIT_REACHED",
                PASSWORD_RESET_DAILY_LIMIT_MESSAGE
        );
    }

    private AuthException unauthorized() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요하거나 세션이 만료되었습니다.");
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
