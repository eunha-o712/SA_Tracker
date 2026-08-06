package com.sa.trk.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;

import com.sa.trk.auth.dto.AuthLoginRequest;
import com.sa.trk.auth.dto.AccountStatusUpdateRequest;
import com.sa.trk.auth.dto.AuthRegisterRequest;
import com.sa.trk.auth.dto.ClanStatusUpdateRequest;
import com.sa.trk.auth.dto.EmailVerificationConfirmRequest;
import com.sa.trk.auth.dto.EmailVerificationRequest;
import com.sa.trk.auth.dto.PasswordResetConfirmRequest;
import com.sa.trk.auth.dto.PasswordResetRequest;
import com.sa.trk.auth.dto.SuddenAccountLinkRequest;
import com.sa.trk.auth.entity.AuthSession;
import com.sa.trk.auth.entity.AccountStatus;
import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.entity.EmailVerificationToken;
import com.sa.trk.auth.entity.PasswordResetToken;
import com.sa.trk.auth.repository.AuthSessionRepository;
import com.sa.trk.auth.repository.AuthUserRepository;
import com.sa.trk.auth.repository.EmailVerificationTokenRepository;
import com.sa.trk.auth.repository.PasswordResetTokenRepository;
import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserBasicDto;

class AuthServiceTests {

    @Mock
    private AuthUserRepository userRepository;

    @Mock
    private AuthSessionRepository sessionRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private AccountEmailService accountEmailService;

    @Mock
    private NexonApiClient nexonApiClient;

    private PasswordHasher passwordHasher;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        passwordHasher = new PasswordHasher();
        authService = new AuthService(
                userRepository,
                sessionRepository,
                emailVerificationTokenRepository,
                passwordResetTokenRepository,
                accountEmailService,
                passwordHasher,
                nexonApiClient
        );
    }

    @Test
    void registersAUserPendingEmailVerificationWithoutCreatingSession() {
        when(userRepository.existsByEmailIgnoreCase("member@satrk.gg")).thenReturn(false);
        when(userRepository.existsByLoginIdIgnoreCase("user001")).thenReturn(false);
        when(userRepository.save(any(AuthUser.class))).thenAnswer(invocation -> {
            AuthUser user = invocation.getArgument(0);
            user.setId(12L);
            return user;
        });
        when(emailVerificationTokenRepository.findTopByUserOrderByCreatedAtDesc(any(AuthUser.class)))
                .thenReturn(Optional.empty());
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountEmailService.sendEmailVerificationLink(any(), any())).thenReturn(true);

        var result = authService.register(new AuthRegisterRequest(" MEMBER@SATRK.GG ", null, "password123!", null, null));

        ArgumentCaptor<AuthUser> storedUser = ArgumentCaptor.forClass(AuthUser.class);
        ArgumentCaptor<EmailVerificationToken> storedToken =
                ArgumentCaptor.forClass(EmailVerificationToken.class);
        ArgumentCaptor<String> deliveredToken = ArgumentCaptor.forClass(String.class);
        assertThat(result.message()).contains("인증 링크");
        verify(userRepository).save(storedUser.capture());
        verify(emailVerificationTokenRepository).save(storedToken.capture());
        verify(accountEmailService).sendEmailVerificationLink(
                eq("member@satrk.gg"),
                deliveredToken.capture()
        );
        assertThat(storedUser.getValue().getId()).isEqualTo(12L);
        assertThat(storedUser.getValue().getEmail()).isEqualTo("member@satrk.gg");
        assertThat(storedUser.getValue().getLoginId()).isEqualTo("user001");
        assertThat(storedUser.getValue().getEmailVerificationPending()).isTrue();
        assertThat(storedUser.getValue().getEmailVerifiedAt()).isNull();
        assertThat(storedToken.getValue().getTokenHash()).hasSize(64);
        assertThat(storedToken.getValue().getTokenHash()).isNotEqualTo(deliveredToken.getValue());
        verify(nexonApiClient, never()).getOuid(any());
        verify(sessionRepository, never()).save(any(AuthSession.class));
    }

    @Test
    void linksSuddenAccountFromMyPage() {
        AuthUser user = user("member@satrk.gg", null, null, "password123!");
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session(user)));
        when(nexonApiClient.getOuid("tracker")).thenReturn(ouid("ouid-123"));
        when(userRepository.findByOuid("ouid-123")).thenReturn(Optional.empty());
        when(nexonApiClient.getUserBasic("ouid-123")).thenReturn(basic("tracker"));

        var result = authService.linkSuddenAccount("session-token", new SuddenAccountLinkRequest("tracker"));

        assertThat(result.suddenNickname()).isEqualTo("tracker");
        assertThat(result.displayName()).isEqualTo("tracker");
        assertThat(result.ouid()).isEqualTo("ouid-123");
        assertThat(result.nicknameVerified()).isFalse();
    }

    @Test
    void rejectsDuplicateOuidWithSupportBoardMessage() {
        AuthUser user = user("member@satrk.gg", null, null, "password123!");
        AuthUser existing = user("other@satrk.gg", "taken", "ouid-123", "password123!");
        existing.setId(99L);
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session(user)));
        when(nexonApiClient.getOuid("tracker")).thenReturn(ouid("ouid-123"));
        when(userRepository.findByOuid("ouid-123")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.linkSuddenAccount("session-token", new SuddenAccountLinkRequest("tracker")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("문의게시판");
    }

    @Test
    void explainsHowToFixUnknownSuddenNickname() {
        AuthUser user = user("member@satrk.gg", null, null, "password123!");
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session(user)));
        when(nexonApiClient.getOuid("unknown-name")).thenThrow(new IllegalArgumentException("invalid"));

        assertThatThrownBy(() -> authService.linkSuddenAccount(
                "session-token",
                new SuddenAccountLinkRequest("unknown-name")
        ))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("게임 내 현재 닉네임")
                .hasMessageContaining("특수문자");
    }

    @Test
    void logsInWithValidCredentials() {
        AuthUser user = user("member@satrk.gg", "agent", "ouid-123", "password123!");
        when(userRepository.findByEmailIgnoreCase("member@satrk.gg")).thenReturn(Optional.of(user));
        when(sessionRepository.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = authService.login(new AuthLoginRequest("member@satrk.gg", null, "password123!"));

        assertThat(result.user().suddenNickname()).isEqualTo("agent");
        assertThat(result.expiresAt()).isAfter(Instant.now().plusSeconds(29L * 24 * 60 * 60));
    }

    @Test
    void pendingEmailVerificationPreventsLogin() {
        AuthUser user = user("member@satrk.gg", null, null, "password123!");
        user.setEmailVerificationPending(true);
        when(userRepository.findByEmailIgnoreCase("member@satrk.gg")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(
                new AuthLoginRequest("member@satrk.gg", null, "password123!")
        ))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("이메일 인증");

        verify(sessionRepository, never()).save(any(AuthSession.class));
    }

    @Test
    void confirmsEmailVerificationAndAllowsFutureLogin() {
        AuthUser user = user("member@satrk.gg", null, null, "password123!");
        user.setEmailVerificationPending(true);
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setTokenHash("unused-in-test");
        verificationToken.setCreatedAt(Instant.now());
        verificationToken.setExpiresAt(Instant.now().plusSeconds(60));
        when(emailVerificationTokenRepository.findForUpdateByTokenHash(any()))
                .thenReturn(Optional.of(verificationToken));
        when(emailVerificationTokenRepository.saveAndFlush(any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.confirmEmailVerification(
                new EmailVerificationConfirmRequest("raw-token")
        );

        assertThat(response.message()).contains("완료");
        assertThat(user.getEmailVerificationPending()).isFalse();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(verificationToken.getUsedAt()).isNotNull();
        verify(emailVerificationTokenRepository).deleteByUserAndUsedAtIsNull(user);
    }

    @Test
    void resendingEmailVerificationWithinCooldownDoesNotSendAgain() {
        AuthUser user = user("member@satrk.gg", null, null, "password123!");
        user.setEmailVerificationPending(true);
        EmailVerificationToken recentToken = new EmailVerificationToken();
        recentToken.setCreatedAt(Instant.now().minusSeconds(10));
        when(userRepository.findByEmailIgnoreCase("member@satrk.gg")).thenReturn(Optional.of(user));
        when(emailVerificationTokenRepository.findTopByUserOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(recentToken));

        var response = authService.resendEmailVerification(
                new EmailVerificationRequest("member@satrk.gg")
        );

        assertThat(response.message()).contains("인증이 필요한 계정");
        verify(accountEmailService, never()).sendEmailVerificationLink(any(), any());
    }

    @Test
    void rejectsWrongPasswordWithoutRevealingWhichFieldFailed() {
        AuthUser user = user("member@satrk.gg", "agent", "ouid-123", "password123!");
        when(userRepository.findByEmailIgnoreCase("member@satrk.gg")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("member@satrk.gg", null, "wrong-password")))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void suspendedUserCannotLogin() {
        AuthUser user = user("member@satrk.gg", "agent", "ouid-123", "password123!");
        user.setAccountStatus(AccountStatus.SUSPENDED);
        when(userRepository.findByEmailIgnoreCase("member@satrk.gg")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("member@satrk.gg", null, "password123!")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("일시 정지");
    }

    @Test
    void rejectsAnExpiredSession() {
        AuthSession session = session(user("member@satrk.gg", "agent", "ouid-123", "password123!"));
        session.setExpiresAt(Instant.now().minusSeconds(1));
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authService.currentUser("expired-token"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void requestsPasswordResetWithoutStoringRawToken() {
        AuthUser user = user("member@satrk.gg", "agent", "ouid-123", "password123!");
        when(userRepository.findForUpdateByEmailIgnoreCase("member@satrk.gg")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.countByUserAndCreatedAtGreaterThanEqual(
                eq(user),
                any(Instant.class)
        )).thenReturn(2L);
        when(passwordResetTokenRepository.findTopByUserOrderByCreatedAtDesc(user)).thenReturn(Optional.empty());
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountEmailService.sendResetLink(any(), any())).thenReturn(true);

        var response = authService.requestPasswordReset(new PasswordResetRequest("member@satrk.gg"));

        ArgumentCaptor<PasswordResetToken> storedToken = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<String> deliveredToken = ArgumentCaptor.forClass(String.class);
        assertThat(response.message()).contains("있다면");
        verify(passwordResetTokenRepository).save(storedToken.capture());
        verify(accountEmailService).sendResetLink(eq("member@satrk.gg"), deliveredToken.capture());
        assertThat(storedToken.getValue().getTokenHash()).hasSize(64);
        assertThat(storedToken.getValue().getTokenHash()).isNotEqualTo(deliveredToken.getValue());
    }

    @Test
    void confirmsPasswordResetRevokesSessionsAndDoesNotAutoLogin() {
        AuthUser user = user("member@satrk.gg", "agent", "ouid-123", "old-password");
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash("unused-in-test");
        resetToken.setCreatedAt(Instant.now());
        resetToken.setExpiresAt(Instant.now().plusSeconds(60));
        when(passwordResetTokenRepository.findForUpdateByTokenHash(any())).thenReturn(Optional.of(resetToken));
        when(passwordResetTokenRepository.saveAndFlush(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.confirmPasswordReset(
                new PasswordResetConfirmRequest("raw-token", "new-password123!", "new-password123!")
        );

        assertThat(response.message()).contains("다시 로그인");
        assertThat(passwordHasher.matches("new-password123!", user.getPasswordSalt(), user.getPasswordHash())).isTrue();
        assertThat(resetToken.getUsedAt()).isNotNull();
        verify(sessionRepository).deleteByUser(user);
        verify(passwordResetTokenRepository).invalidateUnusedByUser(eq(user), any(Instant.class));
        verify(sessionRepository, never()).save(any(AuthSession.class));
        verify(accountEmailService).sendPasswordChangedNotice("member@satrk.gg");
    }

    @Test
    void rejectsMismatchedPasswordConfirmationBeforeUsingToken() {
        assertThatThrownBy(() -> authService.confirmPasswordReset(
                new PasswordResetConfirmRequest("raw-token", "new-password123!", "different-password!")
        ))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("일치하지 않습니다");

        verify(passwordResetTokenRepository, never()).findForUpdateByTokenHash(any());
    }

    @Test
    void repeatedPasswordResetRequestWithinCooldownReturnsGenericResponseWithoutSendingAgain() {
        AuthUser user = user("member@satrk.gg", "agent", "ouid-123", "password123!");
        PasswordResetToken recentToken = new PasswordResetToken();
        recentToken.setCreatedAt(Instant.now().minusSeconds(10));
        when(userRepository.findForUpdateByEmailIgnoreCase("member@satrk.gg")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findTopByUserOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(recentToken));

        var response = authService.requestPasswordReset(new PasswordResetRequest("member@satrk.gg"));

        assertThat(response.message()).contains("있다면");
        verify(accountEmailService, never()).sendResetLink(any(), any());
    }

    @Test
    void fourthPasswordResetRequestInKoreanCalendarDayIsRejected() {
        AuthUser user = user("member@satrk.gg", "agent", "ouid-123", "password123!");
        when(userRepository.findForUpdateByEmailIgnoreCase("member@satrk.gg"))
                .thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.countByUserAndCreatedAtGreaterThanEqual(
                eq(user),
                any(Instant.class)
        )).thenReturn(3L);

        assertThatThrownBy(() -> authService.requestPasswordReset(
                new PasswordResetRequest("member@satrk.gg")
        ))
                .isInstanceOf(AuthException.class)
                .hasMessage("비밀번호 재설정 메일은 하루 최대 3회까지 요청할 수 있습니다. 내일 다시 시도해 주세요.");

        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(accountEmailService, never()).sendResetLink(any(), any());
    }

    @Test
    void passwordResetReturnsDailyLimitMessageWithoutLookingUpAccount() {
        when(accountEmailService.isDailyLimitReached()).thenReturn(true);

        var response = authService.requestPasswordReset(new PasswordResetRequest("member@satrk.gg"));

        assertThat(response.message()).isEqualTo("죄송합니다, 당일 상한 초과로 명일 이용 부탁드립니다.");
        verify(userRepository, never()).findForUpdateByEmailIgnoreCase(any());
        verify(accountEmailService, never()).sendResetLink(any(), any());
    }

    @Test
    void syncsSuddenNicknameFromOuid() {
        AuthUser user = user("member@satrk.gg", "oldName", "ouid-123", "password123!");
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session(user)));
        when(nexonApiClient.getUserBasic("ouid-123")).thenReturn(basic("newName"));

        var response = authService.syncSuddenNickname("session-token");

        assertThat(response.suddenNickname()).isEqualTo("newName");
        assertThat(user.getDisplayName()).isEqualTo("newName");
    }

    @Test
    void linkedUserCanSetCurrentClanToNone() {
        AuthUser user = user("member@satrk.gg", "tracker", "ouid-123", "password123!");
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session(user)));

        var response = authService.updateClanStatus("session-token", new ClanStatusUpdateRequest(true));

        assertThat(response.clanNone()).isTrue();
        assertThat(user.getClanNone()).isTrue();
    }

    @Test
    void unlinkedUserCannotChangeClanStatus() {
        AuthUser user = user("member@satrk.gg", null, null, "password123!");
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session(user)));

        assertThatThrownBy(() -> authService.updateClanStatus("session-token", new ClanStatusUpdateRequest(true)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("연결");
    }

    @Test
    void adminCanCompleteManualVerificationForLinkedUser() {
        AuthUser admin = user("admin@satrk.gg", "operator", "admin-ouid", "password123!");
        admin.setId(9L);
        admin.setAdmin(true);
        AuthUser target = user("member@satrk.gg", "tracker", "ouid-123", "password123!");
        target.setId(12L);
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session(admin)));
        when(userRepository.findById(12L)).thenReturn(Optional.of(target));

        var response = authService.setManualVerification("admin-token", 12L, true);

        assertThat(response.nicknameVerified()).isTrue();
        assertThat(target.getVerifiedAt()).isNotNull();
    }

    @Test
    void adminCannotVerifyUserWithoutLinkedOuid() {
        AuthUser admin = user("admin@satrk.gg", "operator", "admin-ouid", "password123!");
        admin.setAdmin(true);
        AuthUser target = user("member@satrk.gg", null, null, "password123!");
        target.setId(12L);
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session(admin)));
        when(userRepository.findById(12L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> authService.setManualVerification("admin-token", 12L, true))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("OUID");
    }

    @Test
    void adminCanSuspendAndRestoreAccount() {
        AuthUser admin = user("admin@satrk.gg", "operator", "admin-ouid", "password123!");
        admin.setId(9L);
        admin.setAdmin(true);
        AuthUser target = user("member@satrk.gg", "tracker", "ouid-123", "password123!");
        target.setId(12L);
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session(admin)));
        when(userRepository.findById(12L)).thenReturn(Optional.of(target));

        var suspended = authService.setAccountStatus(
                "admin-token",
                12L,
                new AccountStatusUpdateRequest("SUSPENDED", "OUID 분쟁 확인 중")
        );
        assertThat(suspended.accountStatus()).isEqualTo("SUSPENDED");
        verify(sessionRepository).deleteByUser(target);

        var restored = authService.setAccountStatus(
                "admin-token",
                12L,
                new AccountStatusUpdateRequest("ACTIVE", null)
        );
        assertThat(restored.accountStatus()).isEqualTo("ACTIVE");
        assertThat(target.getSanctionReason()).isNull();
    }

    private AuthUser user(String email, String suddenNickname, String ouid, String password) {
        String salt = passwordHasher.newSalt();
        AuthUser user = new AuthUser();
        user.setId(1L);
        user.setEmail(email);
        user.setLoginId("user001");
        user.setSuddenNickname(suddenNickname);
        user.setDisplayName(suddenNickname == null ? "user001" : suddenNickname);
        user.setOuid(ouid);
        user.setClanNone(false);
        user.setNicknameVerified(false);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setPasswordSalt(salt);
        user.setPasswordHash(passwordHasher.hash(password, salt));
        user.setCreatedAt(Instant.now());
        return user;
    }

    private AuthSession session(AuthUser user) {
        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setExpiresAt(Instant.now().plusSeconds(60));
        return session;
    }

    private OuidResponseDto ouid(String value) {
        OuidResponseDto response = new OuidResponseDto();
        response.setOuid(value);
        return response;
    }

    private UserBasicDto basic(String userName) {
        UserBasicDto basic = new UserBasicDto();
        basic.setUser_name(userName);
        return basic;
    }
}
