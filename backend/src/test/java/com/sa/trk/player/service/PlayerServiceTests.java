package com.sa.trk.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.auth.entity.AuthUser;
import com.sa.trk.auth.repository.AuthUserRepository;
import com.sa.trk.config.ClanTestProperties;
import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.UserBasicDto;
import com.sa.trk.nexon.dto.UserRankDto;
import com.sa.trk.nexon.service.NexonMetaCacheService;

class PlayerServiceTests {

    @Mock
    private NexonApiClient nexonApiClient;

    @Mock
    private NexonMetaCacheService nexonMetaCacheService;

    @Mock
    private AuthUserRepository authUserRepository;

    private ClanTestProperties clanTestProperties;
    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clanTestProperties = new ClanTestProperties();
        playerService = new PlayerService(
                nexonApiClient,
                nexonMetaCacheService,
                clanTestProperties,
                authUserRepository
        );
    }

    @Test
    void overridesClanForConfiguredLocalTestPlayer() {
        clanTestProperties.setEnabled(true);
        clanTestProperties.setUserName("힉");
        clanTestProperties.setClanName("다봄");
        when(nexonApiClient.getUserBasic("ouid-hik")).thenReturn(basic("힉", ""));

        UserBasicDto result = playerService.getUserBasic("ouid-hik");

        assertThat(result.getClan_name()).isEqualTo("다봄");
    }

    @Test
    void leavesOtherPlayersClanUntouched() {
        clanTestProperties.setEnabled(true);
        clanTestProperties.setUserName("힉");
        clanTestProperties.setClanName("다봄");
        when(nexonApiClient.getUserBasic("ouid-other")).thenReturn(basic("다른사람", "원래클랜"));

        UserBasicDto result = playerService.getUserBasic("ouid-other");

        assertThat(result.getClan_name()).isEqualTo("원래클랜");
    }

    @Test
    void leavesClanUntouchedWhenOverrideIsDisabled() {
        clanTestProperties.setEnabled(false);
        clanTestProperties.setUserName("힉");
        clanTestProperties.setClanName("다봄");
        when(nexonApiClient.getUserBasic("ouid-hik")).thenReturn(basic("힉", "원래클랜"));

        UserBasicDto result = playerService.getUserBasic("ouid-hik");

        assertThat(result.getClan_name()).isEqualTo("원래클랜");
    }

    @Test
    void favoritePlayerLoadsOnlyBasicAndRankData() {
        UserRankDto rank = new UserRankDto();
        rank.setGrade("grade");
        rank.setSeason_grade("season");
        when(nexonApiClient.getUserBasic("ouid-favorite")).thenReturn(basic("favorite", "clan"));
        when(nexonApiClient.getUserRank("ouid-favorite")).thenReturn(rank);
        when(nexonMetaCacheService.findGradeImage("grade")).thenReturn("grade.png");
        when(nexonMetaCacheService.findSeasonGradeImage("season")).thenReturn("season.png");

        var result = playerService.getFavoritePlayerByOuid("ouid-favorite");

        assertThat(result.getUserName()).isEqualTo("favorite");
        assertThat(result.getOuid()).isEqualTo("ouid-favorite");
        assertThat(result.getImages().getSeasonGradeImage()).isEqualTo("season.png");
        assertThat(result.getTier()).isNull();
        assertThat(result.getRecent()).isNull();
        verify(nexonApiClient, never()).getUserTier("ouid-favorite");
        verify(nexonApiClient, never()).getUserRecentInfo("ouid-favorite");
    }

    @Test
    void includesLinkedSatrkProfileImageInPlayerResponses() {
        UserRankDto rank = new UserRankDto();
        AuthUser user = new AuthUser();
        user.setOuid("ouid-profile");
        user.setProfileImageUrl("/api/profile-images/123e4567-e89b-12d3-a456-426614174000.png");
        when(nexonApiClient.getUserBasic("ouid-profile")).thenReturn(basic("profile", "clan"));
        when(nexonApiClient.getUserRank("ouid-profile")).thenReturn(rank);
        when(authUserRepository.findByOuid("ouid-profile")).thenReturn(java.util.Optional.of(user));

        var result = playerService.getFavoritePlayerByOuid("ouid-profile");

        assertThat(result.getProfileImageUrl()).isEqualTo(user.getProfileImageUrl());
    }

    private UserBasicDto basic(String userName, String clanName) {
        UserBasicDto basic = new UserBasicDto();
        basic.setUser_name(userName);
        basic.setClan_name(clanName);
        return basic;
    }
}
