package com.mealcheck.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealScheduleParticipantDTO {
    private Long id;
    private Long scheduleId;
    private Long userId;
    private String userName;
    private String userDepartment;
    private Boolean checked;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

