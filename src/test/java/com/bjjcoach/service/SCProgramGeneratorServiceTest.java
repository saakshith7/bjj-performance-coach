package com.bjjcoach.service;

import com.bjjcoach.dto.*;
import com.bjjcoach.entity.*;
import com.bjjcoach.exception.ResourceNotFoundException;
import com.bjjcoach.model.User;
import com.bjjcoach.repository.*;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SCProgramGeneratorService Tests")
class SCProgramGeneratorServiceTest {

    @Mock private SCProgramRepository programRepository;
    @Mock private UserRepository userRepository;
    @Mock private WeaknessAnalyzerService analyzerService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private SCProgramGeneratorService programService;

    private User testUser;
    private WeaknessReportResponse mockReport;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-123")
                .email("ryu@bjj.com")
                .name("Ryu Hayabusa")
                .fitnessLevel("intermediate")
                .build();

        // Build a mock weakness report
        PositionStatResponse weakGuard = PositionStatResponse.builder()
                .position("guard")
                .totalOccurrences(10)
                .losses(8)
                .wins(2)
                .lossRate(80.0)
                .severity("HIGH")
                .build();

        TechniqueStatResponse weakArmbar = TechniqueStatResponse.builder()
                .technique("armbar")
                .category("submission")
                .totalAttempts(5)
                .successes(0)
                .failures(5)
                .successRate(0.0)
                .severity("HIGH")
                .build();

        RoleAnalysisResponse roleAnalysis = RoleAnalysisResponse.builder()
                .topLossRate(10.0)
                .bottomLossRate(80.0)
                .weakerRole("bottom")
                .summary("Your bottom game is a significant weakness.")
                .build();

        mockReport = WeaknessReportResponse.builder()
                .generatedAt(Instant.now())
                .totalSessionsAnalyzed(3)
                .weakPositions(List.of(weakGuard))
                .weakTechniques(List.of(weakArmbar))
                .roleAnalysis(roleAnalysis)
                .cardioFlag(true)
                .recommendations(List.of("Work on guard retention"))
                .build();
    }

    // ─── Generate Program Tests ───────────────────────────────────────────

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("unknown@bjj.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                programService.generateProgram("unknown@bjj.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should generate program with 3 training days")
    void shouldGenerateProgramWithThreeDays() throws Exception {
        // given
        when(userRepository.findByEmail("ryu@bjj.com"))
                .thenReturn(Optional.of(testUser));
        when(analyzerService.analyseAndGenerate("ryu@bjj.com"))
                .thenReturn(mockReport);
        when(programRepository.findLatestByUserId("user-123"))
                .thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any()))
                .thenReturn("[]");

        SCProgram savedProgram = SCProgram.builder()
                .id("program-123")
                .user(testUser)
                .generatedAt(Instant.now())
                .durationWeeks(8)
                .fitnessLevel("intermediate")
                .targetWeaknesses("[]")
                .weeklySchedule("[]")
                .build();

        when(programRepository.save(any(SCProgram.class)))
                .thenReturn(savedProgram);

        // when
        SCProgramResponse response =
                programService.generateProgram("ryu@bjj.com");

        // then
        assertThat(response).isNotNull();
        assertThat(response.getWeeklySchedule()).hasSize(3);
        assertThat(response.getWeeklySchedule())
                .extracting(DayProgramDTO::getDay)
                .containsExactly("Monday", "Wednesday", "Friday");
    }

    @Test
    @DisplayName("Should set duration to 8 weeks")
    void shouldSetDurationToEightWeeks() throws Exception {
        when(userRepository.findByEmail("ryu@bjj.com"))
                .thenReturn(Optional.of(testUser));
        when(analyzerService.analyseAndGenerate("ryu@bjj.com"))
                .thenReturn(mockReport);
        when(programRepository.findLatestByUserId("user-123"))
                .thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        SCProgram savedProgram = SCProgram.builder()
                .id("program-123")
                .user(testUser)
                .generatedAt(Instant.now())
                .durationWeeks(8)
                .fitnessLevel("intermediate")
                .targetWeaknesses("[]")
                .weeklySchedule("[]")
                .build();

        when(programRepository.save(any())).thenReturn(savedProgram);

        // when
        SCProgramResponse response =
                programService.generateProgram("ryu@bjj.com");

        // then
        assertThat(response.getDurationWeeks()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should include cardio in target weaknesses when cardio flag is true")
    void shouldIncludeCardioWhenCardioFlagTrue() throws Exception {
        when(userRepository.findByEmail("ryu@bjj.com"))
                .thenReturn(Optional.of(testUser));
        when(analyzerService.analyseAndGenerate("ryu@bjj.com"))
                .thenReturn(mockReport); // cardioFlag = true
        when(programRepository.findLatestByUserId("user-123"))
                .thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        SCProgram savedProgram = SCProgram.builder()
                .id("program-123")
                .user(testUser)
                .generatedAt(Instant.now())
                .durationWeeks(8)
                .fitnessLevel("intermediate")
                .targetWeaknesses("[]")
                .weeklySchedule("[]")
                .build();

        when(programRepository.save(any())).thenReturn(savedProgram);

        // when
        SCProgramResponse response =
                programService.generateProgram("ryu@bjj.com");

        // then
        assertThat(response.getTargetWeaknesses())
                .contains("cardio");
    }

    @Test
    @DisplayName("Should use first program message when no previous program exists")
    void shouldReturnFirstProgramMessageWhenNoPreviousProgram() throws Exception {
        when(userRepository.findByEmail("ryu@bjj.com"))
                .thenReturn(Optional.of(testUser));
        when(analyzerService.analyseAndGenerate("ryu@bjj.com"))
                .thenReturn(mockReport);
        when(programRepository.findLatestByUserId("user-123"))
                .thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        SCProgram savedProgram = SCProgram.builder()
                .id("program-123")
                .user(testUser)
                .generatedAt(Instant.now())
                .durationWeeks(8)
                .fitnessLevel("intermediate")
                .targetWeaknesses("[]")
                .weeklySchedule("[]")
                .build();

        when(programRepository.save(any())).thenReturn(savedProgram);

        // when
        SCProgramResponse response =
                programService.generateProgram("ryu@bjj.com");

        // then
        assertThat(response.getComparisonWithPrevious())
                .contains("first generated program");
    }

    @Test
    @DisplayName("Should use user fitness level from profile")
    void shouldUseFitnessLevelFromUserProfile() throws Exception {
        // given — user is beginner
        User beginnerUser = User.builder()
                .id("user-456")
                .email("beginner@bjj.com")
                .fitnessLevel("beginner")
                .build();

        when(userRepository.findByEmail("beginner@bjj.com"))
                .thenReturn(Optional.of(beginnerUser));
        when(analyzerService.analyseAndGenerate("beginner@bjj.com"))
                .thenReturn(mockReport);
        when(programRepository.findLatestByUserId("user-456"))
                .thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        SCProgram savedProgram = SCProgram.builder()
                .id("program-456")
                .user(beginnerUser)
                .generatedAt(Instant.now())
                .durationWeeks(8)
                .fitnessLevel("beginner")
                .targetWeaknesses("[]")
                .weeklySchedule("[]")
                .build();

        when(programRepository.save(any())).thenReturn(savedProgram);

        // when
        SCProgramResponse response =
                programService.generateProgram("beginner@bjj.com");

        // then
        assertThat(response.getFitnessLevel()).isEqualTo("beginner");
    }

    // ─── Fetch Tests ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw ResourceNotFoundException when no program exists for getLatest")
    void shouldThrowWhenNoProgramExistsForLatest() {
        when(userRepository.findByEmail("ryu@bjj.com"))
                .thenReturn(Optional.of(testUser));
        when(programRepository.findLatestByUserId("user-123"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                programService.getLatestProgram("ryu@bjj.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No program found");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for invalid program ID")
    void shouldThrowForInvalidProgramId() {
        when(programRepository.findById("invalid-id"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                programService.getProgramById("invalid-id", "ryu@bjj.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Program not found");
    }
}
