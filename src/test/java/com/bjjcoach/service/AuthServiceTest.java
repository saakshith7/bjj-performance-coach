package com.bjjcoach.service;

import com.bjjcoach.dto.AuthResponse;
import com.bjjcoach.dto.LoginRequest;
import com.bjjcoach.dto.RegisterRequest;
import com.bjjcoach.exception.DuplicateResourceException;
import com.bjjcoach.model.User;
import com.bjjcoach.repository.UserRepository;
import com.bjjcoach.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {

        registerRequest = new RegisterRequest();
        registerRequest.setName("Ryu Hayabusa");
        registerRequest.setEmail("ryu@bjj.com");
        registerRequest.setPassword("ninja123");
        registerRequest.setBelt("purple");
        registerRequest.setWeightKg(new java.math.BigDecimal("75.5"));
        registerRequest.setAge(26);
        registerRequest.setTrainingDaysPerWeek(5);
        registerRequest.setFitnessLevel("intermediate");
        registerRequest.setGoal("competition");

        savedUser = User.builder()
                .id("user-123")
                .name("Ryu Hayabusa")
                .email("ryu@bjj.com")
                .passwordHash("encoded-password")
                .belt("purple")
                .fitnessLevel("intermediate")
                .goal("competition")
                .build();
    }

    // Register Tests

    @Test
    @DisplayName("Should register user successfully when email is not taken")
    void shouldRegisterUserSuccessfully(){
        // given
        when(userRepository.existsByEmail("ryu@bjj.com")).thenReturn(false);
        when(passwordEncoder.encode("ninja123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken("ryu@bjj.com")).thenReturn("jwt-token-123");

        //when
        AuthResponse response = authService.register(registerRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getName()).isEqualTo("Ryu Hayabusa");
        assertThat(response.getEmail()).isEqualTo("ryu@bjj.com");
        assertThat(response.getBelt()).isEqualTo("purple");

        verify(userRepository).existsByEmail("ryu@bjj.com");
        verify(passwordEncoder).encode("ninja123");
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken("ryu@bjj.com");

    }

    @Test
    @DisplayName("Should throw duplicateResourceException when email already exist")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // given
        when(userRepository.existsByEmail("ryu@bjj.com")).thenReturn(true);

        //when / then
        assertThatThrownBy(()-> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository).existsByEmail("ryu@bjj.com");
        verify(userRepository, never()).save(any());
        verify(jwtUtil,never()).generateToken(any());
    }

    @Test
    @DisplayName("Should never store plain text password")
    void shouldNeverStorePlainTextPassword() {
        // given
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(any())).thenReturn("token");

        //when
        authService.register(registerRequest);

        //then - verify that the saved user never has raw password
        verify(userRepository).save(argThat(user ->
                !user.getPasswordHash().equals("ninja123") &&
                user.getPasswordHash().equals("encoded-password")
        ));
    }

    // login tests

    @Test
    @DisplayName("Should login successfully with correct credentials")
    void shouldLoginSuccessfully() {
        //given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("ryu@bjj.com");
        loginRequest.setPassword("ninja123");

        when(userRepository.findByEmail("ryu@bjj.com"))
                .thenReturn(Optional.of(savedUser));
        when(jwtUtil.generateToken("ryu@bjj.com")).thenReturn("jwt-token-123");

        //when
        AuthResponse response = authService.login(loginRequest);

        //then
        assertThat(response.getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getEmail()).isEqualTo("ryu@bjj.com");
        verify(authManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class)
        );
    }
    @Test
    @DisplayName("Should throw exception when credentials are wrong ")
    void shouldThrowExceptionOnBadCredentials() {
        // given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("ryu@gmail.com");
        loginRequest.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authManager).authenticate(any());

        //when / then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtUtil, never()).generateToken(any());
    }
}
