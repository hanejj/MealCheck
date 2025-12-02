package com.mealcheck.service;

import com.mealcheck.config.DemoAccountGuard;
import com.mealcheck.dto.ChangePasswordRequest;
import com.mealcheck.dto.CreateUserRequest;
import com.mealcheck.dto.UserDTO;
import com.mealcheck.entity.User;
import com.mealcheck.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoAccountGuard demoAccountGuard;
    
    public List<UserDTO> getAllUsers() {
        // 승인된 사용자 모두 반환 (활성/비활성 모두 포함)
        return userRepository.findByApprovedTrue().stream()
            .filter(user -> !DemoAccountGuard.DEMO_USERNAME.equals(user.getUsername()))
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<UserDTO> getActiveUsers() {
        // 승인되고 활성화된 사용자만 반환
        return userRepository.findByApprovedTrue().stream()
            .filter(User::getActive)
            .filter(user -> !DemoAccountGuard.DEMO_USERNAME.equals(user.getUsername()))
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + id));
        return convertToDTO(user);
    }
    
    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        demoAccountGuard.checkNotDemoUser();
        // 아이디 중복 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 사용 중인 아이디입니다");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDepartment(request.getDepartment());
        user.setRole("USER");
        user.setApproved(true); // 관리자가 직접 추가하는 경우 승인 처리
        user.setActive(request.getActive() != null ? request.getActive() : true);
        
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }
    
    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        demoAccountGuard.checkNotDemoUser();
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + id));
        
        user.setName(userDTO.getName());
        user.setDepartment(userDTO.getDepartment());
        if (userDTO.getActive() != null) {
            user.setActive(userDTO.getActive());
        }
        
        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        demoAccountGuard.checkNotDemoUser();
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("사용자를 찾을 수 없습니다: " + id);
        }
        userRepository.deleteById(id);
    }
    
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setDepartment(user.getDepartment());
        dto.setActive(user.getActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
    
    private User convertToEntity(UserDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setDepartment(dto.getDepartment());
        user.setActive(dto.getActive() != null ? dto.getActive() : true);
        return user;
    }
    
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        demoAccountGuard.checkNotDemoUser();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다");
        }
        
        // 새 비밀번호 설정
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
    
    public java.util.Map<String, Object> getUserStatistics() {
        // 승인된 사용자 중 데모 계정 제외
        List<User> approvedUsers = userRepository.findByApprovedTrue().stream()
            .filter(user -> !DemoAccountGuard.DEMO_USERNAME.equals(user.getUsername()))
            .collect(java.util.stream.Collectors.toList());
        
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalUsers", approvedUsers.size());
        stats.put("activeUsers", approvedUsers.stream().filter(User::getActive).count());
        stats.put("inactiveUsers", approvedUsers.stream().filter(u -> !u.getActive()).count());
        stats.put("approvedUsers", approvedUsers.size());
        stats.put("pendingUsers", userRepository.findByApprovedFalse().size());
        stats.put("adminUsers", approvedUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count());
        stats.put("regularUsers", approvedUsers.stream().filter(u -> "USER".equals(u.getRole())).count());
        
        // 부서별 통계 (승인된 사용자만)
        java.util.Map<String, Long> byDepartment = approvedUsers.stream()
            .filter(u -> u.getDepartment() != null && !u.getDepartment().isEmpty())
            .collect(java.util.stream.Collectors.groupingBy(
                User::getDepartment, 
                java.util.stream.Collectors.counting()
            ));
        stats.put("byDepartment", byDepartment);
        
        return stats;
    }
}

