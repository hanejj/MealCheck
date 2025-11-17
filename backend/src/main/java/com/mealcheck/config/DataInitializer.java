package com.mealcheck.config;

import com.mealcheck.entity.User;
import com.mealcheck.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    @Override
    public void run(String... args) throws Exception {
        // admin 계정 확인 및 생성/업데이트
        var adminOptional = userRepository.findByUsername("admin");

        // 관리자/데모 관리자 비밀번호는 환경 변수로부터 주입
        // 깃허브에는 실제 비밀번호를 남기지 않기 위해 기본값은 placeholder 로만 사용합니다.
        String adminPassword = getEnvOrDefault("APP_ADMIN_PASSWORD", "change_me_admin_password");
        String demoAdminPassword = getEnvOrDefault("APP_DEMO_ADMIN_PASSWORD", "change_me_demo_password");
        
        if (adminOptional.isEmpty()) {
            // admin 계정이 없으면 생성
            User admin = new User();
            admin.setUsername("admin");
            admin.setName("관리자");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            admin.setApproved(true);
            admin.setActive(true);
            admin.setDepartment("관리부");
            
            userRepository.save(admin);
            logger.info("=================================================");
            logger.info("초기 관리자 계정이 생성되었습니다.");
            logger.info("아이디: admin");
            logger.info("비밀번호는 환경 변수 APP_ADMIN_PASSWORD 값이 사용되며, 기본값은 change_me_admin_password 입니다.");
            logger.info("=================================================");
        } else {
            // admin 계정이 있으면 비밀번호를 환경 변수 값으로 재설정
            User admin = adminOptional.get();
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            admin.setApproved(true);
            admin.setActive(true);
            userRepository.save(admin);
            logger.info("=================================================");
            logger.info("관리자 계정 비밀번호가 APP_ADMIN_PASSWORD 값으로 재설정되었습니다.");
            logger.info("아이디: admin");
            logger.info("=================================================");
        }

        // 데모(admin 권한, 읽기 전용) 계정 확인 및 생성/업데이트
        var demoOptional = userRepository.findByUsername("demo_admin");

        if (demoOptional.isEmpty()) {
            User demo = new User();
            demo.setUsername("demo_admin");
            demo.setName("데모 관리자");
            demo.setPassword(passwordEncoder.encode(demoAdminPassword));
            demo.setRole("ADMIN"); // 화면은 관리자와 동일하게 사용 가능
            demo.setApproved(true);
            demo.setActive(true);
            demo.setDepartment("Demo");

            userRepository.save(demo);
            logger.info("=================================================");
            logger.info("데모 관리자 계정이 생성되었습니다.");
            logger.info("아이디: demo_admin");
            logger.info("비밀번호는 환경 변수 APP_DEMO_ADMIN_PASSWORD 값이 사용되며, 기본값은 change_me_demo_password 입니다.");
            logger.info("※ 이 계정은 읽기 전용이며, 서버에서 쓰기 작업이 차단됩니다.");
            logger.info("=================================================");
        } else {
            User demo = demoOptional.get();
            demo.setPassword(passwordEncoder.encode(demoAdminPassword));
            demo.setRole("ADMIN");
            demo.setApproved(true);
            demo.setActive(true);
            userRepository.save(demo);
            logger.info("=================================================");
            logger.info("데모 관리자 계정 비밀번호가 APP_DEMO_ADMIN_PASSWORD 값으로 재설정되었습니다.");
            logger.info("아이디: demo_admin");
            logger.info("※ 이 계정은 읽기 전용이며, 서버에서 쓰기 작업이 차단됩니다.");
            logger.info("=================================================");
        }
    }
}

