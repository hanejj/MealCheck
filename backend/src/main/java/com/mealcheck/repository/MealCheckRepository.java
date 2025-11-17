package com.mealcheck.repository;

import com.mealcheck.entity.MealCheck;
import com.mealcheck.entity.MealCheck.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealCheckRepository extends JpaRepository<MealCheck, Long> {
    
    List<MealCheck> findByMealDate(LocalDate mealDate);
    
    List<MealCheck> findByUserId(Long userId);
    
    List<MealCheck> findByMealDateBetween(LocalDate startDate, LocalDate endDate);
    
    Optional<MealCheck> findByUserIdAndMealDateAndMealType(
        Long userId, LocalDate mealDate, MealType mealType
    );
    
    @Query("SELECT mc FROM MealCheck mc WHERE mc.user.id = :userId " +
           "AND mc.mealDate = :mealDate")
    List<MealCheck> findByUserIdAndMealDate(
        @Param("userId") Long userId, 
        @Param("mealDate") LocalDate mealDate
    );
    
    @Query("SELECT COUNT(mc) FROM MealCheck mc WHERE mc.mealDate = :mealDate " +
           "AND mc.mealType = :mealType AND mc.checked = true")
    Long countCheckedByDateAndType(
        @Param("mealDate") LocalDate mealDate, 
        @Param("mealType") MealType mealType
    );
}

