package com.mealcheck.config;

import com.mealcheck.entity.User;
import com.mealcheck.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_shouldCreateDemoAdminWhenNotExists() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("demo_admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dataInitializer.run();

        // admin, demo_admin 두 계정을 모두 저장해야 함
        verify(userRepository, atLeast(2)).save(any(User.class));
        verify(userRepository).findByUsername(eq("demo_admin"));
    }

    @Test
    void run_shouldUpdateDemoAdminWhenExists() throws Exception {
        User demo = new User();
        demo.setUsername("demo_admin");
        demo.setRole("USER");
        demo.setApproved(false);
        demo.setActive(false);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("demo_admin")).thenReturn(Optional.of(demo));
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dataInitializer.run();

        assertEquals("ADMIN", demo.getRole());
        assertEquals(true, demo.getApproved());
        assertEquals(true, demo.getActive());
        verify(userRepository).save(demo);
    }
}


