package com.bjjcoach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeaknessReportResponse {

    private Instant generatedAt;
    private int totalSessionsAnalyzed;
    private List<PositionStatResponse> weakPositions;
    private List<TechniqueStatResponse> weakTechniques;
    private RoleAnalysisResponse roleAnalysis;
    private boolean cardioFlag;
    private List<String> recommendations;
}
