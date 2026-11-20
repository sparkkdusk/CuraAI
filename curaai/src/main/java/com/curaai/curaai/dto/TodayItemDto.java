package com.curaai.curaai.dto;

public record TodayItemDto(
        Long medicationId,
        String drugName,
        String dosage,
        String frequency,
        boolean taken
) {}
