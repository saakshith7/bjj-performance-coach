package com.bjjcoach.controller;

import com.bjjcoach.dto.PositionStatResponse;
import com.bjjcoach.dto.TechniqueStatResponse;
import com.bjjcoach.dto.WeaknessReportResponse;
import com.bjjcoach.service.WeaknessAnalyzerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final WeaknessAnalyzerService analyzerService;

    @GetMapping("/weakness-report")
    public ResponseEntity<WeaknessReportResponse> generateReport(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                analyzerService.analyseAndGenerate(userDetails.getUsername())
        );
    }

    @GetMapping("/position-stats")
    public ResponseEntity<List<PositionStatResponse>> getPositionStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                analyzerService.getPositionStats(userDetails.getUsername())
        );
    }

    @GetMapping("/technique-stats")
    public ResponseEntity<List<TechniqueStatResponse>> getTechniqueStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                analyzerService.getTechniqueStats(userDetails.getUsername())
        );
    }



}
