package com.curaai.curaai.dto;

public record NutritionLogDto(
        Long id,
        String foodName,
        String mealType,
        int calories,
        Integer protein,
        Integer carbs,
        Integer fat,
        Integer fiber,
        Integer sugar,
        Integer healthScore,
        String insight
) {}
