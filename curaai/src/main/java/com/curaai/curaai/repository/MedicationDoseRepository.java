package com.curaai.curaai.repository;

import com.curaai.curaai.model.MedicationDose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MedicationDoseRepository extends JpaRepository<MedicationDose, Long> {

    Optional<MedicationDose> findByMedicationIdAndDoseDate(Long medicationId, LocalDate doseDate);
}
