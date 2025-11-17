package com.mealcheck.dto;

import com.mealcheck.entity.MealSchedule.MealType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealHistoryDTO {
    private Long id;
    private Long scheduleId;
    private LocalDate mealDate;
    private MealType mealType;
    private String description;
    private Long userId;
    private String userName;
    private String userDepartment;
    private Boolean checked;
    private String note;
    private LocalDateTime checkedAt;
}

