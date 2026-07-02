package com.sa.trk.nexon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchDto {

    @JsonProperty("match_id")
    private String match_id;

    @JsonProperty("match_type")
    private String match_type;

    @JsonProperty("match_mode")
    private String match_mode;

    @JsonProperty("date_match")
    private String date_match;

    @JsonProperty("match_result")
    private String match_result;

    @JsonProperty("kill")
    private Integer kill;

    @JsonProperty("death")
    private Integer death;

    @JsonProperty("assist")
    private Integer assist;

    /*
     * 매치 목록 API에는 없고, 맵 필터에서 상세 조회한 경우에만 채워집니다.
     */
    private String match_map;
}