package com.curaai.curaai.dto;

import java.time.LocalDate;
import java.util.List;

public record DaySummaryDto(
        LocalDate date,
        String dayLabel,
        int totalMinutes,
        int totalCalories,
        List<ExerciseLogDto> logs
) {}