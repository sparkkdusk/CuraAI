package com.curaai.curaai.service;

import com.curaai.curaai.dto.*;
import com.curaai.curaai.model.Medication;
import com.curaai.curaai.model.MedicationDose;
import com.curaai.curaai.model.Prescription;
import com.curaai.curaai.model.PrescriptionMedication;
import com.curaai.curaai.model.User;
import com.curaai.curaai.repository.MedicationDoseRepository;
import com.curaai.curaai.repository.PrescriptionMedicationRepository;
import com.curaai.curaai.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Persistence layer for saved prescriptions and the day-to-day medication
 * tracker built on top of them. Kept separate from PrescriptionAnalysisService,
 * which only talks to the Gemini API and never touches the database.
 */
@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionMedicationRepository medicationRepository;
    private final MedicationDoseRepository doseRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                                PrescriptionMedicationRepository medicationRepository,
                                MedicationDoseRepository doseRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.medicationRepository = medicationRepository;
        this.doseRepository = doseRepository;
    }

    @Transactional
    public Long save(User user, String label, String rawText, List<Medication> medications) {
        Prescription prescription = new Prescription();
        prescription.setUser(user);
        prescription.setLabel(label);
        prescription.setRawText(rawText);

        if (medications != null) {
            for (Medication m : medications) {
                PrescriptionMedication pm = new PrescriptionMedication();
                pm.setPrescription(prescription);
                pm.setDrugName(m.drugName());
                pm.setDosage(m.dosage());
                pm.setFrequency(m.frequency());
                prescription.getMedications().add(pm);
            }
        }

        return prescriptionRepository.save(prescription).getId();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionSummaryDto> history(User user) {
        return prescriptionRepository.findByUserOrderByUploadedAtDesc(user).stream()
                .map(p -> new PrescriptionSummaryDto(
                        p.getId(), p.getUploadedAt(), p.getLabel(), p.getMedications().size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PrescriptionDetailDto detail(Long prescriptionId, User user) {
        Prescription p = prescriptionRepository.findByIdAndUser(prescriptionId, user)
                .orElseThrow(() -> new NoSuchElementException("Prescription not found"));

        List<MedicationDto> meds = p.getMedications().stream()
                .map(m -> new MedicationDto(m.getId(), m.getDrugName(), m.getDosage(), m.getFrequency(), m.isActive()))
                .toList();

        return new PrescriptionDetailDto(p.getId(), p.getUploadedAt(), p.getLabel(), p.getRawText(), meds);
    }

    /** Every active medication for this user, with whether today's dose has been marked taken. */
    @Transactional(readOnly = true)
    public List<TodayItemDto> today(User user) {
        LocalDate today = LocalDate.now();
        List<PrescriptionMedication> active = medicationRepository.findByPrescriptionUserAndActiveTrue(user);

        return active.stream()
                .map(m -> {
                    boolean taken = doseRepository.findByMedicationIdAndDoseDate(m.getId(), today)
                            .map(MedicationDose::isTaken)
                            .orElse(false);
                    return new TodayItemDto(m.getId(), m.getDrugName(), m.getDosage(), m.getFrequency(), taken);
                })
                .toList();
    }

    @Transactional
    public TodayItemDto markDose(Long medicationId, User user, boolean taken) {
        PrescriptionMedication medication = medicationRepository.findById(medicationId)
                .filter(m -> m.getPrescription().getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NoSuchElementException("Medication not found"));

        LocalDate today = LocalDate.now();
        MedicationDose dose = doseRepository.findByMedicationIdAndDoseDate(medicationId, today)
                .orElseGet(() -> {
                    MedicationDose d = new MedicationDose();
                    d.setMedication(medication);
                    d.setDoseDate(today);
                    return d;
                });
        dose.setTaken(taken);
        dose.setTakenAt(taken ? LocalDateTime.now() : null);
        doseRepository.save(dose);

        return new TodayItemDto(medication.getId(), medication.getDrugName(), medication.getDosage(),
                medication.getFrequency(), taken);
    }

    @Transactional
    public void deactivateMedication(Long medicationId, User user) {
        PrescriptionMedication medication = medicationRepository.findById(medicationId)
                .filter(m -> m.getPrescription().getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new NoSuchElementException("Medication not found"));
        medication.setActive(false);
    }
}
