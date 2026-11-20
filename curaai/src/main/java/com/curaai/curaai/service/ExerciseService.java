package com.curaai.curaai.service;

import com.curaai.curaai.dto.DaySummaryDto;
import com.curaai.curaai.dto.ExerciseLogDto;
import com.curaai.curaai.model.ExerciseLog;
import com.curaai.curaai.model.User;
import com.curaai.curaai.repository.ExerciseLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Handles daily exercise logging and rolls the last 7 days into a
 * day-by-day summary for the weekly view on /exercise.
 */
@Service
public class ExerciseService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    private final ExerciseLogRepository repository;
    private final VectorStoreService vectorStoreService;

    public ExerciseService(ExerciseLogRepository repository, VectorStoreService vectorStoreService) {
        this.repository = repository;
        this.vectorStoreService = vectorStoreService;
    }

    // NOT @Transactional on purpose: repository.save() commits on its own, so the log is
    // always saved, and a failure inside RAG indexing can no longer roll the save back.
    public void logActivity(User user, String activityType, int durationMinutes,
                            Integer caloriesBurned, String notes, LocalDate date) {
        ExerciseLog entry = new ExerciseLog();
        entry.setUser(user);
        entry.setDate(date != null ? date : LocalDate.now());
        entry.setActivityType(activityType);
        entry.setDurationMinutes(durationMinutes);
        entry.setCaloriesBurned(caloriesBurned);
        entry.setNotes(notes);
        ExerciseLog saved = repository.save(entry);

        // Run off-thread so a slow/rate-limited embeddings API never delays the save response
        CompletableFuture.runAsync(() -> indexForRag(user, saved));
    }

    /** One-time (or repeatable) backfill: (re)indexes every existing exercise log for RAG search. */
    public void reindexAll(User user) {
        List<ExerciseLog> all = repository.findByUser(user);
        CompletableFuture.runAsync(() -> {
            for (ExerciseLog entry : all) {
                indexForRag(user, entry);
            }
        });
    }

    private void indexForRag(User user, ExerciseLog entry) {
        try {
            String content = String.format(
                    "On %s, did %s for %d minutes, burning %s calories.%s",
                    entry.getDate(),
                    entry.getActivityType(),
                    entry.getDurationMinutes(),
                    entry.getCaloriesBurned() != null ? entry.getCaloriesBurned().toString() : "an unknown amount of",
                    (entry.getNotes() != null && !entry.getNotes().isBlank()) ? " Notes: " + entry.getNotes() : ""
            );
            vectorStoreService.upsertChunk(user.getId(), VectorStoreService.SourceType.EXERCISE, entry.getId(), content);
        } catch (Exception e) {
            // Indexing failure shouldn't block the exercise log itself from saving.
            log.error("RAG indexing failed for exercise log {}", entry.getId(), e);
        }
    }

    /** Today and the 6 days before it, oldest first, each with its logs and totals. */
    @Transactional(readOnly = true)
    public List<DaySummaryDto> weeklySummary(User user) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        List<ExerciseLog> logs = repository.findByUserAndDateBetweenOrderByDateAsc(user, weekStart, today);

        Map<LocalDate, List<ExerciseLog>> byDate = logs.stream()
                .collect(Collectors.groupingBy(ExerciseLog::getDate));

        List<DaySummaryDto> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            List<ExerciseLog> dayLogs = byDate.getOrDefault(date, List.of());

            int totalMinutes = dayLogs.stream().mapToInt(ExerciseLog::getDurationMinutes).sum();
            int totalCalories = dayLogs.stream()
                    .mapToInt(l -> l.getCaloriesBurned() != null ? l.getCaloriesBurned() : 0)
                    .sum();

            List<ExerciseLogDto> logDtos = dayLogs.stream()
                    .map(l -> new ExerciseLogDto(l.getId(), l.getActivityType(), l.getDurationMinutes(),
                            l.getCaloriesBurned(), l.getNotes()))
                    .toList();

            String dayLabel = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            result.add(new DaySummaryDto(date, dayLabel, totalMinutes, totalCalories, logDtos));
        }

        return result;
    }

    public int weeklyTotalMinutes(List<DaySummaryDto> week) {
        return week.stream().mapToInt(DaySummaryDto::totalMinutes).sum();
    }

    public int weeklyTotalCalories(List<DaySummaryDto> week) {
        return week.stream().mapToInt(DaySummaryDto::totalCalories).sum();
    }

    public int activeDaysCount(List<DaySummaryDto> week) {
        return (int) week.stream().filter(d -> d.totalMinutes() > 0).count();
    }

    /**
     * Plain-text summary of the last 7 days of exercise, for injecting straight into the
     * chat prompt (does not depend on vector search / embeddings).
     */
    public String weeklyContextText(User user) {
        List<DaySummaryDto> week = weeklySummary(user);
        StringBuilder sb = new StringBuilder();
        for (DaySummaryDto d : week) {
            sb.append("- ").append(d.dayLabel()).append(" ").append(d.date()).append(": ");
            if (d.logs().isEmpty()) {
                sb.append("no exercise (rest day)\n");
                continue;
            }
            sb.append(d.totalMinutes()).append(" min, ").append(d.totalCalories()).append(" kcal total. Sessions: ");
            sb.append(d.logs().stream().map(l -> l.activityType() + " " + l.durationMinutes() + " min"
                            + (l.caloriesBurned() != null ? ", " + l.caloriesBurned() + " kcal" : "")
                            + (l.notes() != null && !l.notes().isBlank() ? " (" + l.notes() + ")" : ""))
                    .collect(Collectors.joining("; ")));
            sb.append("\n");
        }
        sb.append("Today's date is ").append(LocalDate.now()).append(".");
        return sb.toString();
    }
}