package com.mealcheck.service;

import com.mealcheck.config.DemoAccountGuard;
import com.mealcheck.config.JwtTokenProvider;
import com.mealcheck.dto.AuthResponse;
import com.mealcheck.dto.LoginRequest;
import com.mealcheck.dto.RegisterRequest;
import com.mealcheck.entity.User;
import com.mealcheck.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private DemoAccountGuard demoAccountGuard;

    public AuthResponse register(RegisterRequest request) {
        // 아이디 중복 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 사용 중인 아이디입니다");
        }

        // 모든 신규 사용자는 승인 대기 상태로 생성
        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDepartment(request.getDepartment());
        user.setRole("USER");
        user.setApproved(false); // 관리자 승인 필요
        user.setActive(false); // 승인 전에는 비활성 상태

        userRepository.save(user);

        // 모든 사용자는 승인 대기 메시지
        throw new RuntimeException("PENDING_APPROVAL");
    }

    public AuthResponse login(LoginRequest request) {
        // 인증 처리
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 사용자 정보 조회
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // JWT 토큰 생성
        String token = tokenProvider.generateToken(user.getUsername());

        return new AuthResponse(
            token,
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getDepartment(),
            user.getRole(),
            user.getActive()
        );
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }

    public List<User> getPendingUsers() {
        return userRepository.findByApprovedFalse();
    }

    public void approveUser(Long userId) {
        demoAccountGuard.checkNotDemoUser();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        user.setApproved(true);
        user.setActive(true); // 승인 후 활성화
        userRepository.save(user);
    }

    public void rejectUser(Long userId) {
        demoAccountGuard.checkNotDemoUser();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        userRepository.delete(user);
    }

    public boolean checkUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
}

