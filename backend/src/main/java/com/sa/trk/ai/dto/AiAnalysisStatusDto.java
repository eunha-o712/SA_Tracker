package com.sa.trk.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiAnalysisStatusDto {

    private String version;
    private boolean upToDate;
}
