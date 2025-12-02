package com.mealcheck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private Long id;
    
    // 로그인 아이디
    private String username;

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 100, message = "이름은 100자 이내여야 합니다")
    private String name;
    
    @Size(max = 50, message = "부서명은 50자 이내여야 합니다")
    private String department;
    
    private Boolean active;
    
    // 권한 (예: ADMIN, USER)
    private String role;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}

