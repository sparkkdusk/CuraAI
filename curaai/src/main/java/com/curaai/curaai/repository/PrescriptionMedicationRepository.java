package com.curaai.curaai.repository;

import com.curaai.curaai.model.PrescriptionMedication;
import com.curaai.curaai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionMedicationRepository extends JpaRepository<PrescriptionMedication, Long> {

    /** All active medications across every prescription belonging to this user - feeds the daily tracker. */
    List<PrescriptionMedication> findByPrescriptionUserAndActiveTrue(User user);
}
