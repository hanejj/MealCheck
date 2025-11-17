package com.mealcheck.service;

import com.mealcheck.config.DemoAccountGuard;
import com.mealcheck.dto.MealCheckDTO;
import com.mealcheck.entity.MealCheck;
import com.mealcheck.entity.MealCheck.MealType;
import com.mealcheck.entity.User;
import com.mealcheck.repository.MealCheckRepository;
import com.mealcheck.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MealCheckService {
    
    private final MealCheckRepository mealCheckRepository;
    private final UserRepository userRepository;
    private final DemoAccountGuard demoAccountGuard;
    
    public List<MealCheckDTO> getAllMealChecks() {
        return mealCheckRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<MealCheckDTO> getMealChecksByDate(LocalDate date) {
        return mealCheckRepository.findByMealDate(date).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<MealCheckDTO> getTodayMealChecks() {
        return getMealChecksByDate(LocalDate.now());
    }
    
    public List<MealCheckDTO> getMealChecksByUser(Long userId) {
        return mealCheckRepository.findByUserId(userId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public MealCheckDTO getMealCheckById(Long id) {
        MealCheck mealCheck = mealCheckRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("식사 체크를 찾을 수 없습니다: " + id));
        return convertToDTO(mealCheck);
    }
    
    @Transactional
    public MealCheckDTO createMealCheck(MealCheckDTO dto) {
        demoAccountGuard.checkNotDemoUser();
        User user = userRepository.findById(dto.getUserId())
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + dto.getUserId()));
        
        // 중복 체크
        mealCheckRepository.findByUserIdAndMealDateAndMealType(
            dto.getUserId(), dto.getMealDate(), dto.getMealType()
        ).ifPresent(mc -> {
            throw new RuntimeException("이미 등록된 식사 체크입니다");
        });
        
        MealCheck mealCheck = convertToEntity(dto, user);
        MealCheck saved = mealCheckRepository.save(mealCheck);
        return convertToDTO(saved);
    }
    
    @Transactional
    public MealCheckDTO updateMealCheck(Long id, MealCheckDTO dto) {
        demoAccountGuard.checkNotDemoUser();
        MealCheck mealCheck = mealCheckRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("식사 체크를 찾을 수 없습니다: " + id));
        
        mealCheck.setChecked(dto.getChecked());
        mealCheck.setNote(dto.getNote());
        
        MealCheck updated = mealCheckRepository.save(mealCheck);
        return convertToDTO(updated);
    }
    
    @Transactional
    public void deleteMealCheck(Long id) {
        demoAccountGuard.checkNotDemoUser();
        if (!mealCheckRepository.existsById(id)) {
            throw new RuntimeException("식사 체크를 찾을 수 없습니다: " + id);
        }
        mealCheckRepository.deleteById(id);
    }
    
    public Map<String, Object> getStatistics(LocalDate startDate, LocalDate endDate) {
        List<MealCheck> checks = mealCheckRepository.findByMealDateBetween(startDate, endDate);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", checks.size());
        stats.put("checked", checks.stream().filter(MealCheck::getChecked).count());
        
        Map<MealType, Long> byType = checks.stream()
            .filter(MealCheck::getChecked)
            .collect(Collectors.groupingBy(MealCheck::getMealType, Collectors.counting()));
        stats.put("byType", byType);
        
        return stats;
    }
    
    private MealCheckDTO convertToDTO(MealCheck mealCheck) {
        MealCheckDTO dto = new MealCheckDTO();
        dto.setId(mealCheck.getId());
        dto.setUserId(mealCheck.getUser().getId());
        dto.setUserName(mealCheck.getUser().getName());
        dto.setMealDate(mealCheck.getMealDate());
        dto.setMealType(mealCheck.getMealType());
        dto.setChecked(mealCheck.getChecked());
        dto.setNote(mealCheck.getNote());
        dto.setCreatedAt(mealCheck.getCreatedAt());
        return dto;
    }
    
    private MealCheck convertToEntity(MealCheckDTO dto, User user) {
        MealCheck mealCheck = new MealCheck();
        mealCheck.setUser(user);
        mealCheck.setMealDate(dto.getMealDate());
        mealCheck.setMealType(dto.getMealType());
        mealCheck.setChecked(dto.getChecked() != null ? dto.getChecked() : false);
        mealCheck.setNote(dto.getNote());
        return mealCheck;
    }
}

