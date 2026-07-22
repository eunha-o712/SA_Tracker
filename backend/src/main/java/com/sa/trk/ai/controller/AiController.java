package com.sa.trk.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.ai.dto.AiAnalysisStatusDto;
import com.sa.trk.ai.dto.RecordRoomAiResponseDto;
import com.sa.trk.ai.service.RecordRoomAiService;

@RestController
public class AiController {

    private final RecordRoomAiService recordRoomAiService;

    public AiController(RecordRoomAiService recordRoomAiService) {
        this.recordRoomAiService = recordRoomAiService;
    }

    @GetMapping("/api/ai/record-room")
    public RecordRoomAiResponseDto getRecordRoomAnalysis(@RequestParam("userName") String userName) {
        return recordRoomAiService.analyze(userName);
    }

    @GetMapping("/api/ai/record-room/status")
    public AiAnalysisStatusDto getRecordRoomAnalysisStatus(
            @RequestParam("userName") String userName) {
        return recordRoomAiService.getStatus(userName);
    }
}
