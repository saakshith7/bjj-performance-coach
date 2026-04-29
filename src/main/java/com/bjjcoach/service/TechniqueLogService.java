package com.bjjcoach.service;

import com.bjjcoach.dto.TechniqueLogRequest;
import com.bjjcoach.dto.TechniqueLogResponse;
import com.bjjcoach.entity.Session;
import com.bjjcoach.entity.TechniqueLog;
import com.bjjcoach.repository.SessionRepository;
import com.bjjcoach.repository.TechniqueLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TechniqueLogService {

    private final TechniqueLogRepository techniqueLogRepository;
    private final SessionRepository sessionRepository;

    public TechniqueLogResponse logTechnique(String sessionId,
                                             TechniqueLogRequest req,
                                             String email) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }

        TechniqueLog log = TechniqueLog.builder()
                .session(session)
                .technique(req.getTechnique())
                .category(req.getCategory())
                .success(req.getSuccess())
                .notes(req.getNotes())
                .build();

        return toResponse(techniqueLogRepository.save(log));
    }

    public List<TechniqueLogResponse> getTechniquesForSession(String sessionId,
                                                              String email) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }

        return techniqueLogRepository.findBySessionId(sessionId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private TechniqueLogResponse toResponse(TechniqueLog log) {
        return TechniqueLogResponse.builder()
                .id(log.getId())
                .sessionId(log.getSession().getId())
                .technique(log.getTechnique())
                .category(log.getCategory())
                .success(log.getSuccess())
                .notes(log.getNotes())
                .build();
    }
}