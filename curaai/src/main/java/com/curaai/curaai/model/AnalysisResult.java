package com.curaai.curaai.model;

import java.util.List;

/**
 * Response payload for POST /prescription/analyze.
 * Serialized to JSON and consumed by prescription.html's fetch() call.
 */
public record AnalysisResult(
        boolean success,
        String rawText,
        List<Medication> medications,
        String error
) {}
