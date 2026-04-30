package com.bjjcoach.service;

import com.bjjcoach.dto.*;
import com.bjjcoach.exception.DuplicateResourceException;
import com.bjjcoach.model.User;
import com.bjjcoach.repository.UserRepository;
import com.bjjcoach.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("Email already in use" + req.getEmail());
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .belt(req.getBelt())
                .weightKg(BigDecimal.valueOf(req.getWeightKg()))
                .age(req.getAge())
                .trainingDaysPerWeek(req.getTrainingDaysPerWeek())
                .fitnessLevel(req.getFitnessLevel())
                .goal(req.getGoal())
                .build();

        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getBelt());
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        User user = userRepository.findByEmail(req.getEmail()).orElseThrow();
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getBelt());
    }
}