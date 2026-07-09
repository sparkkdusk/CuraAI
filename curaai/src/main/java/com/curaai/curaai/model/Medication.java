package com.curaai.curaai.model;

/**
 * A single medication entry extracted from a prescription image:
 * the drug name, its dosage (e.g. "500mg"), and how often it's taken
 * (e.g. "twice daily", "1-0-1").
 */
public record Medication(String drugName, String dosage, String frequency) {}

