package com.bjjcoach.service;

import com.bjjcoach.dto.SessionRequest;
import com.bjjcoach.dto.SessionResponse;
import com.bjjcoach.entity.Session;
import com.bjjcoach.exception.ResourceNotFoundException;
import com.bjjcoach.exception.UnauthorizedException;
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: "+ email));

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
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email:"+ email));

        return sessionRepository
                .findByUserIdOrderBySessionDateDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public SessionResponse getSessionIDBy(String sessionId,String email){
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with ID:"+ sessionId));

        if(!session.getUser().getEmail().equals(email)){
            throw new UnauthorizedException("You do not have access to this session");
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
