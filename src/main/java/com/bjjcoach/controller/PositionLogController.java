package com.bjjcoach.controller;

import com.bjjcoach.dto.PositionLogRequest;
import com.bjjcoach.dto.PositionLogResponse;
import com.bjjcoach.service.PositionLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class PositionLogController {

    private final PositionLogService positionLogService;

    @PostMapping("/{sessionId}/positions")
    public ResponseEntity<PositionLogResponse> logPosition(
            @PathVariable String sessionId,
            @Valid @RequestBody PositionLogRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                positionLogService.logPosition(sessionId, req, userDetails.getUsername())
        );
    }

    @GetMapping("/{sessionId}/positions")
    public ResponseEntity<List<PositionLogResponse>> getPositions(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                positionLogService.getPositionsForSession(sessionId, userDetails.getUsername())
        );
    }
}