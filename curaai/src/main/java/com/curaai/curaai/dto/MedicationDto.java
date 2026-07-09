package com.curaai.curaai.dto;

public record MedicationDto(
        Long id,
        String drugName,
        String dosage,
        String frequency,
        boolean active
) {}
