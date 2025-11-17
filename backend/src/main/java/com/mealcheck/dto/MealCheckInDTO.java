package com.mealcheck.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealCheckInDTO {
    
    private Long id;
    private Long mealScheduleId;
    private Long userId;
    private String userName;
    private String userDepartment;
    private String note;
    private LocalDateTime createdAt;
}

