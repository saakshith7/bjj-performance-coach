package com.bjjcoach.service;


import com.bjjcoach.dto.PositionLogRequest;
import com.bjjcoach.dto.PositionLogResponse;
import com.bjjcoach.entity.PositionLog;
import com.bjjcoach.entity.Session;
import com.bjjcoach.repository.PositionLogRepository;
import com.bjjcoach.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PositionLogService {

    private final PositionLogRepository positionLogRepository;
    private final SessionRepository sessionRepository;

    public @Nullable PositionLogResponse logPosition(String sessionId,
                                                     PositionLogRequest req,
                                                     String email){
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if(!session.getUser().getEmail().equals(email)){
            throw new RuntimeException("Unauthorised");
        }

        PositionLog log = PositionLog.builder()
                .session(session)
                .position(req.getPosition())
                .outcome(req.getOutcome())
                .role(req.getRole())
                .build();

        return toResponse(positionLogRepository.save(log));
    }

    public List<PositionLogResponse> getPositionsForSession(String sessionId,
                                                            String email){
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(()-> new RuntimeException("Session not found"));

        if(!session.getUser().getEmail().equals(email)){
            throw new RuntimeException("unauthorised");
        }
        return positionLogRepository.findBySessionId(sessionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PositionLogResponse toResponse(PositionLog log){
        return PositionLogResponse.builder()
                .id(log.getId())
                .sessionId(log.getSession().getId())
                .position(log.getPosition())
                .outcome(log.getOutcome())
                .role(log.getRole())
                .build();
    }
}
