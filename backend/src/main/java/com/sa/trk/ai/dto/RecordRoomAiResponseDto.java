package com.sa.trk.ai.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class RecordRoomAiResponseDto {

    private String userName;
    private String summary;
    private String playStyle;
    private List<String> strengths = new ArrayList<>();
    private List<String> risks = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    private String model;
    private Instant generatedAt;
    private Integer sampleSize;
    private boolean aiGenerated = true;
}
