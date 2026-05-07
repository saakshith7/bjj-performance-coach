package com.bjjcoach.service;

import com.bjjcoach.dto.DayProgramDTO;
import com.bjjcoach.dto.ExerciseDTO;
import com.bjjcoach.dto.SCProgramResponse;
import com.bjjcoach.dto.WeaknessReportResponse;
import com.bjjcoach.entity.SCProgram;
import com.bjjcoach.exception.ResourceNotFoundException;
import com.bjjcoach.model.User;
import com.bjjcoach.repository.SCProgramRepository;
import com.bjjcoach.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;



import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SCProgramGeneratorService {

    private final SCProgramRepository programRepository;
    private final UserRepository userRepository;
    private final WeaknessAnalyzerService analyzerService;
    private final ObjectMapper objectMapper;

    // ─── Core Mapping Engine ──────────────────────────────────────────────
    // Maps BJJ weakness → list of targeted exercises
    private static final Map<String, List<ExerciseDTO>> WEAKNESS_EXERCISE_MAP = new HashMap<>();

    static {
        // Guard retention weakness
        WEAKNESS_EXERCISE_MAP.put("guard", List.of(
                ExerciseDTO.builder()
                        .name("Hip Thrusts")
                        .sets(4).reps("12").rest("60 seconds")
                        .targetArea("Hip flexors and glutes - essential for guard retention and re-guard")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Lying Leg Raises")
                        .sets(3).reps("15").rest("45 seconds")
                        .targetArea("Lower abs and hip flexors for leg control in guard")
                        .difficulty("beginner").build(),
                ExerciseDTO.builder()
                        .name("Hip Circles (standing)")
                        .sets(2).reps("20 each direction").rest("30 seconds")
                        .targetArea("Hip mobility for guard movement and recovery")
                        .difficulty("beginner").build()
        ));

        // Half guard weakness
        WEAKNESS_EXERCISE_MAP.put("half_guard", List.of(
                ExerciseDTO.builder()
                        .name("Lateral Band Walks")
                        .sets(3).reps("15 each side").rest("45 seconds")
                        .targetArea("Hip abductors for half guard frames and knee shield")
                        .difficulty("beginner").build(),
                ExerciseDTO.builder()
                        .name("Single Leg Hip Bridge")
                        .sets(3).reps("12 each leg").rest("45 seconds")
                        .targetArea("Glute and hamstring strength for underhook battles")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Resistance Band Hip Flexion")
                        .sets(3).reps("15 each leg").rest("45 seconds")
                        .targetArea("Hip flexor strength for deep half guard entries")
                        .difficulty("beginner").build()
        ));

        // Mount escape weakness
        WEAKNESS_EXERCISE_MAP.put("mount", List.of(
                ExerciseDTO.builder()
                        .name("Explosive Bridge and Roll")
                        .sets(4).reps("10").rest("60 seconds")
                        .targetArea("Explosive hip extension for mount escape")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Box Jumps")
                        .sets(3).reps("8").rest("90 seconds")
                        .targetArea("Lower body explosiveness for bridging power")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Weighted Glute Bridge")
                        .sets(4).reps("12").rest("60 seconds")
                        .targetArea("Glute strength for powerful bridge escape")
                        .difficulty("beginner").build()

        ));

        // Side control escape weakness
        WEAKNESS_EXERCISE_MAP.put("side_control", List.of(
                ExerciseDTO.builder()
                        .name("fall of Press")
                        .sets(3).reps("12 each side").rest("45 seconds")
                        .targetArea("Core anti-rotation for framing under side control")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Dead Bug")
                        .sets(3).reps("10 each side").rest("45 seconds")
                        .targetArea("Core stability for maintaining frames and escaping")
                        .difficulty("beginner").build(),
                ExerciseDTO.builder()
                        .name("Push-up Plus")
                        .sets(3).reps("12").rest("45 seconds")
                        .targetArea("Serratus and pushing strength for frame creation")
                        .difficulty("beginner").build()
        ));

        // Back control escape weakness
        WEAKNESS_EXERCISE_MAP.put("back_control", List.of(
                ExerciseDTO.builder()
                        .name("Neck Bridge")
                        .sets(3).reps("30 seconds hold").rest("60 seconds")
                        .targetArea("Neck strength for chin tuck defense against chokes")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Seated Hip Rotation")
                        .sets(3).reps("15 each direction").rest("30 seconds")
                        .targetArea("Hip mobility for turn-in escape from back control")
                        .difficulty("beginner").build(),
                ExerciseDTO.builder()
                        .name("Rear Delt Fly")
                        .sets(3).reps("15").rest("45 seconds")
                        .targetArea("Rear deltoid for seat belt grip breaks")
                        .difficulty("beginner").build()
        ));

        // Turtle weakness
        WEAKNESS_EXERCISE_MAP.put("turtle", List.of(
                ExerciseDTO.builder()
                        .name("Cat-Cow Stretch")
                        .sets(3).reps("15").rest("30 seconds")
                        .targetArea("Spine mobility for granby roll and turtle defense")
                        .difficulty("beginner").build(),
                ExerciseDTO.builder()
                        .name("Hollow Body Hold")
                        .sets(3).reps("30 seconds").rest("45 seconds")
                        .targetArea("Core tension for maintaining turtle posture")
                        .difficulty("intermediate").build()
        ));

        // Armbar finish weakness
        WEAKNESS_EXERCISE_MAP.put("armbar", List.of(
                ExerciseDTO.builder()
                        .name("Dead Hangs")
                        .sets(3).reps("30 seconds").rest("60 seconds")
                        .targetArea("Grip strength and forearm endurance for armbar control")
                        .difficulty("beginner").build(),
                ExerciseDTO.builder()
                        .name("Towel Pull-ups")
                        .sets(3).reps("6-8").rest("90 seconds")
                        .targetArea("Grip and lat strength for breaking posture in armbar")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Lat Pull down")
                        .sets(4).reps("10").rest("60 seconds")
                        .targetArea("Lat strength for pulling arm across body to finish armbar")
                        .difficulty("beginner").build()
        ));

        // Triangle weakness
        WEAKNESS_EXERCISE_MAP.put("triangle", List.of(
                ExerciseDTO.builder()
                        .name("Adductor Machine")
                        .sets(3).reps("15").rest("45 seconds")
                        .targetArea("Inner thigh strength for triangle squeeze")
                        .difficulty("beginner").build(),
                ExerciseDTO.builder()
                        .name("Sumo Squat")
                        .sets(3).reps("12").rest("60 seconds")
                        .targetArea("Hip adductor and glute strength for triangle lock")
                        .difficulty("beginner").build()
        ));

        // Scissor sweep weakness
        WEAKNESS_EXERCISE_MAP.put("scissor_sweep", List.of(
                ExerciseDTO.builder()
                        .name("Lying Hip Abduction")
                        .sets(3).reps("15 each side").rest("45 seconds")
                        .targetArea("Hip abductor for scissor leg movement timing")
                        .difficulty("beginner").build(),
                ExerciseDTO.builder()
                        .name("Plank Hip Dips")
                        .sets(3).reps("12 each side").rest("45 seconds")
                        .targetArea("Oblique strength for rotational sweep power")
                        .difficulty("intermediate").build()
        ));

        // Takedown weakness
        WEAKNESS_EXERCISE_MAP.put("double_leg_takedown", List.of(
                ExerciseDTO.builder()
                        .name("Romanian Deadlift")
                        .sets(4).reps("8").rest("90 seconds")
                        .targetArea("Hamstring and posterior chain for takedown drive")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Bulgarian Split Squat")
                        .sets(3).reps("10 each leg").rest("75 seconds")
                        .targetArea("Single leg strength for penetration step")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Medicine Ball Slam")
                        .sets(3).reps("10").rest("60 seconds")
                        .targetArea("Full body explosiveness for takedown finishes")
                        .difficulty("intermediate").build()
        ));

        // Heel hook weakness
        WEAKNESS_EXERCISE_MAP.put("heel_hook", List.of(
                ExerciseDTO.builder()
                        .name("Hip Internal Rotation Stretch")
                        .sets(3).reps("30 seconds each").rest("30 seconds")
                        .targetArea("Hip mobility for heel hook entries and control")
                        .difficulty("beginner").build(),
                ExerciseDTO.builder()
                        .name("Lying Hamstring Curl")
                        .sets(3).reps("12").rest("45 seconds")
                        .targetArea("Hamstring strength for heel hook wrapping and finishing")
                        .difficulty("beginner").build()
        ));

        // Cardio weakness
        WEAKNESS_EXERCISE_MAP.put("cardio", List.of(
                ExerciseDTO.builder()
                        .name("Zone 2 Run or Row")
                        .sets(1).reps("25 minutes").rest("N/A")
                        .targetArea("Aerobic base — ability to maintain performance in later rounds")
                        .difficulty("beginner").build(),
                ExerciseDTO.builder()
                        .name("HIT Intervals (30s on / 30s off)")
                        .sets(8).reps("30 seconds").rest("30 seconds")
                        .targetArea("Anaerobic capacity for explosive scrambles")
                        .difficulty("intermediate").build()
        ));

        // Top game weakness
        WEAKNESS_EXERCISE_MAP.put("top_game", List.of(
                ExerciseDTO.builder()
                        .name("Barbell Squat")
                        .sets(4).reps("6-8").rest("120 seconds")
                        .targetArea("Leg drive and base for top pressure and passing")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Knee on Belly Isometric Hold")
                        .sets(3).reps("30 seconds each side").rest("45 seconds")
                        .targetArea("Balance and hip stability for knee on belly control")
                        .difficulty("beginner").build(),
                ExerciseDTO.builder()
                        .name("Kettlebell Swing")
                        .sets(4).reps("12").rest("60 seconds")
                        .targetArea("Hip hinge explosiveness for top pressure and guard passing drive")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Farmers Carry")
                        .sets(3).reps("30 meters").rest("60 seconds")
                        .targetArea("Grip and total body tension for maintaining top control")
                        .difficulty("beginner").build()
        ));

        WEAKNESS_EXERCISE_MAP.put("bottom_game", List.of(
                ExerciseDTO.builder()
                        .name("Deadlift")
                        .sets(4).reps("5").rest("120 seconds")
                        .targetArea("Full posterior chain — foundational strength for all bottom game escapes")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Ab Wheel Rollout")
                        .sets(3).reps("10").rest("60 seconds")
                        .targetArea("Core strength for maintaining frames and creating space")
                        .difficulty("intermediate").build(),
                ExerciseDTO.builder()
                        .name("Jefferson Curl")
                        .sets(3).reps("8").rest("60 seconds")
                        .targetArea("Spine flexibility and hamstring length for bottom positions")
                        .difficulty("intermediate").build()
        ));
    }

    // ─── Cardio day exercises (always included if cardio flag) ────────────
    private static final List<ExerciseDTO> CARDIO_EXERCISES = List.of(
            ExerciseDTO.builder()
                    .name("Zone 2 Easy Run or Row")
                    .sets(1).reps("20-30 minutes").rest("N/A")
                    .targetArea("Aerobic base building — keeps heart rate at 60-70% max")
                    .difficulty("beginner").build(),
            ExerciseDTO.builder()
                    .name("Jump Rope")
                    .sets(5).reps("2 minutes").rest("1 minute")
                    .targetArea("Footwork and conditioning — mimics scramble intensity")
                    .difficulty("beginner").build(),
            ExerciseDTO.builder()
                    .name("Rowing Machine Intervals")
                    .sets(6).reps("500m").rest("90 seconds")
                    .targetArea("Full body cardio — back and leg dominant like BJJ")
                    .difficulty("intermediate").build()
    );

    // Generate Program

    public SCProgramResponse generateProgram(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Run weakness analysis first
        WeaknessReportResponse report = analyzerService.analyseAndGenerate(email);

        // collect all weak areas
        List<String> weakAreas = new ArrayList<>();

        report.getWeakPositions().forEach(p -> weakAreas.add(p.getPosition()));
        report.getWeakTechniques().forEach(t -> weakAreas.add(t.getTechnique()));

        String weakerRole = report.getRoleAnalysis().getWeakerRole();

        if ("bottom".equals(weakerRole)) {
            weakAreas.add("bottom_game");
        } else if ("top".equals(weakerRole)) {
            weakAreas.add("top_game");
        }
        if (report.isCardioFlag()) {
            weakAreas.add("cardio");
        }

        // collect all relevant exercises from mapping engine
        List<ExerciseDTO> allExercises = weakAreas.stream()
                .distinct()
                .flatMap(area -> WEAKNESS_EXERCISE_MAP
                        .getOrDefault(area, List.of()).stream())
                .collect(Collectors.toList());

        // if no specific weakness found use bottom game as default
        if (allExercises.isEmpty()) {
            allExercises = new ArrayList<>(WEAKNESS_EXERCISE_MAP.get("bottom_game"));
        }

        // scale by fitness level
        String fitnessLevel = user.getFitnessLevel() != null
                ? user.getFitnessLevel() : "beginner";
        allExercises = scaleByFitnessLevel(allExercises, fitnessLevel);

        // Split into 3 day weekly schedule
        List<DayProgramDTO> weeklySchedule = buildWeeklySchedule(
                allExercises, report, fitnessLevel
        );

        // Check previous program for comparison
        String comparison = buildComparison(user.getId(), weakAreas);

        // save to database
        SCProgram saved = saveProgram(user, fitnessLevel, weakAreas, weeklySchedule);

        return SCProgramResponse.builder()
                .id(saved.getId())
                .generatedAt(saved.getGeneratedAt())
                .durationWeeks(8)
                .fitnessLevel(fitnessLevel)
                .targetWeaknesses(weakAreas)
                .weeklySchedule(weeklySchedule)
                .notes("deload on week 4 and 8 - reduce all sets by 1 and weight by 40%." + "Train S&C on non-BJJ days where possible.")
                .comparisonWithPrevious(comparison)
                .build();


    }
    // scale sets and reps by fitness level

    private List<ExerciseDTO> scaleByFitnessLevel(List<ExerciseDTO> exercises,
                                                  String fitnessLevel) {
        return exercises.stream().map(e -> {
            ExerciseDTO scaled = ExerciseDTO.builder()
                    .name(e.getName())
                    .reps(e.getReps())
                    .sets(e.getSets())
                    .targetArea(e.getTargetArea())
                    .difficulty(e.getDifficulty())
                    .build();

            switch (fitnessLevel.toLowerCase()) {
                case "beginner" -> scaled.setSets(Math.max(2, e.getSets() - 1));
                case "advanced" -> scaled.setSets(e.getSets() + 1);
                default -> scaled.setSets(e.getSets()); // intermediate
            }
            return scaled;
        }).collect(Collectors.toList());
    }

    // build 3 day weekly schedule

    private List<DayProgramDTO> buildWeeklySchedule(List<ExerciseDTO> allExercises,
                                                    WeaknessReportResponse report,
                                                    String fitnessLevel) {

        List<DayProgramDTO> schedule = new ArrayList<>();

        // deduplicate exercises by name - keep first occurrence preserve insertion order
        List<ExerciseDTO> unique = allExercises.stream()
                .collect(Collectors.toMap(
                        ExerciseDTO::getName,
                        e -> e,
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .collect(Collectors.toList());


        int total = unique.size();
        int third = Math.max(1, total / 3);

        // Monday - position weakness report
        String mondayFocus = report.getWeakPositions().isEmpty()
                ? "General BJJ Strength"
                : "Guard and Bottom Game Strength";

        schedule.add(DayProgramDTO.builder()
                .day("Monday")
                .focus(mondayFocus)
                .exercises(unique.subList(0, Math.min(third, total)))
                .build());

        // wednesday - technique weakness report
        String wednesdayFocus = report.getWeakTechniques().isEmpty()
                ? "Core and Mobility"
                : "Submission Finishing Strength";

        schedule.add(DayProgramDTO.builder()
                .day("Wednesday")
                .focus(wednesdayFocus)
                .exercises(unique.subList(
                        Math.min(third, total),
                        Math.min(third * 2, total)
                ))
                .build());

        //friday - cardio or remaining exercises
        List<ExerciseDTO> fridayExercises;
        if (report.isCardioFlag()) {
            fridayExercises = CARDIO_EXERCISES;
        } else {
            fridayExercises = unique.subList(
                    Math.min(third * 2, total),
                    total
            );
            if (fridayExercises.isEmpty()) {
                fridayExercises = CARDIO_EXERCISES.subList(0, 1);
            }
        }

        schedule.add(DayProgramDTO.builder()
                .day("Friday")
                .focus(report.isCardioFlag()
                        ? "Cardio and Conditioning"
                        : "Explosiveness and Accessory Work")
                .exercises(fridayExercises)
                .build());

        return schedule;

    }

    // Compare with previous program

    private String buildComparison(String userId, List<String> currentWeakAreas) {
        Optional<SCProgram> previous = programRepository.findLatestByUserId(userId);

        if (previous.isEmpty()) {
            return "This is your first generated program. Regenerate after 4 weeks to track progress.";
        }

        try {
            // ALL of this must be inside the try block
            List<String> prevWeakAreas = objectMapper.readValue(
                    previous.get().getTargetWeaknesses(),
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, String.class)
            );

            List<String> improved = prevWeakAreas.stream()
                    .filter(w -> !currentWeakAreas.contains(w))
                    .collect(Collectors.toList());

            List<String> newWeaknesses = currentWeakAreas.stream()
                    .filter(w -> !prevWeakAreas.contains(w))
                    .collect(Collectors.toList());

            List<String> persisting = currentWeakAreas.stream()
                    .filter(prevWeakAreas::contains)
                    .collect(Collectors.toList());

            StringBuilder comparison = new StringBuilder();

            if (!improved.isEmpty()) {
                comparison.append("✅ Improved since last program: ")
                        .append(String.join(", ", improved)).append(". ");
            }
            if (!newWeaknesses.isEmpty()) {
                comparison.append("🆕 New weaknesses detected: ")
                        .append(String.join(", ", newWeaknesses)).append(". ");
            }
            if (!persisting.isEmpty()) {
                comparison.append("⚠️ Still needs work: ")
                        .append(String.join(", ", persisting)).append(".");
            }
            if (comparison.isEmpty()) {
                return "Your weak areas are unchanged since the last program. Keep training consistently.";
            }

            return comparison.toString();

        } catch (JacksonException e) {
            return "Could not compare with previous program.";
        }
    }

    // ─── Save program to database ─────────────────────────────────────────

    private SCProgram saveProgram(User user,
                                  String fitnessLevel,
                                  List<String> weakAreas,
                                  List<DayProgramDTO> weeklySchedule) {
        try {
            SCProgram program = SCProgram.builder()
                    .user(user)
                    .durationWeeks(8)
                    .fitnessLevel(fitnessLevel)
                    .targetWeaknesses(objectMapper.writeValueAsString(weakAreas))
                    .weeklySchedule(objectMapper.writeValueAsString(weeklySchedule))
                    .notes("Deload on week 4 and week 8.")
                    .build();

            return programRepository.save(program);

        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize program data");
        }
    }

    // ─── Fetch methods ────────────────────────────────────────────────────

    public SCProgramResponse getLatestProgram(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SCProgram program = programRepository.findLatestByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No program found. Generate one first at POST /api/programs/generate"
                ));

        return toResponse(program);
    }

    public List<SCProgramResponse> getAllPrograms(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return programRepository.findByUserIdOrderByGeneratedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public SCProgramResponse getProgramById(String programId, String email) {
        SCProgram program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Program not found with ID: " + programId
                ));

        if (!program.getUser().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Program not found with ID: " + programId);
        }

        return toResponse(program);
    }

    // ─── Entity to DTO conversion ─────────────────────────────────────────

    private SCProgramResponse toResponse(SCProgram program) {
        try {
            List<String> weaknesses = objectMapper.readValue(
                    program.getTargetWeaknesses(),
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, String.class)
            );

            List<DayProgramDTO> schedule = objectMapper.readValue(
                    program.getWeeklySchedule(),
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, DayProgramDTO.class)
            );

            return SCProgramResponse.builder()
                    .id(program.getId())
                    .generatedAt(program.getGeneratedAt())
                    .durationWeeks(program.getDurationWeeks())
                    .fitnessLevel(program.getFitnessLevel())
                    .targetWeaknesses(weaknesses)
                    .weeklySchedule(schedule)
                    .notes(program.getNotes())
                    .comparisonWithPrevious(null)
                    .build();

        } catch (JacksonException e) {
            throw new RuntimeException("Failed to deserialize program data");
        }
    }
}