package com.mealcheck.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 데모 계정은 실제 데이터 수정이 되지 않도록 막기 위한 가드 컴포넌트입니다.
 * 현재 인증된 사용자의 username 이 DEMO_USERNAME 과 같으면 예외를 던집니다.
 */
@Component
public class DemoAccountGuard {

    // 채용 담당자에게 제공할 데모 계정 아이디
    public static final String DEMO_USERNAME = "demo_admin";

    /**
     * 현재 인증된 사용자가 데모 계정이면 RuntimeException 을 던집니다.
     */
    public void checkNotDemoUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && DEMO_USERNAME.equals(authentication.getName())) {
            throw new RuntimeException("데모 계정은 실제 관리자가 아니므로 이 기능을 사용할 수 없습니다. 실제 관리자 계정으로 다시 로그인해 주세요.");
        }
    }
}


