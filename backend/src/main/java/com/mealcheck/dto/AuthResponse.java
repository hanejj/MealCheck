package com.mealcheck.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String name;
    private String department;
    private String role;
    private Boolean active;
    
    public AuthResponse(String token, Long id, String username, String name, String department, String role, Boolean active) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.name = name;
        this.department = department;
        this.role = role;
        this.active = active;
    }
}

