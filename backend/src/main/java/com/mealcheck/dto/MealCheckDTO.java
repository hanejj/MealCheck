package com.mealcheck.dto;

import com.mealcheck.entity.MealCheck.MealType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealCheckDTO {
    
    private Long id;
    
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;
    
    private String userName;
    
    @NotNull(message = "식사 날짜는 필수입니다")
    private LocalDate mealDate;
    
    @NotNull(message = "식사 타입은 필수입니다")
    private MealType mealType;
    
    private Boolean checked;
    
    @Size(max = 200, message = "메모는 200자 이내여야 합니다")
    private String note;
    
    private LocalDateTime createdAt;
}

