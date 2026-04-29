package com.bjjcoach.service;

import com.bjjcoach.dto.SessionRequest;
import com.bjjcoach.dto.SessionResponse;
import com.bjjcoach.entity.Session;
import com.bjjcoach.model.User;
import com.bjjcoach.repository.SessionRepository;
import com.bjjcoach.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public SessionResponse createSession(SessionRequest req,String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Session session = Session.builder()
                .user(user)
                .sessionDate(req.getSessionDate())
                .durationMinutes(req.getDurationMinutes())
                .sessionType(req.getSessionType())
                .energyLevel(req.getEnergyLevel())
                .notes(req.getNotes())
                .build();

        Session saved = sessionRepository.save(session);
        return toResponse(saved);

    }

    public List<SessionResponse> getMySessions(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return sessionRepository
                .findByUserIdOrderBySessionDateDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public SessionResponse getSessionIDBy(String sessionId,String email){
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if(!session.getUser().getEmail().equals(email)){
            throw new RuntimeException("unauthorized");
        }

        return toResponse(session);
    }

    private SessionResponse toResponse(Session session){
        return SessionResponse.builder()
                .id(session.getId())
                .sessionDate(session.getSessionDate())
                .durationMinutes(session.getDurationMinutes())
                .sessionType(session.getSessionType())
                .energyLevel(session.getEnergyLevel())
                .notes(session.getNotes())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
