package com.curaai.curaai.service;

import com.curaai.curaai.dto.DaySummaryDto;
import com.curaai.curaai.dto.ExerciseLogDto;
import com.curaai.curaai.model.ExerciseLog;
import com.curaai.curaai.model.User;
import com.curaai.curaai.repository.ExerciseLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles daily exercise logging and rolls the last 7 days into a
 * day-by-day summary for the weekly view on /exercise.
 */
@Service
public class ExerciseService {

    private final ExerciseLogRepository repository;

    public ExerciseService(ExerciseLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void logActivity(User user, String activityType, int durationMinutes,
                            Integer caloriesBurned, String notes, LocalDate date) {
        ExerciseLog log = new ExerciseLog();
        log.setUser(user);
        log.setDate(date != null ? date : LocalDate.now());
        log.setActivityType(activityType);
        log.setDurationMinutes(durationMinutes);
        log.setCaloriesBurned(caloriesBurned);
        log.setNotes(notes);
        repository.save(log);
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
}