package com.curaai.curaai.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks whether a given medication was taken on a given day.
 * One row per (medication, day) - simple daily adherence checkbox rather
 * than modeling exact dose times, which frequency strings like "1-0-1" or
 * "twice daily" can't reliably be parsed into without user confirmation.
 */
@Entity
@Table(
        name = "medication_doses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"medication_id", "dose_date"})
)
public class MedicationDose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medication_id", nullable = false)
    private PrescriptionMedication medication;

    @Column(name = "dose_date", nullable = false)
    private LocalDate doseDate;

    @Column(nullable = false)
    private boolean taken = false;

    private LocalDateTime takenAt;

    public MedicationDose() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PrescriptionMedication getMedication() { return medication; }
    public void setMedication(PrescriptionMedication medication) { this.medication = medication; }

    public LocalDate getDoseDate() { return doseDate; }
    public void setDoseDate(LocalDate doseDate) { this.doseDate = doseDate; }

    public boolean isTaken() { return taken; }
    public void setTaken(boolean taken) { this.taken = taken; }

    public LocalDateTime getTakenAt() { return takenAt; }
    public void setTakenAt(LocalDateTime takenAt) { this.takenAt = takenAt; }
}
