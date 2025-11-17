package com.mealcheck.service;

import com.mealcheck.config.DemoAccountGuard;
import com.mealcheck.dto.MealCheckDTO;
import com.mealcheck.repository.MealCheckRepository;
import com.mealcheck.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MealCheckServiceDemoGuardTest {

    @Mock
    private MealCheckRepository mealCheckRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DemoAccountGuard demoAccountGuard;

    @InjectMocks
    private MealCheckService mealCheckService;

    @Test
    void createMealCheck_shouldCallDemoAccountGuard() {
        MealCheckDTO dto = new MealCheckDTO();
        dto.setUserId(1L);
        dto.setMealDate(LocalDate.now());

        try {
            mealCheckService.createMealCheck(dto);
        } catch (Exception ignored) {
            // 저장 과정에서 발생하는 예외는 이 테스트의 관심사가 아님
        }

        verify(demoAccountGuard, times(1)).checkNotDemoUser();
    }
}


