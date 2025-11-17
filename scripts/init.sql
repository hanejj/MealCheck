-- MealCheck 데이터베이스 초기화 스크립트

-- 데이터베이스 생성 (이미 docker-compose에서 생성되지만 백업용)
CREATE DATABASE IF NOT EXISTS mealcheck CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mealcheck;

-- 샘플 데이터 삽입 (선택사항)
-- 실제 운영 환경에서는 이 부분을 제거하거나 주석 처리하세요

-- Users 테이블은 JPA가 자동으로 생성하므로, 앱 실행 후 샘플 데이터를 삽입할 수 있습니다

