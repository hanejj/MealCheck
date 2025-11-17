package com.mealcheck.repository;

import com.mealcheck.entity.MealScheduleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MealScheduleParticipantRepository extends JpaRepository<MealScheduleParticipant, Long> {
    List<MealScheduleParticipant> findByScheduleId(Long scheduleId);
    List<MealScheduleParticipant> findByUserId(Long userId);
    Optional<MealScheduleParticipant> findByScheduleIdAndUserId(Long scheduleId, Long userId);
    List<MealScheduleParticipant> findByScheduleIdAndCheckedTrue(Long scheduleId);
    long countByScheduleIdAndCheckedTrue(Long scheduleId);
}

