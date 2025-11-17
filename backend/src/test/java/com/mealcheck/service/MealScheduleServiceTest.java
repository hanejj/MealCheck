package com.mealcheck.service;

import com.mealcheck.config.DemoAccountGuard;
import com.mealcheck.dto.MealScheduleDTO;
import com.mealcheck.entity.MealSchedule;
import com.mealcheck.entity.MealScheduleParticipant;
import com.mealcheck.entity.User;
import com.mealcheck.repository.MealScheduleParticipantRepository;
import com.mealcheck.repository.MealScheduleRepository;
import com.mealcheck.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealScheduleServiceTest {

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
    void getSchedulesByDate_withUserId_shouldSetCurrentUserChecked() {
        User user = new User();
        user.setId(1L);

        MealSchedule schedule = new MealSchedule();
        schedule.setId(10L);
        schedule.setMealDate(LocalDate.now());
        schedule.setCreatedBy(user);
        schedule.setActive(true);

        when(scheduleRepository.findByMealDate(any(LocalDate.class)))
                .thenReturn(Collections.singletonList(schedule));
        when(userRepository.findByApprovedTrue()).thenReturn(Collections.singletonList(user));
        when(participantRepository.countByScheduleIdAndCheckedTrue(10L)).thenReturn(1L);

        MealScheduleParticipant participant = new MealScheduleParticipant();
        participant.setId(100L);
        participant.setUser(user);
        participant.setSchedule(schedule);
        participant.setChecked(true);
        when(participantRepository.findByScheduleIdAndUserId(10L, 1L)).thenReturn(Optional.of(participant));

        var result = mealScheduleService.getSchedulesByDate(LocalDate.now(), 1L);

        assertEquals(1, result.size());
        MealScheduleDTO dto = result.get(0);
        assertTrue(dto.getCurrentUserChecked());
        assertEquals(1L, dto.getCheckedCount());
        assertEquals(1L, dto.getTotalParticipants());
    }

    @Test
    void checkParticipant_shouldThrowWhenUserInactive() {
        User user = new User();
        user.setId(1L);
        user.setActive(false);

        MealSchedule schedule = new MealSchedule();
        schedule.setId(10L);

        when(scheduleRepository.findById(10L)).thenReturn(Optional.of(schedule));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> mealScheduleService.checkParticipant(10L, 1L, null));

        assertTrue(ex.getMessage().contains("비활성 사용자"));
    }
}


