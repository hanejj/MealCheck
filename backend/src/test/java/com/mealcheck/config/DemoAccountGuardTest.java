package com.mealcheck.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DemoAccountGuardTest {

    private final DemoAccountGuard demoAccountGuard = new DemoAccountGuard();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkNotDemoUser_shouldNotThrow_whenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertDoesNotThrow(() -> demoAccountGuard.checkNotDemoUser());
    }

    @Test
    void checkNotDemoUser_shouldNotThrow_whenNormalUser() {
        var auth = new UsernamePasswordAuthenticationToken("normal_user", "password");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertDoesNotThrow(() -> demoAccountGuard.checkNotDemoUser());
    }

    @Test
    void checkNotDemoUser_shouldThrow_whenDemoAdmin() {
        var auth = new UsernamePasswordAuthenticationToken(DemoAccountGuard.DEMO_USERNAME, "password");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThrows(RuntimeException.class, () -> demoAccountGuard.checkNotDemoUser());
    }
}


