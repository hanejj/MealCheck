package com.mealcheck.dto;

import com.mealcheck.entity.MealSchedule;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealScheduleRequest {
    
    @NotNull(message = "식사 날짜는 필수입니다")
    private LocalDate mealDate;
    
    @NotNull(message = "식사 타입은 필수입니다")
    private MealSchedule.MealType mealType;
    
    private String description;
    private Boolean active = true;
    private LocalDateTime deadline;
}

