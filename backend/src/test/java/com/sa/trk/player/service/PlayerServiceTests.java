package com.sa.trk.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.config.ClanTestProperties;
import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.UserBasicDto;
import com.sa.trk.nexon.service.NexonMetaCacheService;

class PlayerServiceTests {

    @Mock
    private NexonApiClient nexonApiClient;

    @Mock
    private NexonMetaCacheService nexonMetaCacheService;

    private ClanTestProperties clanTestProperties;
    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clanTestProperties = new ClanTestProperties();
        playerService = new PlayerService(nexonApiClient, nexonMetaCacheService, clanTestProperties);
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

    private UserBasicDto basic(String userName, String clanName) {
        UserBasicDto basic = new UserBasicDto();
        basic.setUser_name(userName);
        basic.setClan_name(clanName);
        return basic;
    }
}
