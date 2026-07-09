package com.curaai.curaai.dto;

import com.curaai.curaai.model.Medication;

import java.util.List;

/**
 * Body for POST /prescription/save. The front-end sends back the (possibly
 * user-edited) result of /prescription/analyze along with an optional label.
 */
public record SavePrescriptionRequest(
        String label,
        String rawText,
        List<Medication> medications
) {}
