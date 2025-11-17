package com.mealcheck.repository;

import com.mealcheck.entity.MealCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MealCheckInRepository extends JpaRepository<MealCheckIn, Long> {
    
    List<MealCheckIn> findByMealScheduleId(Long mealScheduleId);
    
    List<MealCheckIn> findByUserId(Long userId);
    
    Optional<MealCheckIn> findByMealScheduleIdAndUserId(Long mealScheduleId, Long userId);
    
    boolean existsByMealScheduleIdAndUserId(Long mealScheduleId, Long userId);
    
    long countByMealScheduleId(Long mealScheduleId);
}

