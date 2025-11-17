package com.mealcheck.service;

import com.mealcheck.config.DemoAccountGuard;
import com.mealcheck.dto.ChangePasswordRequest;
import com.mealcheck.dto.CreateUserRequest;
import com.mealcheck.dto.UserDTO;
import com.mealcheck.entity.User;
import com.mealcheck.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DemoAccountGuard demoAccountGuard;

    @InjectMocks
    private UserService userService;

    @Test
    void getActiveUsers_shouldReturnOnlyActiveApprovedUsers() {
        User active = new User();
        active.setActive(true);
        active.setApproved(true);

        User inactive = new User();
        inactive.setActive(false);
        inactive.setApproved(true);

        when(userRepository.findByApprovedTrue()).thenReturn(Arrays.asList(active, inactive));

        List<UserDTO> result = userService.getActiveUsers();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getActive());
    }

    @Test
    void createUser_shouldEncodePasswordAndSetDefaults() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("user1");
        request.setName("User 1");
        request.setPassword("plain");
        request.setDepartment("Dev");

        when(userRepository.existsByUsername("user1")).thenReturn(false);
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO dto = userService.createUser(request);

        assertNotNull(dto);
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("plain");
    }

    @Test
    void changePassword_shouldUpdatePasswordWhenCurrentMatches() {
        User user = new User();
        user.setId(1L);
        user.setPassword("encoded_old");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old");
        request.setNewPassword("new");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded_old")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("encoded_new");

        userService.changePassword(1L, request);

        assertEquals("encoded_new", user.getPassword());
        verify(userRepository).save(user);
    }
}


