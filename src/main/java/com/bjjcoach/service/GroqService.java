package com.bjjcoach.service;

import com.bjjcoach.dto.WeaknessReportResponse;
import com.bjjcoach.model.User;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    private final RestClient restClient;

    public GroqService() {
        this.restClient = RestClient.create();
    }

    // Generate AI recommendations

    public String generatePersonalRecommendations(WeaknessReportResponse report, User user) {
        String prompt = buildPrompt(report, user);

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", "llama-3.1-8b-instant",
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.7,
                    "max_tokens", 1000
            );

            GroqResponse response = restClient.post()
                    .uri(apiUrl)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(GroqResponse.class);

            if (response != null
                    && response.choices() != null
                    && !response.choices().isEmpty()) {
                return response.choices().get(0).message().content();
            }
            return fallbackRecommendations(report);

        } catch (Exception e) {
            log.warn("Groq API call failed - using fallback. Error: {}", e.getMessage());
            return fallbackRecommendations(report);
        }
    }


    // build prompt

    private String buildPrompt(WeaknessReportResponse report, User user) {
        StringBuilder prompt = new StringBuilder();

        // User profile
        prompt.append("=== PRACTITIONER PROFILE ===\n");
        prompt.append("Belt: ").append(user.getBelt()).append("\n");
        prompt.append("Training frequency: ")
                .append(user.getTrainingDaysPerWeek())
                .append(" days per week\n");
        prompt.append("Fitness level: ").append(user.getFitnessLevel()).append("\n");
        prompt.append("Goal: ").append(user.getGoal()).append("\n");
        prompt.append("Sessions analyzed: ")
                .append(report.getTotalSessionsAnalyzed()).append("\n\n");

        // Weak positions
        prompt.append("=== POSITION WEAKNESSES ===\n");
        if (report.getWeakPositions().isEmpty()) {
            prompt.append("No significant position weaknesses detected yet ");
            prompt.append("(insufficient data)\n");
        } else {
            report.getWeakPositions().forEach(p ->
                    prompt.append(String.format("- %s: %.1f%% loss rate (%s severity) — ",
                                    p.getPosition(), p.getLossRate(), p.getSeverity()))
                            .append(p.getTotalOccurrences())
                            .append(" total occurrences, ")
                            .append(p.getLosses())
                            .append(" losses\n")
            );
        }

        // Weak techniques
        prompt.append("\n=== TECHNIQUE WEAKNESSES ===\n");
        if (report.getWeakTechniques().isEmpty()) {
            prompt.append("No significant technique weaknesses detected yet\n");
        } else {
            report.getWeakTechniques().forEach(t ->
                    prompt.append(String.format("- %s (%s): %.1f%% success rate (%s severity) — ",
                                    t.getTechnique(), t.getCategory(),
                                    t.getSuccessRate(), t.getSeverity()))
                            .append(t.getTotalAttempts())
                            .append(" attempts, ")
                            .append(t.getSuccesses())
                            .append(" successes\n")
            );
        }

        // Role analysis
        prompt.append("\n=== ROLE ANALYSIS ===\n");
        prompt.append("Top game loss rate: ")
                .append(report.getRoleAnalysis().getTopLossRate()).append("%\n");
        prompt.append("Bottom game loss rate: ")
                .append(report.getRoleAnalysis().getBottomLossRate()).append("%\n");
        prompt.append("Weaker role: ")
                .append(report.getRoleAnalysis().getWeakerRole()).append("\n");

        // Cardio
        // Cardio
        prompt.append("\n=== CARDIO ===\n");
        if (report.isCardioFlag()) {
            prompt.append("Cardio weakness detected: YES — performance significantly worse in low energy sessions\n\n");
        } else {
            prompt.append("Cardio weakness detected: NO\n\n");
        }

        // Instructions
        prompt.append("=== YOUR TASK ===\n");
        prompt.append("Provide a highly personalized 3-week improvement plan. Include:\n");
        prompt.append("1. The single most important thing to focus on first and why\n");
        prompt.append("2. Specific daily drills for the top 2 position weaknesses ");
        prompt.append("(with exact rep counts)\n");
        prompt.append("3. Technical fix for the worst technique weakness ");
        prompt.append("(what is likely going wrong mechanically)\n");
        prompt.append("4. One S&C exercise to prioritize this week based on their ");
        prompt.append("fitness level and weaknesses\n");
        prompt.append("5. A realistic expectation — how long to see improvement\n\n");
        prompt.append("Be specific, direct and speak like a coach — not a chatbot. ");
        prompt.append("Keep response under 400 words.");

        return prompt.toString();
    }

    // ─── Fallback if API fails ────────────────────────────────────────────

    private String fallbackRecommendations(WeaknessReportResponse report) {
        if (report.getRecommendations() == null ||
                report.getRecommendations().isEmpty()) {
            return "Log more sessions to receive personalized AI recommendations.";
        }
        return "AI recommendations temporarily unavailable. " +
                "Rule-based recommendations: " +
                String.join(". ", report.getRecommendations());
    }

    // ─── Response records ─────────────────────────────────────────────────

    record GroqResponse(List<Choice> choices) {}
    record Choice(Message message) {}
    record Message(String content) {}
}
