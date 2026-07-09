package com.curaai.curaai.dto;

import java.time.LocalDateTime;

public record PrescriptionSummaryDto(
        Long id,
        LocalDateTime uploadedAt,
        String label,
        int medicationCount
) {}
