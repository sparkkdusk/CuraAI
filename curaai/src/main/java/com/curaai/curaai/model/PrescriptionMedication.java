package com.curaai.curaai.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A single medication line belonging to a saved {@link Prescription}.
 * Distinct from the {@link Medication} record, which is just the plain
 * DTO shape returned by the Gemini OCR call before anything is persisted.
 */
@Entity
@Table(name = "prescription_medications")
public class PrescriptionMedication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(nullable = false)
    private String drugName;

    private String dosage;

    private String frequency;

    /** False once the user marks a course as finished/discontinued; hidden from the daily tracker. */
    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "medication", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MedicationDose> doses = new ArrayList<>();

    public PrescriptionMedication() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Prescription getPrescription() { return prescription; }
    public void setPrescription(Prescription prescription) { this.prescription = prescription; }

    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<MedicationDose> getDoses() { return doses; }
    public void setDoses(List<MedicationDose> doses) { this.doses = doses; }
}
