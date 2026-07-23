package com.sa.trk.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.auth.dto.AuthLoginRequest;
import com.sa.trk.auth.dto.AccountStatusUpdateRequest;
import com.sa.trk.auth.dto.AuthRegisterRequest;
import com.sa.trk.auth.dto.ClanStatusUpdateRequest;
import com.sa.trk.auth.dto.PasswordResetConfirmRequest;
import com.sa.trk.auth.dto.PasswordResetRequest;
import com.sa.trk.auth.dto.SuddenAccountLinkRequest;
import com.sa.trk.auth.entity.AuthSession;
import com.sa.trk.auth.entity.AccountStatus;
import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.entity.PasswordResetToken;
import com.sa.trk.auth.repository.AuthSessionRepository;
import com.sa.trk.auth.repository.AuthUserRepository;
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
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordResetEmailService passwordResetEmailService;

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
                passwordResetTokenRepository,
                passwordResetEmailService,
                passwordHasher,
                nexonApiClient
        );
    }

    @Test
    void registersAUserWithInternalIdOnlyAndCreatesASession() {
        when(userRepository.existsByEmailIgnoreCase("member@satrk.gg")).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.existsByLoginIdIgnoreCase("user001")).thenReturn(false);
        when(userRepository.save(any(AuthUser.class))).thenAnswer(invocation -> {
            AuthUser user = invocation.getArgument(0);
            user.setId(12L);
            return user;
        });
        when(sessionRepository.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = authService.register(new AuthRegisterRequest(" MEMBER@SATRK.GG ", null, "password123!", null, null));

        assertThat(result.token()).isNotBlank();
        assertThat(result.user().id()).isEqualTo(12L);
        assertThat(result.user().email()).isEqualTo("member@satrk.gg");
        assertThat(result.user().loginId()).isEqualTo("user001");
        assertThat(result.user().displayName()).isEqualTo("user001");
        assertThat(result.user().suddenNickname()).isNull();
        assertThat(result.user().ouid()).isNull();
        verify(nexonApiClient, never()).getOuid(any());
        verify(sessionRepository).save(any(AuthSession.class));
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
    void logsInWithValidCredentials() {
        AuthUser user = user("member@satrk.gg", "agent", "ouid-123", "password123!");
        when(userRepository.findByEmailIgnoreCase("member@satrk.gg")).thenReturn(Optional.of(user));
        when(sessionRepository.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = authService.login(new AuthLoginRequest("member@satrk.gg", null, "password123!"));

        assertThat(result.user().suddenNickname()).isEqualTo("agent");
        assertThat(result.expiresAt()).isAfter(Instant.now().plusSeconds(29L * 24 * 60 * 60));
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
        when(userRepository.findByEmailIgnoreCase("member@satrk.gg")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetEmailService.sendResetLink(any(), any())).thenReturn(new PasswordResetDelivery("http://localhost:5173/login?resetToken=test"));

        var response = authService.requestPasswordReset(new PasswordResetRequest("member@satrk.gg"));

        assertThat(response.devResetUrl()).contains("resetToken");
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void confirmsPasswordResetAndCreatesSession() {
        AuthUser user = user("member@satrk.gg", "agent", "ouid-123", "old-password");
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash("unused-in-test");
        resetToken.setCreatedAt(Instant.now());
        resetToken.setExpiresAt(Instant.now().plusSeconds(60));
        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(resetToken));
        when(sessionRepository.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.confirmPasswordReset(new PasswordResetConfirmRequest("raw-token", "new-password123!"));

        assertThat(response.token()).isNotBlank();
        assertThat(passwordHasher.matches("new-password123!", user.getPasswordSalt(), user.getPasswordHash())).isTrue();
        assertThat(resetToken.getUsedAt()).isNotNull();
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
