package com.curaai.curaai.repository;

import com.curaai.curaai.model.Prescription;
import com.curaai.curaai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByUserOrderByUploadedAtDesc(User user);

    Optional<Prescription> findByIdAndUser(Long id, User user);
}
