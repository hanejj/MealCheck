package com.mealcheck.service;

import com.mealcheck.config.DemoAccountGuard;
import com.mealcheck.entity.User;
import com.mealcheck.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceDemoGuardTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DemoAccountGuard demoAccountGuard;

    @InjectMocks
    private AuthService authService;

    @Test
    void approveUser_shouldCallDemoAccountGuard() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));

        try {
            authService.approveUser(1L);
        } catch (Exception ignored) {
            // 실제 저장 예외는 이 테스트의 관심사가 아님
        }

        verify(demoAccountGuard, times(1)).checkNotDemoUser();
    }

    @Test
    void rejectUser_shouldCallDemoAccountGuard() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));

        try {
            authService.rejectUser(1L);
        } catch (Exception ignored) {
            // 실제 삭제 예외는 이 테스트의 관심사가 아님
        }

        verify(demoAccountGuard, times(1)).checkNotDemoUser();
    }
}


