package com.bjjcoach.service;

import com.bjjcoach.dto.PositionStatResponse;
import com.bjjcoach.dto.RoleAnalysisResponse;
import com.bjjcoach.dto.TechniqueStatResponse;
import com.bjjcoach.dto.WeaknessReportResponse;
import com.bjjcoach.entity.PositionLog;
import com.bjjcoach.entity.Session;
import com.bjjcoach.entity.TechniqueLog;
import com.bjjcoach.exception.ResourceNotFoundException;
import com.bjjcoach.model.User;
import com.bjjcoach.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WeaknessAnalyzerService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PositionLogRepository positionLogRepository;
    private final TechniqueLogRepository techniqueLogRepository;
    private final WeaknessReportRepository weaknessReportRepository;

    public WeaknessReportResponse analyseAndGenerate(String email){

        // Get User
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));

        // Get all sessions for user
        List<Session> sessions = sessionRepository
                .findByUserIdOrderBySessionDateDesc(user.getId());

        if(sessions.isEmpty()){
            throw new ResourceNotFoundException(
                    "No sessions found. Log at least one session before generating a report."
            );
        }

        //get all positions and techniques logs for user
        List<PositionLog> allPositions = positionLogRepository
                .findBySession_User_Id(user.getId());

        List<TechniqueLog> allTechniques = techniqueLogRepository
                .findBySession_User_Id(user.getId());

        // Run analysis
        List<PositionStatResponse> positionStats = analysePositions(allPositions);
        List<TechniqueStatResponse> techniqueStats = analyseTechniques(allTechniques);
        RoleAnalysisResponse roleAnalysis = analyseRole(allPositions);
        boolean cardioFlag = analyzeCardio(sessions, allPositions);
        List<String> recommendations = generateRecommendations(
                positionStats, techniqueStats, roleAnalysis, cardioFlag
        );

        // Weak areas only (severity HIGH or MEDIUM)
        List<PositionStatResponse> weakPositions = positionStats.stream()
                .filter(p -> p.getSeverity().equals("HIGH") || p.getSeverity().equals("MEDIUM"))
                .collect(Collectors.toList());

        List<TechniqueStatResponse> weakTechniques = techniqueStats.stream()
                .filter(t->t.getSeverity().equals("HIGH") || t.getSeverity().equals("MEDIUM"))
                .collect(Collectors.toList());

        return WeaknessReportResponse.builder()
                .generatedAt(java.time.Instant.now())
                .totalSessionsAnalyzed(sessions.size())
                .weakPositions(weakPositions)
                .weakTechniques(weakTechniques)
                .roleAnalysis(roleAnalysis)
                .cardioFlag(cardioFlag)
                .recommendations(recommendations)
                .build();
    }

    // positional Analysis

    private List<PositionStatResponse> analysePositions(List<PositionLog> logs){
        //group by position name
        Map<String, List<PositionLog>> byPosition = logs.stream()
                .collect(Collectors.groupingBy(PositionLog::getPosition));

        return byPosition.entrySet().stream()
                .map(entry->{
                    String position = entry.getKey();
                    List<PositionLog> posLogs = entry.getValue();

                    int total =  posLogs.size();
                    int losses = (int) posLogs.stream()
                            .filter(p-> "lost".equals(p.getOutcome())).count();
                    int wins = (int) posLogs.stream()
                            .filter(p-> "won".equals(p.getOutcome())).count();
                    int neutral = total - losses - wins;

                    double lossRate = total > 0 ? (double) losses / total * 100 : 0;

                    return PositionStatResponse.builder()
                            .position(position)
                            .totalOccurrences(total)
                            .losses(losses)
                            .wins(wins)
                            .neutral(neutral)
                            .lossRate(Math.round(lossRate*10.0)/10.0)
                            .severity(calculatePositionSeverity(lossRate, total))
                            .build();
                })
                .sorted(Comparator.comparingDouble(PositionStatResponse::getLossRate).reversed())
                .collect(Collectors.toList());
    }

    private String calculatePositionSeverity(double lossRate , int total){
        if(total < 3) return "INSUFFICIENT_DATA";
        if(lossRate >=70) return "HIGH";
        if(lossRate >=55) return "MEDIUM";
        if(lossRate >=40) return "LOW";
        return "OK";
    }

    // technique Analysis
    private List<TechniqueStatResponse> analyseTechniques (List<TechniqueLog> logs){
        Map<String,List<TechniqueLog>> byTechnique = logs.stream()
                .collect(Collectors.groupingBy(TechniqueLog::getTechnique));

        return byTechnique.entrySet().stream()
                .map(entry-> {
                    String technique = entry.getKey();
                    List<TechniqueLog> techLogs = entry.getValue();

                    int total = techLogs.size();
                    int successes = (int) techLogs.stream()
                            .filter(t-> Boolean.TRUE.equals(t.getSuccess())).count();
                        int failures = total - successes;

                        double successRate = total > 0
                                ? (double) successes/total * 100 : 0;
                        double failRate = 100 - successRate;

                        String category = techLogs.get(0).getCategory();

                        return TechniqueStatResponse.builder()
                                .technique(technique)
                                .category(category)
                                .totalAttempts(total)
                                .successes(successes)
                                .failures(failures)
                                .successRate(Math.round(successRate*10.0)/10.0)
                                .severity(calculateTechniqueSeverity(successRate,total))
                                .build();

                })
                .sorted(Comparator.comparingDouble(TechniqueStatResponse::getSuccessRate))
                .collect(Collectors.toList());



    }

    private String calculateTechniqueSeverity(double successRate,int total){
        if(total<3) return "INSUFFICIENT_DATA";
        if(successRate<=20) return "HIGH";
        if(successRate<=35) return "MEDIUM";
        if(successRate<=50) return "Low";
        return "OK";
    }

    // role Analysis

    private RoleAnalysisResponse analyseRole (List<PositionLog> logs){
        List<PositionLog> topLogs = logs.stream()
                .filter(p->"top".equals(p.getRole()))
                .collect(Collectors.toList());

        List<PositionLog> bottomLogs = logs.stream()
                .filter(p->"bottom".equals(p.getRole()))
                .collect(Collectors.toList());

         // Minimum data check - unreliable with less than 5 per role
        if(topLogs.size()<5 || bottomLogs.size() <5){
            return RoleAnalysisResponse.builder()
                    .topLossRate(0.0)
                    .bottomLossRate(0.0)
                    .weakerRole("insufficient_data")
                    .summary("Not enough data yet - log at least 5 top and 5 bottom positions for accurate role analysis.")
                    .build();
        }

        double topLossRate = calculateLossRate(topLogs);
        double bottomLossRate = calculateLossRate(bottomLogs);
        double difference = Math.abs(bottomLossRate-topLossRate);
        String weakerRole;
        String summary;

        double weakerRateval = bottomLossRate > topLossRate ? bottomLossRate : topLossRate;
        double strongerRateVal = bottomLossRate > topLossRate ? topLossRate : bottomLossRate;

        if (difference < 5) {
            weakerRole = "balanced";
            summary = "Your top and bottom game are remarkably balanced — this is rare and a real strength.";

        } else if (difference < 15) {
            weakerRole = bottomLossRate > topLossRate ? "bottom" : "top";
            double weakRate = bottomLossRate > topLossRate ? bottomLossRate : topLossRate;
            double strongRate = bottomLossRate > topLossRate ? topLossRate : bottomLossRate;
            summary = String.format(
                    "Slight lean towards %s game being weaker (%.1f%% vs %.1f%% loss rate) — worth monitoring but not a major concern yet.",
                    weakerRole, weakRate, strongRate
            );

        } else if (difference < 30) {
            weakerRole = bottomLossRate > topLossRate ? "bottom" : "top";
            String strongerRole = "bottom".equals(weakerRole) ? "top" : "bottom";
            double weakRate = bottomLossRate > topLossRate ? bottomLossRate : topLossRate;
            double strongRate = bottomLossRate > topLossRate ? topLossRate : bottomLossRate;
            summary = String.format(
                    "You are clearly more comfortable on %s (%.1f%% loss rate vs %.1f%% on %s). Focus on your %s game.",
                    strongerRole, strongRate, weakRate, weakerRole, weakerRole
            );

        } else {
            weakerRole = bottomLossRate > topLossRate ? "bottom" : "top";
            double weakRate = bottomLossRate > topLossRate ? bottomLossRate : topLossRate;
            double strongRate = bottomLossRate > topLossRate ? topLossRate : bottomLossRate;
            summary = String.format(
                    "Your %s game is a significant weakness (%.1f%% loss rate vs %.1f%%). This gap is limiting your overall game and should be your primary focus.",
                    weakerRole, weakRate, strongRate
            );
        }
        return RoleAnalysisResponse.builder()
                .topLossRate(Math.round(topLossRate*10.0)/10.0)
                .bottomLossRate(Math.round(bottomLossRate*10.0)/10.0)
                .weakerRole(weakerRole)
                .summary(summary)
                .build();




    }
    private double calculateLossRate(List<PositionLog> logs){
        if(logs.isEmpty()) return 0.0;
        long losses = logs.stream()
                .filter(p-> "lost".equals(p.getOutcome())).count();
        return (double) losses / logs.size()*100;
    }

    // Cardio Analysis

    private boolean analyzeCardio (List<Session> sessions,
                                   List<PositionLog> allPositions){
        // Get session IDs by energy level group
        Set<String> lowEnergySessionIds = sessions.stream()
                .filter(s->s.getEnergyLevel()!= null && s.getEnergyLevel()<=2)
                .map(Session::getId)
                .collect(Collectors.toSet());

        Set<String> highEnergySessionIds = sessions.stream()
                .filter(s->s.getEnergyLevel()!= null && s.getEnergyLevel()>=4)
                .map(Session::getId)
                .collect(Collectors.toSet());

        if(lowEnergySessionIds.isEmpty() || highEnergySessionIds.isEmpty()) {
            return false;
        }

        List<PositionLog> lowEnergyPositions = allPositions.stream()
                .filter(p->lowEnergySessionIds.contains(p.getSession().getId()))
                .collect(Collectors.toList());

        List<PositionLog> highEnergyPositions = allPositions.stream()
                .filter(p->highEnergySessionIds.contains(p.getSession().getId()))
                .collect(Collectors.toList());

        double lowEnergyLossRate = calculateLossRate(lowEnergyPositions);
        double highEnergyLossRate = calculateLossRate(highEnergyPositions);

        //Flag cardio as weak if loss rate is 20%+ worse in low energy sessions
        return(lowEnergyLossRate - highEnergyLossRate) > 20;

    }

    // Recommendation Engine

    private List<String> generateRecommendations(
            List<PositionStatResponse> positionStats,
            List<TechniqueStatResponse> techniqueStats,
            RoleAnalysisResponse roleAnalysis,
            boolean cardioFlag){

        List<String> recommendations = new ArrayList<>();

        // Position recommendations
        Map<String , String> positionAdvice = new HashMap<>();
        positionAdvice.put("guard", "Drill guard retention — hip escapes and frames daily before class");
        positionAdvice.put("half_guard", "Work half guard sweeps — knee shield and deep half entries");
        positionAdvice.put("mount", "Practice bridge and roll escape — 50 reps daily");
        positionAdvice.put("side_control", "Focus on elbow-knee escape and hip-to-hip recovery");
        positionAdvice.put("back_control", "Work seat belt grip breaks and turn-in escapes");
        positionAdvice.put("turtle", "Drill granby rolls and tight defensive turtle posture");

        positionStats.stream()
                .filter(p-> "HIGH".equals(p.getSeverity()) || "MEDIUM".equals(p.getSeverity()))
                .limit(3)
                .forEach(p->{
                    String advice = positionAdvice.getOrDefault(
                            p.getPosition(),
                            "Drill" + p.getPosition() + "escapes and recovery"
                    );
                    recommendations.add(String.format(
                            "%s (%.1f%% loss rate)", advice, (double) p.getLossRate()
                    ));
                });

        // Technique recommendations
        Map<String, String> techniqueAdvice = new HashMap<>();
        techniqueAdvice.put("armbar", "Focus on grip breaking and hip extension mechanics for armbar finish");
        techniqueAdvice.put("triangle", "Work on angle adjustment and head pull for triangle finish");
        techniqueAdvice.put("rear_naked_choke", "Drill seatbelt grip and chin strap control for RNC");
        techniqueAdvice.put("scissor_sweep", "Work on timing — create the angle before initiating scissor sweep");
        techniqueAdvice.put("double_leg_takedown", "Penetration step drilling — level change and drive through");
        techniqueAdvice.put("heel_hook", "Outside heel hook entry from 50/50 — control the hip first");

        techniqueStats.stream()
                .filter(t -> "HIGH".equals(t.getSeverity())|| "MEDIUM".equals(t.getSeverity()))
                .limit(3)
                .forEach(t ->{
                    String advice = techniqueAdvice.getOrDefault(
                            t.getTechnique(),
                            "Drill" + t.getTechnique() + "mechanics with a partner"
                    );
                    recommendations.add(String.format(
                            "%s (%.1f%% success rate)", advice, (double) t.getSuccessRate()
                    ));
                });

        // Role recommendation

        switch (roleAnalysis.getWeakerRole()) {
            case "bottom" -> recommendations.add(
                    "Your bottom game needs work - dedicate 2 rounds per session to guard recovery and escape drills"

            );
            case "top" -> recommendations.add(
                    "Focus on top pressure — work knee on belly and mount control before attempting submissions"
            );
            case "balanced" -> recommendations.add(
                    "Your top and bottom game are well balanced — keep developing both equally"
            );
            case "insufficient_data" -> recommendations.add(
                    "Log more sessions with both top and bottom positions to get role-specific recommendations"
            );
        }

        // cardio recommendation
        if(cardioFlag){
            recommendations.add("Cardio is affecting your performance — add 20 mins of zone 2 cardio (easy jog/row) 3x per week");

        }
        return recommendations;



    }

    // Stats only endpoints

    public List<PositionStatResponse> getPositionStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<PositionLog> logs = positionLogRepository
                .findBySession_User_Id(user.getId());
        return analysePositions(logs);
    }

    public List<TechniqueStatResponse> getTechniqueStats(String email ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new ResourceNotFoundException("User not found"));
        List<TechniqueLog> logs = techniqueLogRepository
                .findBySession_User_Id(user.getId());
        return analyseTechniques(logs);
    }




}
