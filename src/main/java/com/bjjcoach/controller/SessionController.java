package com.bjjcoach.controller;

import com.bjjcoach.dto.SessionRequest;
import com.bjjcoach.dto.SessionResponse;
import com.bjjcoach.service.SessionService;
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
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
            @Valid @RequestBody SessionRequest req,
            @AuthenticationPrincipal UserDetails userDetails){
        return ResponseEntity.ok(
                sessionService.createSession(req, userDetails.getUsername())
        );
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getMySessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                sessionService.getMySessions(userDetails.getUsername())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSessionId(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails){
        return ResponseEntity.ok(
                sessionService.getSessionIDBy(id,userDetails.getUsername())
        );
    }



}
