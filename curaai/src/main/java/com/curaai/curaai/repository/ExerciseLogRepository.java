package com.curaai.curaai.repository;

import com.curaai.curaai.model.ExerciseLog;
import com.curaai.curaai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExerciseLogRepository extends JpaRepository<ExerciseLog, Long> {

    List<ExerciseLog> findByUserAndDateBetweenOrderByDateAsc(User user, LocalDate start, LocalDate end);

    List<ExerciseLog> findByUserOrderByDateDescIdDesc(User user);
}