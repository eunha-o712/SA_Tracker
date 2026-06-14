package com.sa.trk.nexon.dto;

import java.util.List;

import lombok.Data;

@Data
public class MatchResponseDto {

    private List<MatchDto> match;
}