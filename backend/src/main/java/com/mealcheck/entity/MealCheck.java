package com.mealcheck.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_checks", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "meal_date", "meal_type"}))
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealCheck {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDate mealDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MealType mealType;
    
    @Column(nullable = false)
    private Boolean checked = false;
    
    @Column(length = 200)
    private String note;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum MealType {
        BREAKFAST,  // 아침
        LUNCH,      // 점심
        DINNER      // 저녁
    }
}

