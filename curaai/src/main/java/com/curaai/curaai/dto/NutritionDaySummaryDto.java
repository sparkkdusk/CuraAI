package com.curaai.curaai.dto;

import java.time.LocalDate;
import java.util.List;

public record NutritionDaySummaryDto(
        LocalDate date,
        String dayLabel,
        int totalCalories,
        List<NutritionLogDto> logs
) {}
