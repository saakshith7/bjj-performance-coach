package com.bjjcoach.controller;

import com.bjjcoach.dto.TechniqueLogRequest;
import com.bjjcoach.dto.TechniqueLogResponse;
import com.bjjcoach.service.TechniqueLogService;
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
public class TechniqueLogController {

    private final TechniqueLogService techniqueLogService;

    @PostMapping("/{sessionId}/techniques")
    public ResponseEntity<TechniqueLogResponse> logTechnique(
            @PathVariable String sessionId,
            @Valid @RequestBody TechniqueLogRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                techniqueLogService.logTechnique(sessionId, req, userDetails.getUsername())
        );
    }

    @GetMapping("/{sessionId}/techniques")
    public ResponseEntity<List<TechniqueLogResponse>> getTechniques(
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                techniqueLogService.getTechniquesForSession(sessionId, userDetails.getUsername())
        );
    }
}