package com.curaai.curaai.dto;

public record ExerciseLogDto(
        Long id,
        String activityType,
        int durationMinutes,
        Integer caloriesBurned,
        String notes
) {}