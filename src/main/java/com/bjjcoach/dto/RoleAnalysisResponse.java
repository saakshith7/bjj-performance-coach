package com.bjjcoach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleAnalysisResponse {
    private double topLossRate;
    private double bottomLossRate;
    private String weakerRole;
    private String summary;
}
