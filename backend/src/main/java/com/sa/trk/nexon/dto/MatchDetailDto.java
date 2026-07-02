package com.sa.trk.nexon.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchDetailDto {

    @JsonProperty("match_id")
    private String match_id;

    @JsonProperty("match_type")
    private String match_type;

    @JsonProperty("match_mode")
    private String match_mode;

    @JsonProperty("date_match")
    private String date_match;

    @JsonProperty("match_map")
    private String match_map;

    @JsonProperty("match_detail")
    private List<MatchDetailItemDto> match_detail = new ArrayList<>();
}