package com.mealcheck.service;

import com.mealcheck.config.DemoAccountGuard;
import com.mealcheck.dto.MealCheckDTO;
import com.mealcheck.entity.MealCheck;
import com.mealcheck.entity.User;
import com.mealcheck.repository.MealCheckRepository;
import com.mealcheck.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealCheckServiceTest {

    @Mock
    private MealCheckRepository mealCheckRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DemoAccountGuard demoAccountGuard;

    @InjectMocks
    private MealCheckService mealCheckService;

    @Test
    void createMealCheck_shouldThrowWhenDuplicateExists() {
        MealCheckDTO dto = new MealCheckDTO();
        dto.setUserId(1L);
        dto.setMealDate(LocalDate.now());
        dto.setMealType(MealCheck.MealType.LUNCH);

        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(mealCheckRepository.findByUserIdAndMealDateAndMealType(1L, dto.getMealDate(), dto.getMealType()))
                .thenReturn(Optional.of(new MealCheck()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mealCheckService.createMealCheck(dto));

        assertEquals("이미 등록된 식사 체크입니다", ex.getMessage());
    }

    @Test
    void createMealCheck_shouldSaveWhenNoDuplicate() {
        MealCheckDTO dto = new MealCheckDTO();
        dto.setUserId(1L);
        dto.setMealDate(LocalDate.now());
        dto.setMealType(MealCheck.MealType.LUNCH);

        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(mealCheckRepository.findByUserIdAndMealDateAndMealType(1L, dto.getMealDate(), dto.getMealType()))
                .thenReturn(Optional.empty());
        when(mealCheckRepository.save(any(MealCheck.class))).thenAnswer(invocation -> {
            MealCheck mc = invocation.getArgument(0);
            mc.setId(100L);
            return mc;
        });

        MealCheckDTO result = mealCheckService.createMealCheck(dto);

        assertEquals(100L, result.getId());
        assertEquals(1L, result.getUserId());
    }
}


