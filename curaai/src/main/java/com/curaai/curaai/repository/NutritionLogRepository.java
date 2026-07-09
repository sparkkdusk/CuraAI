package com.curaai.curaai.repository;

import com.curaai.curaai.model.NutritionLog;
import com.curaai.curaai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface NutritionLogRepository extends JpaRepository<NutritionLog, Long> {

    List<NutritionLog> findByUserAndDateBetweenOrderByDateAsc(User user, LocalDate start, LocalDate end);

    List<NutritionLog> findByUserOrderByDateDescIdDesc(User user);
}
