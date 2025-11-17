package com.mealcheck.controller;

import com.mealcheck.dto.MealHistoryDTO;
import com.mealcheck.dto.MealScheduleDTO;
import com.mealcheck.dto.MealScheduleParticipantDTO;
import com.mealcheck.entity.User;
import com.mealcheck.repository.UserRepository;
import com.mealcheck.service.MealScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meal-schedules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MealScheduleController {
    
    private final MealScheduleService scheduleService;
    private final UserRepository userRepository;
    
    @GetMapping
    public ResponseEntity<List<MealScheduleDTO>> getAllSchedules() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<MealScheduleDTO>> getActiveSchedules() {
        return ResponseEntity.ok(scheduleService.getActiveSchedules());
    }
    
    @GetMapping("/upcoming")
    public ResponseEntity<List<MealScheduleDTO>> getUpcomingSchedules() {
        return ResponseEntity.ok(scheduleService.getUpcomingSchedules());
    }
    
    @GetMapping("/date/{date}")
    public ResponseEntity<List<MealScheduleDTO>> getSchedulesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        return ResponseEntity.ok(scheduleService.getSchedulesByDate(date, user.getId()));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MealScheduleDTO> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }
    
    @PostMapping
    public ResponseEntity<MealScheduleDTO> createSchedule(
            @Valid @RequestBody MealScheduleDTO dto,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        MealScheduleDTO created = scheduleService.createSchedule(dto, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<MealScheduleDTO> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody MealScheduleDTO dto) {
        MealScheduleDTO updated = scheduleService.updateSchedule(id, dto);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
    
    // 참여자 관련 엔드포인트
    @GetMapping("/{id}/participants")
    public ResponseEntity<List<MealScheduleParticipantDTO>> getParticipants(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getParticipantsBySchedule(id));
    }
    
    @GetMapping("/{id}/participants/checked")
    public ResponseEntity<List<MealScheduleParticipantDTO>> getCheckedParticipants(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getCheckedParticipants(id));
    }
    
    @PostMapping("/{id}/check")
    public ResponseEntity<MealScheduleParticipantDTO> checkSchedule(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String note = request.get("note") != null ? request.get("note").toString() : null;
        
        MealScheduleParticipantDTO participant = scheduleService.checkParticipant(id, userId, note);
        return ResponseEntity.ok(participant);
    }
    
    @PostMapping("/{id}/uncheck")
    public ResponseEntity<Void> uncheckSchedule(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        scheduleService.uncheckParticipant(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    // 식사 기록 조회
    @GetMapping("/history/my")
    public ResponseEntity<List<MealHistoryDTO>> getMyMealHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        return ResponseEntity.ok(scheduleService.getUserMealHistory(user.getId(), startDate, endDate));
    }
    
    @GetMapping("/history/all")
    public ResponseEntity<List<MealHistoryDTO>> getAllMealHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(scheduleService.getAllMealHistory(startDate, endDate));
    }
    
    @GetMapping("/history/user/{userId}")
    public ResponseEntity<List<MealHistoryDTO>> getUserMealHistoryById(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(scheduleService.getUserMealHistory(userId, startDate, endDate));
    }
}
