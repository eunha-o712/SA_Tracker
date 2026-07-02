package com.sa.trk.nexon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchDetailItemDto {

    @JsonProperty("team_id")
    private String team_id;

    @JsonProperty("match_result")
    private String match_result;

    @JsonProperty("user_name")
    private String user_name;

    @JsonProperty("season_grade")
    private String season_grade;

    @JsonProperty("clan_name")
    private String clan_name;

    @JsonProperty("kill")
    private Integer kill;

    @JsonProperty("death")
    private Integer death;

    @JsonProperty("headshot")
    private Integer headshot;

    @JsonProperty("damage")
    private Double damage;

    @JsonProperty("assist")
    private Integer assist;
}