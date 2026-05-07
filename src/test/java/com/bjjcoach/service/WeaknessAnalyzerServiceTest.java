package com.bjjcoach.service;

import com.bjjcoach.dto.*;
import com.bjjcoach.entity.*;
import com.bjjcoach.exception.ResourceNotFoundException;
import com.bjjcoach.model.User;
import com.bjjcoach.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(
        strictness = org.mockito.quality.Strictness.LENIENT
)
@DisplayName("WeaknessAnalyzerService Tests")
class WeaknessAnalyzerServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private PositionLogRepository positionLogRepository;
    @Mock private TechniqueLogRepository techniqueLogRepository;
    @Mock private WeaknessReportRepository weaknessReportRepository;

    @InjectMocks
    private WeaknessAnalyzerService analyzerService;

    private User testUser;
    private Session testSession;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-123")
                .email("ryu@bjj.com")
                .name("Ryu Hayabusa")
                .belt("purple")
                .fitnessLevel("intermediate")
                .build();

        testSession = new Session();
        testSession.setId("session-123");
        testSession.setUser(testUser);
        testSession.setSessionDate(LocalDate.of(2026, 4, 28));
        testSession.setEnergyLevel(4);
        testSession.setSessionType("gi");
    }

    // ─── Helper methods ───────────────────────────────────────────────────

    private PositionLog makePositionLog(String position,
                                        String outcome,
                                        String role) {
        PositionLog log = new PositionLog();
        log.setId(java.util.UUID.randomUUID().toString());
        log.setSession(testSession);
        log.setPosition(position);
        log.setOutcome(outcome);
        log.setRole(role);
        return log;
    }

    private TechniqueLog makeTechniqueLog(String technique,
                                          String category,
                                          boolean success) {
        TechniqueLog log = new TechniqueLog();
        log.setId(java.util.UUID.randomUUID().toString());
        log.setSession(testSession);
        log.setTechnique(technique);
        log.setCategory(category);
        log.setSuccess(success);
        return log;
    }

    private void mockUserAndSessions(List<Session> sessions) {
        when(userRepository.findByEmail("ryu@bjj.com"))
                .thenReturn(Optional.of(testUser));
        when(sessionRepository.findByUserIdOrderBySessionDateDesc("user-123"))
                .thenReturn(sessions);
    }

    // ─── User not found ───────────────────────────────────────────────────

    @Nested
    @DisplayName("User validation")
    class UserValidation {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByEmail("unknown@bjj.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    analyzerService.analyseAndGenerate("unknown@bjj.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when no sessions logged")
        void shouldThrowWhenNoSessionsExist() {
            when(userRepository.findByEmail("ryu@bjj.com"))
                    .thenReturn(Optional.of(testUser));
            when(sessionRepository.findByUserIdOrderBySessionDateDesc("user-123"))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() ->
                    analyzerService.analyseAndGenerate("ryu@bjj.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("No sessions found");
        }
    }

    // ─── Position Analysis Tests ──────────────────────────────────────────

    @Nested
    @DisplayName("Position analysis")
    class PositionAnalysis {

        @Test
        @DisplayName("Should flag guard as HIGH severity when loss rate >= 70%")
        void shouldFlagGuardAsHighSeverity() {
            // given — 7 losses out of 10 = 70% loss rate — should be HIGH
            List<PositionLog> positions = List.of(
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "won", "bottom"),
                    makePositionLog("guard", "won", "bottom"),
                    makePositionLog("guard", "won", "bottom")
            );

            // Stub directly instead of using mockUserAndSessions helper
            // because analyzeAndGenerate calls userRepository twice internally
            when(userRepository.findByEmail("ryu@bjj.com"))
                    .thenReturn(Optional.of(testUser));
            when(sessionRepository.findByUserIdOrderBySessionDateDesc("user-123"))
                    .thenReturn(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(positions);
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");


            // then
            assertThat(report.getWeakPositions())
                    .isNotEmpty();

            PositionStatResponse guardStat = report.getWeakPositions().stream()
                    .filter(p -> "guard".equals(p.getPosition()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Guard not found in weak positions. All weak positions: "
                                    + report.getWeakPositions()
                    ));

            assertThat(guardStat.getLossRate()).isEqualTo(70.0);
            assertThat(guardStat.getSeverity()).isEqualTo("HIGH");
            assertThat(guardStat.getLosses()).isEqualTo(7);
            assertThat(guardStat.getWins()).isEqualTo(3);
            assertThat(guardStat.getTotalOccurrences()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should flag position as MEDIUM severity when loss rate is 55-69%")
        void shouldFlagPositionAsMediumSeverity() {
            // given — 6 losses out of 10 = 60% loss rate
            List<PositionLog> positions = List.of(
                    makePositionLog("half_guard", "lost", "bottom"),
                    makePositionLog("half_guard", "lost", "bottom"),
                    makePositionLog("half_guard", "lost", "bottom"),
                    makePositionLog("half_guard", "lost", "bottom"),
                    makePositionLog("half_guard", "lost", "bottom"),
                    makePositionLog("half_guard", "lost", "bottom"),
                    makePositionLog("half_guard", "won", "bottom"),
                    makePositionLog("half_guard", "won", "bottom"),
                    makePositionLog("half_guard", "won", "bottom"),
                    makePositionLog("half_guard", "won", "bottom")
            );

            mockUserAndSessions(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(positions);
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then
            PositionStatResponse halfGuardStat = report.getWeakPositions().stream()
                    .filter(p -> "half_guard".equals(p.getPosition()))
                    .findFirst()
                    .orElseThrow();

            assertThat(halfGuardStat.getSeverity()).isEqualTo("MEDIUM");
            assertThat(halfGuardStat.getLossRate()).isEqualTo(60.0);
        }

        @Test
        @DisplayName("Should return INSUFFICIENT_DATA when position logged fewer than 3 times")
        void shouldReturnInsufficientDataForFewOccurrences() {
            // given — only 2 occurrences
            List<PositionLog> positions = List.of(
                    makePositionLog("turtle", "lost", "bottom"),
                    makePositionLog("turtle", "lost", "bottom")
            );

            mockUserAndSessions(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(positions);
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then — turtle should not appear in weakPositions
            // because INSUFFICIENT_DATA is filtered out
            boolean turtleInWeak = report.getWeakPositions().stream()
                    .anyMatch(p -> "turtle".equals(p.getPosition()));

            assertThat(turtleInWeak).isFalse();
        }

        @Test
        @DisplayName("Should return OK severity when loss rate is below 40%")
        void shouldReturnOkSeverityForStrongPosition() {
            // given — 2 losses out of 10 = 20% loss rate
            List<PositionLog> positions = List.of(
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "lost", "top"),
                    makePositionLog("mount", "lost", "top")
            );

            // getPositionStats only needs user and positionLog — no sessionRepository
            when(userRepository.findByEmail("ryu@bjj.com"))
                    .thenReturn(Optional.of(testUser));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(positions);

            // when
            List<PositionStatResponse> stats =
                    analyzerService.getPositionStats("ryu@bjj.com");

            // then
            PositionStatResponse mountStat = stats.stream()
                    .filter(p -> "mount".equals(p.getPosition()))
                    .findFirst()
                    .orElseThrow();

            assertThat(mountStat.getSeverity()).isEqualTo("OK");
            assertThat(mountStat.getLossRate()).isEqualTo(20.0);
        }

        @Test
        @DisplayName("Should sort positions by loss rate descending — worst first")
        void shouldSortPositionsByLossRateDescending() {
            // given — guard 80% loss, mount 20% loss
            List<PositionLog> positions = List.of(
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "won", "bottom"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "lost", "top")
            );

            // getPositionStats only needs user and positionLog
            when(userRepository.findByEmail("ryu@bjj.com"))
                    .thenReturn(Optional.of(testUser));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(positions);

            // when
            List<PositionStatResponse> stats =
                    analyzerService.getPositionStats("ryu@bjj.com");

            // then — guard should appear before mount
            int guardIndex = -1;
            int mountIndex = -1;
            for (int i = 0; i < stats.size(); i++) {
                if ("guard".equals(stats.get(i).getPosition())) guardIndex = i;
                if ("mount".equals(stats.get(i).getPosition())) mountIndex = i;
            }

            assertThat(guardIndex).isLessThan(mountIndex);
        }

    // ─── Technique Analysis Tests ─────────────────────────────────────────

    @Nested
    @DisplayName("Technique analysis")
    class TechniqueAnalysis {

        @Test
        @DisplayName("Should flag armbar as HIGH severity when success rate <= 20%")
        void shouldFlagArmbarAsHighSeverity() {
            // given — 0 successes out of 5 attempts = 0% success rate
            List<TechniqueLog> techniques = List.of(
                    makeTechniqueLog("armbar", "submission", false),
                    makeTechniqueLog("armbar", "submission", false),
                    makeTechniqueLog("armbar", "submission", false),
                    makeTechniqueLog("armbar", "submission", false),
                    makeTechniqueLog("armbar", "submission", false)
            );

            mockUserAndSessions(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(techniques);

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then
            TechniqueStatResponse armbarStat = report.getWeakTechniques().stream()
                    .filter(t -> "armbar".equals(t.getTechnique()))
                    .findFirst()
                    .orElseThrow();

            assertThat(armbarStat.getSuccessRate()).isEqualTo(0.0);
            assertThat(armbarStat.getSeverity()).isEqualTo("HIGH");
            assertThat(armbarStat.getTotalAttempts()).isEqualTo(5);
            assertThat(armbarStat.getSuccesses()).isEqualTo(0);
            assertThat(armbarStat.getFailures()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should flag technique as MEDIUM when success rate is 21-35%")
        void shouldFlagTechniqueAsMediumSeverity() {
            // given — 1 success out of 4 = 25% success rate
            List<TechniqueLog> techniques = List.of(
                    makeTechniqueLog("scissor_sweep", "sweep", true),
                    makeTechniqueLog("scissor_sweep", "sweep", false),
                    makeTechniqueLog("scissor_sweep", "sweep", false),
                    makeTechniqueLog("scissor_sweep", "sweep", false)
            );

            mockUserAndSessions(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(techniques);

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then
            TechniqueStatResponse sweepStat = report.getWeakTechniques().stream()
                    .filter(t -> "scissor_sweep".equals(t.getTechnique()))
                    .findFirst()
                    .orElseThrow();

            assertThat(sweepStat.getSeverity()).isEqualTo("MEDIUM");
            assertThat(sweepStat.getSuccessRate()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("Should return OK when technique success rate is above 50%")
        void shouldReturnOkForSuccessfulTechnique() {
            // given — 4 successes out of 5 = 80% success rate
            List<TechniqueLog> techniques = List.of(
                    makeTechniqueLog("rear_naked_choke", "submission", true),
                    makeTechniqueLog("rear_naked_choke", "submission", true),
                    makeTechniqueLog("rear_naked_choke", "submission", true),
                    makeTechniqueLog("rear_naked_choke", "submission", true),
                    makeTechniqueLog("rear_naked_choke", "submission", false)
            );

            // getTechniqueStats only needs user and techniqueLog — no sessionRepository
            when(userRepository.findByEmail("ryu@bjj.com"))
                    .thenReturn(Optional.of(testUser));
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(techniques);

            // when
            List<TechniqueStatResponse> stats =
                    analyzerService.getTechniqueStats("ryu@bjj.com");

            // then
            TechniqueStatResponse rncStat = stats.stream()
                    .filter(t -> "rear_naked_choke".equals(t.getTechnique()))
                    .findFirst()
                    .orElseThrow();

            assertThat(rncStat.getSeverity()).isEqualTo("OK");
            assertThat(rncStat.getSuccessRate()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("Should return INSUFFICIENT_DATA when technique attempted fewer than 3 times")
        void shouldReturnInsufficientDataForFewAttempts() {
            // given — only 2 attempts
            List<TechniqueLog> techniques = List.of(
                    makeTechniqueLog("triangle", "submission", false),
                    makeTechniqueLog("triangle", "submission", false)
            );

            mockUserAndSessions(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(techniques);

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then — triangle should not appear in weakTechniques
            boolean triangleInWeak = report.getWeakTechniques().stream()
                    .anyMatch(t -> "triangle".equals(t.getTechnique()));

            assertThat(triangleInWeak).isFalse();
        }
    }

    // ─── Role Analysis Tests ──────────────────────────────────────────────

    @Nested
    @DisplayName("Role analysis")
    class RoleAnalysis {

        @Test
        @DisplayName("Should return insufficient_data when fewer than 5 top positions logged")
        void shouldReturnInsufficientDataForFewTopPositions() {
            // given — only 3 top positions
            List<PositionLog> positions = List.of(
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "lost", "top"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom")
            );

            mockUserAndSessions(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(positions);
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then
            assertThat(report.getRoleAnalysis().getWeakerRole())
                    .isEqualTo("insufficient_data");
            assertThat(report.getRoleAnalysis().getSummary())
                    .contains("Not enough data yet");
        }

        @Test
        @DisplayName("Should detect bottom game as weaker when bottom loss rate significantly higher")
        void shouldDetectBottomGameAsWeaker() {
            // given — bottom: 80% loss rate, top: 10% loss rate
            List<PositionLog> positions = List.of(
                    // bottom positions — 8 losses out of 10
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("half_guard", "lost", "bottom"),
                    makePositionLog("half_guard", "lost", "bottom"),
                    makePositionLog("half_guard", "lost", "bottom"),
                    makePositionLog("half_guard", "won", "bottom"),
                    makePositionLog("half_guard", "won", "bottom"),
                    // top positions — 1 loss out of 10
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("side_control", "won", "top"),
                    makePositionLog("side_control", "won", "top"),
                    makePositionLog("side_control", "won", "top"),
                    makePositionLog("side_control", "won", "top"),
                    makePositionLog("side_control", "lost", "top")
            );

            mockUserAndSessions(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(positions);
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then
            assertThat(report.getRoleAnalysis().getWeakerRole())
                    .isEqualTo("bottom");
            assertThat(report.getRoleAnalysis().getBottomLossRate())
                    .isGreaterThan(report.getRoleAnalysis().getTopLossRate());
        }

        @Test
        @DisplayName("Should detect balanced game when difference is less than 5%")
        void shouldDetectBalancedGame() {
            // given — top: 50% loss, bottom: 50% loss
            List<PositionLog> positions = List.of(
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "won", "bottom"),
                    makePositionLog("guard", "won", "bottom"),
                    makePositionLog("guard", "won", "bottom"),
                    makePositionLog("mount", "lost", "top"),
                    makePositionLog("mount", "lost", "top"),
                    makePositionLog("mount", "lost", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top"),
                    makePositionLog("mount", "won", "top")
            );

            mockUserAndSessions(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(positions);
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then
            assertThat(report.getRoleAnalysis().getWeakerRole())
                    .isEqualTo("balanced");
            assertThat(report.getRoleAnalysis().getSummary())
                    .contains("remarkably balanced");
        }
    }

    // ─── Cardio Analysis Tests ────────────────────────────────────────────

    @Nested
    @DisplayName("Cardio analysis")
    class CardioAnalysis {

        @Test
        @DisplayName("Should flag cardio when low energy sessions have 20%+ worse loss rate")
        void shouldFlagCardioWhenLowEnergySessionsAreWorse() {
            // given — high energy session (level 5) performs well
            Session highEnergySession = new Session();
            highEnergySession.setId("session-high");
            highEnergySession.setUser(testUser);
            highEnergySession.setSessionDate(LocalDate.of(2026, 4, 21));
            highEnergySession.setEnergyLevel(5);

            // given — low energy session (level 1) performs badly
            Session lowEnergySession = new Session();
            lowEnergySession.setId("session-low");
            lowEnergySession.setUser(testUser);
            lowEnergySession.setSessionDate(LocalDate.of(2026, 4, 23));
            lowEnergySession.setEnergyLevel(1);

            // high energy positions — mostly wins
            PositionLog highWin1 = makePositionLog("guard", "won", "bottom");
            highWin1.setSession(highEnergySession);
            PositionLog highWin2 = makePositionLog("guard", "won", "bottom");
            highWin2.setSession(highEnergySession);
            PositionLog highWin3 = makePositionLog("mount", "won", "top");
            highWin3.setSession(highEnergySession);
            PositionLog highLoss1 = makePositionLog("guard", "lost", "bottom");
            highLoss1.setSession(highEnergySession);

            // low energy positions — mostly losses
            PositionLog lowLoss1 = makePositionLog("guard", "lost", "bottom");
            lowLoss1.setSession(lowEnergySession);
            PositionLog lowLoss2 = makePositionLog("guard", "lost", "bottom");
            lowLoss2.setSession(lowEnergySession);
            PositionLog lowLoss3 = makePositionLog("mount", "lost", "top");
            lowLoss3.setSession(lowEnergySession);
            PositionLog lowWin1 = makePositionLog("mount", "won", "top");
            lowWin1.setSession(lowEnergySession);

            when(userRepository.findByEmail("ryu@bjj.com"))
                    .thenReturn(Optional.of(testUser));
            when(sessionRepository.findByUserIdOrderBySessionDateDesc("user-123"))
                    .thenReturn(List.of(highEnergySession, lowEnergySession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(List.of(highWin1, highWin2, highWin3, highLoss1,
                            lowLoss1, lowLoss2, lowLoss3, lowWin1));
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then
            assertThat(report.isCardioFlag()).isTrue();
            assertThat(report.getRecommendations())
                    .anyMatch(r -> r.contains("zone 2 cardio") ||
                            r.contains("Cardio"));
        }

        @Test
        @DisplayName("Should not flag cardio when all sessions have similar energy levels")
        void shouldNotFlagCardioWhenEnergyLevelsSimilar() {
            // given — all sessions energy level 4
            testSession.setEnergyLevel(4);

            List<PositionLog> positions = List.of(
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "won", "bottom"),
                    makePositionLog("mount", "won", "top")
            );

            mockUserAndSessions(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(positions);
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then — no low energy sessions exist so cardio flag cannot trigger
            assertThat(report.isCardioFlag()).isFalse();
        }
    }

    // ─── Recommendations Tests ────────────────────────────────────────────

    @Nested
    @DisplayName("Recommendations")
    class Recommendations {

        @Test
        @DisplayName("Should include guard recommendation when guard is weak")
        void shouldIncludeGuardRecommendationWhenGuardIsWeak() {
            // given — guard 80% loss rate
            List<PositionLog> positions = List.of(
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "won", "bottom")
            );

            mockUserAndSessions(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(positions);
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then
            assertThat(report.getRecommendations())
                    .anyMatch(r -> r.toLowerCase().contains("guard"));
        }

        @Test
        @DisplayName("Should include armbar recommendation when armbar success rate is low")
        void shouldIncludeArmbarRecommendation() {
            // given
            List<TechniqueLog> techniques = List.of(
                    makeTechniqueLog("armbar", "submission", false),
                    makeTechniqueLog("armbar", "submission", false),
                    makeTechniqueLog("armbar", "submission", false),
                    makeTechniqueLog("armbar", "submission", false),
                    makeTechniqueLog("armbar", "submission", false)
            );

            mockUserAndSessions(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(techniques);

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then
            assertThat(report.getRecommendations())
                    .anyMatch(r -> r.toLowerCase().contains("armbar"));
        }

        @Test
        @DisplayName("Should not be empty when weaknesses are detected")
        void recommendationsShouldNotBeEmptyWhenWeaknessesDetected() {
            // given
            List<PositionLog> positions = List.of(
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "lost", "bottom"),
                    makePositionLog("guard", "won", "bottom")
            );

            mockUserAndSessions(List.of(testSession));
            when(positionLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(positions);
            when(techniqueLogRepository.findBySession_User_Id("user-123"))
                    .thenReturn(Collections.emptyList());

            // when
            WeaknessReportResponse report =
                    analyzerService.analyseAndGenerate("ryu@bjj.com");

            // then
            assertThat(report.getRecommendations()).isNotEmpty();
        }
    }
}}