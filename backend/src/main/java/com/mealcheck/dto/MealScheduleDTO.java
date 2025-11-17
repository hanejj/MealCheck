package com.mealcheck.dto;

import com.mealcheck.entity.MealSchedule.MealType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealScheduleDTO {
    private Long id;
    
    @NotNull(message = "식사 날짜는 필수입니다")
    private LocalDate mealDate;
    
    @NotNull(message = "식사 타입은 필수입니다")
    private MealType mealType;
    
    private String description;
    private Boolean active;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    
    // 통계 필드
    private Long totalParticipants;
    private Long checkedCount;
    
    // 현재 사용자의 체크 여부
    private Boolean currentUserChecked;
}
