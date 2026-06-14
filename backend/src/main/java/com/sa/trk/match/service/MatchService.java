package com.sa.trk.match.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sa.trk.match.dto.MatchDetailResponseDto;
import com.sa.trk.match.dto.MatchListResponseDto;
import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.MatchDetailDto;
import com.sa.trk.nexon.dto.MatchDto;
import com.sa.trk.nexon.dto.OuidResponseDto;

@Service
public class MatchService {

    private static final int PAGE_SIZE = 20;

    private final NexonApiClient nexonApiClient;

    public MatchService(NexonApiClient nexonApiClient) {
        this.nexonApiClient = nexonApiClient;
    }

    public MatchListResponseDto getMatches(String userName, Integer page) {
        OuidResponseDto ouidResponse = nexonApiClient.getOuid(userName);
        String ouid = ouidResponse.getOuid();

        List<MatchDto> allMatches = nexonApiClient.getMatches(ouid, "폭파미션", "퀵매치 클랜전");

        int totalCount = allMatches.size();
        int currentPage = (page == null || page < 1) ? 1 : page;

        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalCount);

        List<MatchDto> pagedMatches;
        if (fromIndex >= totalCount) {
            pagedMatches = Collections.emptyList();
        } else {
            pagedMatches = allMatches.subList(fromIndex, toIndex);
        }

        MatchListResponseDto responseDto = new MatchListResponseDto();
        responseDto.setUserName(userName);
        responseDto.setPage(currentPage);
        responseDto.setSize(PAGE_SIZE);
        responseDto.setTotalCount(totalCount);
        responseDto.setMatches(pagedMatches);

        return responseDto;
    }

    public MatchDetailResponseDto getMatchDetail(String matchId) {
        MatchDetailDto detailDto = nexonApiClient.getMatchDetail(matchId);

        MatchDetailResponseDto responseDto = new MatchDetailResponseDto();
        responseDto.setMatchId(detailDto.getMatch_id());
        responseDto.setMatchType(detailDto.getMatch_type());
        responseDto.setMatchMode(detailDto.getMatch_mode());
        responseDto.setDateMatch(detailDto.getDate_match());
        responseDto.setMatchMap(detailDto.getMatch_map());
        responseDto.setMatchDetail(detailDto.getMatch_detail());

        return responseDto;
    }
}