package com.mealcheck.service;

import com.mealcheck.config.DemoAccountGuard;
import com.mealcheck.dto.MealScheduleDTO;
import com.mealcheck.repository.MealScheduleParticipantRepository;
import com.mealcheck.repository.MealScheduleRepository;
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
class MealScheduleServiceDemoGuardTest {

    @Mock
    private MealScheduleRepository scheduleRepository;

    @Mock
    private MealScheduleParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DemoAccountGuard demoAccountGuard;

    @InjectMocks
    private MealScheduleService mealScheduleService;

    @Test
    void createSchedule_shouldCallDemoAccountGuard() {
        MealScheduleDTO dto = new MealScheduleDTO();
        dto.setMealDate(LocalDate.now());
        dto.setDescription("테스트 스케줄");

        try {
            mealScheduleService.createSchedule(dto, 1L);
        } catch (Exception ignored) {
            // 저장 과정에서 발생하는 예외는 이 테스트의 관심사가 아님
        }

        verify(demoAccountGuard, times(1)).checkNotDemoUser();
    }
}


