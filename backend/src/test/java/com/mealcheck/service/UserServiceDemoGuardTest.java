package com.mealcheck.service;

import com.mealcheck.config.DemoAccountGuard;
import com.mealcheck.dto.CreateUserRequest;
import com.mealcheck.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceDemoGuardTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DemoAccountGuard demoAccountGuard;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_shouldCallDemoAccountGuard() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("user1");
        request.setName("User 1");
        request.setPassword("password");
        request.setDepartment("Dev");

        try {
            userService.createUser(request);
        } catch (Exception ignored) {
            // 저장 과정에서 발생하는 예외는 이 테스트의 관심사가 아님
        }

        verify(demoAccountGuard, times(1)).checkNotDemoUser();
    }
}


