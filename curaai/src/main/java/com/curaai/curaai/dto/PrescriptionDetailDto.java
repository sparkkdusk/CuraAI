package com.curaai.curaai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionDetailDto(
        Long id,
        LocalDateTime uploadedAt,
        String label,
        String rawText,
        List<MedicationDto> medications
) {}
