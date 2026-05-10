package com.bjjcoach.controller;

import com.bjjcoach.dto.AIRecommendationResponse;
import com.bjjcoach.dto.PositionStatResponse;
import com.bjjcoach.dto.TechniqueStatResponse;
import com.bjjcoach.dto.WeaknessReportResponse;
import com.bjjcoach.exception.ResourceNotFoundException;
import com.bjjcoach.model.User;
import com.bjjcoach.repository.UserRepository;
import com.bjjcoach.service.GroqService;
import com.bjjcoach.service.WeaknessAnalyzerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Autowired
    private GroqService groqService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WeaknessAnalyzerService weaknessAnalyzerService; // Add this!

    @PostMapping("/ai-recommendations")
    public ResponseEntity<AIRecommendationResponse> getAIRecommendation(
            @AuthenticationPrincipal UserDetails userDetails) {

        // get weakness report
        WeaknessReportResponse report =
                analyzerService.analyseAndGenerate(userDetails.getUsername());

        // Get user profile for context
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));

        //Call gemini
        String aiRecommendation =
                groqService.generatePersonalRecommendations(report,user);

        return ResponseEntity.ok(
                AIRecommendationResponse.builder()
                        .recommendations(aiRecommendation)
                        .generatedAt(java.time.Instant.now())
                        .model("llama-3.1-8b-instant")
                        .basedOn(report)
                        .build()
        );
    }

}
