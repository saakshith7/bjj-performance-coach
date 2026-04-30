package com.bjjcoach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionStatResponse {
    private String position;
    private int totalOccurrences;
    private int losses;
    private int wins;
    private int neutral;
    private Double lossRate;
    private String severity; // HIGH, MEDIUM, LOW, OK

}
