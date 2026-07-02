package com.sa.trk.nexon.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchResponseDto {

    @JsonProperty("match")
    private List<MatchDto> match = new ArrayList<>();
}