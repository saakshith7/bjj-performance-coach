package com.bjjcoach.controller;

import com.bjjcoach.dto.SCProgramResponse;
import com.bjjcoach.service.SCProgramGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
public class SCProgramController {
    private final SCProgramGeneratorService programService;

    @PostMapping("/generate")
    public ResponseEntity<SCProgramResponse> generate(
            @AuthenticationPrincipal UserDetails userDetails){
        return ResponseEntity.ok(
                programService.generateProgram(userDetails.getUsername())
        );
    }

    @GetMapping("/latest")
    public ResponseEntity<SCProgramResponse> getLastest(
            @AuthenticationPrincipal UserDetails userDetails){
        return ResponseEntity.ok(
                programService.getLatestProgram(userDetails.getUsername())
        );
    }

    @GetMapping
    public ResponseEntity<List<SCProgramResponse>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                programService.getAllPrograms(userDetails.getUsername())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SCProgramResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                programService.getProgramById(id, userDetails.getUsername())
        );
    }

}
