package com.sa.trk.clan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.clan.entity.ClanMember;
import com.sa.trk.clan.repository.ClanMemberRepository;
import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.repository.AuthUserRepository;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserBasicDto;
import com.sa.trk.player.service.PlayerService;

class ClanMemberServiceTests {

    @Mock
    private ClanMemberRepository clanMemberRepository;

    @Mock
    private PlayerService playerService;

    @Mock
    private AuthUserRepository authUserRepository;

    private ClanMemberService clanMemberService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clanMemberService = new ClanMemberService(clanMemberRepository, playerService, authUserRepository);
    }

    @Test
    void validatesAndStoresAClanMember() {
        when(clanMemberRepository.findByOwnerIdAndUserNameIgnoreCase(7L, "원장")).thenReturn(Optional.empty());
        when(authUserRepository.getReferenceById(7L)).thenReturn(owner(7L));
        OuidResponseDto ouid = new OuidResponseDto();
        ouid.setOuid("ouid-1");
        UserBasicDto basic = new UserBasicDto();
        basic.setUser_name("원장");
        basic.setClan_name("다봄");
        when(playerService.getOuid("원장")).thenReturn(ouid);
        when(playerService.getUserBasic("ouid-1")).thenReturn(basic);
        when(clanMemberRepository.save(any(ClanMember.class))).thenAnswer(invocation -> {
            ClanMember member = invocation.getArgument(0);
            member.setId(1L);
            return member;
        });

        var response = clanMemberService.addMember(7L, "  원장  ");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserName()).isEqualTo("원장");
        assertThat(response.getClanName()).isEqualTo("다봄");
    }

    @Test
    void returnsAnExistingMemberWithoutCallingNexonAgain() {
        ClanMember member = new ClanMember();
        member.setId(1L);
        member.setUserName("원장");
        member.setClanName("다봄");
        when(clanMemberRepository.findByOwnerIdAndUserNameIgnoreCase(7L, "원장")).thenReturn(Optional.of(member));

        var response = clanMemberService.addMember(7L, "원장");

        assertThat(response.getId()).isEqualTo(1L);
        verify(playerService, never()).getOuid("원장");
        verify(clanMemberRepository, never()).save(any(ClanMember.class));
    }

    @Test
    void rejectsPlayersWithoutAClan() {
        when(clanMemberRepository.findByOwnerIdAndUserNameIgnoreCase(7L, "무소속")).thenReturn(Optional.empty());
        OuidResponseDto ouid = new OuidResponseDto();
        ouid.setOuid("ouid-2");
        UserBasicDto basic = new UserBasicDto();
        basic.setUser_name("무소속");
        when(playerService.getOuid("무소속")).thenReturn(ouid);
        when(playerService.getUserBasic("ouid-2")).thenReturn(basic);

        assertThatThrownBy(() -> clanMemberService.addMember(7L, "무소속"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소속 클랜이 없는 플레이어는 등록할 수 없습니다.");
    }

    @Test
    void rejectsDeletingAMissingMember() {
        when(clanMemberRepository.findByIdAndOwnerId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clanMemberService.deleteMember(7L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("삭제할 클랜원을 찾을 수 없습니다.");
        verify(clanMemberRepository, never()).delete(any(ClanMember.class));
    }

    @Test
    void sameNicknameCanExistInDifferentUserRosters() {
        ClanMember first = new ClanMember();
        first.setId(1L);
        first.setUserName("원장");
        first.setClanName("다봄");
        ClanMember second = new ClanMember();
        second.setId(2L);
        second.setUserName("원장");
        second.setClanName("다봄");
        when(clanMemberRepository.findByOwnerIdAndUserNameIgnoreCase(7L, "원장")).thenReturn(Optional.of(first));
        when(clanMemberRepository.findByOwnerIdAndUserNameIgnoreCase(8L, "원장")).thenReturn(Optional.of(second));

        assertThat(clanMemberService.addMember(7L, "원장").getId()).isEqualTo(1L);
        assertThat(clanMemberService.addMember(8L, "원장").getId()).isEqualTo(2L);
    }

    private AuthUser owner(Long id) {
        AuthUser owner = new AuthUser();
        owner.setId(id);
        return owner;
    }
}
