package com.mealcheck.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealCheckInRequest {
    
    @NotNull(message = "스케줄 ID는 필수입니다")
    private Long mealScheduleId;
    
    private String note;
}

