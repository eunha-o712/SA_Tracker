package com.sa.trk.clan.dto;

import java.util.List;

public record ClanTeamBalanceRequest(List<Long> memberIds, Integer teamSize, Integer variant) {
}
