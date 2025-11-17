package com.mealcheck.controller;

import com.mealcheck.dto.MealCheckDTO;
import com.mealcheck.service.MealCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meal-checks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MealCheckController {
    
    private final MealCheckService mealCheckService;
    
    @GetMapping
    public ResponseEntity<List<MealCheckDTO>> getAllMealChecks() {
        return ResponseEntity.ok(mealCheckService.getAllMealChecks());
    }
    
    @GetMapping("/today")
    public ResponseEntity<List<MealCheckDTO>> getTodayMealChecks() {
        return ResponseEntity.ok(mealCheckService.getTodayMealChecks());
    }
    
    @GetMapping("/date/{date}")
    public ResponseEntity<List<MealCheckDTO>> getMealChecksByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(mealCheckService.getMealChecksByDate(date));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MealCheckDTO>> getMealChecksByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(mealCheckService.getMealChecksByUser(userId));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MealCheckDTO> getMealCheckById(@PathVariable Long id) {
        return ResponseEntity.ok(mealCheckService.getMealCheckById(id));
    }
    
    @PostMapping
    public ResponseEntity<MealCheckDTO> createMealCheck(@Valid @RequestBody MealCheckDTO dto) {
        MealCheckDTO created = mealCheckService.createMealCheck(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<MealCheckDTO> updateMealCheck(
            @PathVariable Long id, 
            @Valid @RequestBody MealCheckDTO dto) {
        MealCheckDTO updated = mealCheckService.updateMealCheck(id, dto);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMealCheck(@PathVariable Long id) {
        mealCheckService.deleteMealCheck(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(mealCheckService.getStatistics(startDate, endDate));
    }
}

