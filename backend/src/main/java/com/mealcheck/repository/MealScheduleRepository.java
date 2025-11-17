package com.mealcheck.repository;

import com.mealcheck.entity.MealSchedule;
import com.mealcheck.entity.MealSchedule.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealScheduleRepository extends JpaRepository<MealSchedule, Long> {
    List<MealSchedule> findByMealDate(LocalDate mealDate);
    List<MealSchedule> findByMealDateBetween(LocalDate startDate, LocalDate endDate);
    List<MealSchedule> findByActiveTrue();
    Optional<MealSchedule> findByMealDateAndMealType(LocalDate mealDate, MealType mealType);
    List<MealSchedule> findByMealDateGreaterThanEqualOrderByMealDateAsc(LocalDate date);
}
