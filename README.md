## 🍽 식사 여부 체크 프로그램 (MealCheck)

### 📌 프로젝트 개요

- **설명**: 팀원들의 **식사 여부(참석/불참)를 손쉽게 관리**하기 위한 사내용 웹 애플리케이션입니다.  
  날짜·시간대별 식사 스케줄을 등록하고, 구성원들이 참여 여부를 체크하면, 관리자는 **참석 인원·식사 기록·통계를 한눈에 확인**할 수 있습니다.
- **목적**
  - 식사 인원 파악 자동화 (엑셀/메신저 수기 정리 탈출)
  - 주·월간 식사 패턴 파악 및 비용/인원 관리 지원
- **형태**: 풀스택 개인 프로젝트

### 🙋‍♂️ 역할

- **기획 & 설계**
  - 요구사항 정의 (일반 사용자 / 관리자 / 데모 계정 시나리오)
  - ERD 설계 (User, MealSchedule, MealScheduleParticipant, MealCheck 등)
  - API 명세 작성 및 화면 흐름 설계
- **백엔드**
  - Spring Boot 기반 REST API 설계 및 구현
  - **JWT 기반 로그인 / 권한(ADMIN/USER) / 승인 대기 사용자 플로우** 구현
  - 식사 스케줄 등록/수정/삭제, 참여 체크/해제 로직 구현
  - 사용자별/전체 식사 히스토리 조회, 기간별 통계 API 구현
  - **데모 전용 관리자 계정(read-only)** 및 서버 단에서의 쓰기 차단 로직 구현
  - H2 + JUnit/Mockito를 활용한 **단위 테스트 및 기본 통합 테스트 코드 작성**
- **프론트엔드**
  - React 기반 SPA 구조 설계, 라우팅 및 PrivateRoute 구현
  - 로그인/회원가입, 대시보드, 스케줄 관리, 내 식사 이력, 사용자 관리, 통계 화면 구현
  - Axios를 통한 백엔드 API 연동 및 에러 처리 UX 설계
- **인프라 & 배포**
  - Docker/Docker Compose로 **backend + frontend + MariaDB** 컨테이너 구성
  - AWS EC2(t3.micro, 프리티어)에 Docker Compose로 배포
  - 배포 자동화를 위한 `deploy-to-ec2.sh`, `quick-redeploy.sh` 스크립트 작성

### 🔹 기술 스택 (Tech Stack)

- **Frontend**
  - **React 18**
  - JavaScript
  - React Router
  - Axios
  - CSS (커스텀 스타일)
- **Backend**
  - **Spring Boot 3.x**
  - Java 17
  - Spring Data JPA
  - Spring Security + JWT
  - Bean Validation
- **Database**
  - **MariaDB 10.11** (MySQL 호환)
  - 로컬/테스트 환경: H2
- **Infra & Tools**
  - AWS EC2 (프리티어)
  - Docker, Docker Compose
  - Maven
  - Git, GitHub
  - Postman (API 테스트)

### 📁 프로젝트 구조

```text
meal-check/
├── backend/              # Spring Boot REST API
├── frontend/             # React 웹 애플리케이션
├── scripts/              # 배포 및 유틸리티 스크립트
├── docker-compose.yml    # 로컬 개발 환경
└── docker-compose.prod.yml  # 프로덕션 배포 환경
```

### 🚀 주요 기능

- **인증/권한**
  - 회원가입, 로그인, 로그아웃 (JWT)
  - 관리자(ADMIN) / 일반 사용자(USER) 역할 분리
  - 신규 가입자는 **승인 대기 상태**로 생성, 관리자가 승인 시 활성화
- **식사 스케줄 관리 (관리자)**
  - 날짜·시간대(아침/점심/저녁)별 식사 스케줄 등록/수정/삭제
  - 스케줄별 생성자, 활성 여부, 참여 인원 수 표시
  - 중복 방지: 같은 날짜·시간대에는 한 개 스케줄만 등록 가능
- **식사 체크 (구성원)**
  - 오늘/특정 날짜에 대한 식사 스케줄 조회
  - 각 스케줄에 대해 **참석/미참석 체크 및 메모 작성**
  - 비활성 사용자(active=false)는 체크 불가
- **식사 이력 & 통계**
  - 개인별 식사 참여 이력(기간 필터) 조회
  - 전체 식사 이력 및 사용자별/기간별 통계 API
  - 스케줄별 총 활성 사용자 수, 체크 인원 수 집계
- **사용자 관리 (관리자)**
  - 사용자 목록/활성 사용자 목록 조회
  - 사용자 정보 수정, 활성/비활성 전환, 삭제
  - 승인 대기 사용자 목록 및 승인/거절 처리

### 👤 데모 계정 (읽기 전용)

채용 담당자가 직접 서비스의 모든 화면과 기능을 체험해볼 수 있도록 **읽기 전용 데모 관리자 계정**을 제공합니다.  
데모 계정의 **아이디/비밀번호는 깃허브에 공개하지 않고**, 배포 환경의 환경 변수로만 관리합니다.

### 권한 및 제약

- **가능한 것**
  - 관리자 페이지를 포함한 **모든 화면 접속**
  - 사용자 목록/통계/식사 스케줄/식사 체크 이력 등 **모든 데이터 조회**
  - 스케줄 생성/수정, 식사 체크/체크 해제, 사용자 생성/수정/삭제 등 **UI 상의 모든 버튼/폼을 눌러보는 것**

- **제한되는 것 (읽기 전용)**
  - 데모 계정으로 로그인했을 때는 서버에서 **모든 쓰기 작업(생성/수정/삭제, 비밀번호 변경 등)이 차단**됩니다.
  - 실제 데이터베이스에는 **어떠한 변경도 반영되지 않으며**, 필요 시 클라이언트에는  
    “데모 계정은 실제 관리자가 아니므로 이 기능을 사용할 수 없습니다” 라는 에러 메시지가 표시됩니다.

즉, 채용 담당자는 **실서비스와 동일한 UX를 안전하게 체험**할 수 있고, 운영 데이터는 절대 손상되지 않도록 설계되어 있습니다.

### 🏗 아키텍처 개요

- **구조**
  - Frontend (React) → Backend API (Spring Boot) → DB (MariaDB)
  - JWT 기반 인증으로, 모든 보호된 API는 토큰 검증 후 접근
- **권한/보안**
  - Spring Security로 URL/HTTP Method 별 권한 제어  
  - 예: `/api/meal-schedules` POST/PUT/DELETE → ADMIN 전용  
  - `/api/meal-schedules/history/my` → 로그인 사용자
  - `GET /api/users`, `GET /api/users/active` → 로그인 사용자 / 나머지 사용자 관리(생성·수정·삭제, 통계)는 ADMIN 전용
  - `/api/auth/pending`, `/api/auth/approve/{userId}`, `/api/auth/reject/{userId}` → ADMIN 전용
  - 데모 관리자 계정은 `DemoAccountGuard`를 통해 서비스 레이어에서 **추가로 쓰기 차단**

### 🗄 DB 설계 (요약)

- **User**
  - `username`, `name`, `department`, `role(ADMIN/USER)`,
    `approved`(승인 여부), `active`(활성 여부), 생성/수정 시각
- **MealSchedule**
  - 식사 날짜, 시간대(아침/점심/저녁), 설명, 활성 여부, 생성자 정보
- **MealScheduleParticipant**
  - 스케줄–사용자 매핑, 체크 여부, 메모, 체크 시각
- **MealCheck** (단순 체크 기록용)
  - 날짜·식사 타입별 체크 상태, 메모

### 🌐 주요 API

- **인증/권한**
  - `POST /api/auth/register` – 회원가입 (승인 대기 상태로 생성)
  - `POST /api/auth/login` – 로그인(JWT 발급)
  - `GET /api/auth/me` – 현재 로그인 사용자 정보 (로그인 사용자)
  - `GET /api/auth/pending` – 승인 대기 사용자 목록 (ADMIN)
  - `POST /api/auth/approve/{userId}` – 사용자 승인 (ADMIN)
  - `POST /api/auth/reject/{userId}` – 사용자 가입 거절/삭제 (ADMIN)
  - `GET /api/auth/check-username/{username}` – 아이디 중복 여부 확인 (비인증/공개)

- **사용자**
  - `GET /api/users` – 전체 사용자 목록 조회 (로그인 사용자, 승인된 사용자 기준)
  - `GET /api/users/active` – 활성 사용자 목록 조회 (로그인 사용자)
  - `GET /api/users/statistics` – 사용자/부서별 통계 조회 (ADMIN)
  - `POST /api/users` – 사용자 생성 (ADMIN)
  - `GET /api/users/{id}` – 사용자 상세 조회 (ADMIN)
  - `PUT /api/users/{id}` – 사용자 정보 수정 (ADMIN)
  - `DELETE /api/users/{id}` – 사용자 삭제 (ADMIN)
  - `POST /api/users/change-password` – 비밀번호 변경 (본인, 로그인 필요)

- **식사 스케줄**
  - `GET /api/meal-schedules/upcoming` – 다가오는 스케줄 목록
  - `GET /api/meal-schedules/history/my` – 내 식사 이력 조회
  - `POST /api/meal-schedules` – 스케줄 생성 (ADMIN)
  - `PUT /api/meal-schedules/{id}` – 스케줄 수정 (ADMIN)
  - `DELETE /api/meal-schedules/{id}` – 스케줄 삭제 (ADMIN)
  - `POST /api/meal-schedules/{id}/check` – 스케줄 참여 체크
  - `POST /api/meal-schedules/{id}/uncheck` – 스케줄 참여 체크 해제

- **식사 체크 (단순 기록용 `MealCheck`)**
  - `GET /api/meal-checks` – 전체 식사 체크 기록 목록 조회 (로그인 사용자)
  - `GET /api/meal-checks/today` – 오늘 날짜 기준 식사 체크 기록 조회 (로그인 사용자)
  - `GET /api/meal-checks/date/{date}` – 특정 날짜의 식사 체크 기록 조회 (로그인 사용자)
  - `GET /api/meal-checks/user/{userId}` – 특정 사용자의 식사 체크 기록 조회 (로그인 사용자)
  - `GET /api/meal-checks/{id}` – 단일 식사 체크 상세 조회 (로그인 사용자)
  - `POST /api/meal-checks` – 식사 체크 생성 (로그인 사용자)
  - `PUT /api/meal-checks/{id}` – 식사 체크 수정 (로그인 사용자)
  - `DELETE /api/meal-checks/{id}` – 식사 체크 삭제 (로그인 사용자)
  - `GET /api/meal-checks/statistics?startDate=&endDate=` – 기간별 식사 통계 조회 (로그인 사용자)

### 🧪 테스트

- **백엔드 단위 테스트**
  - `UserServiceTest`: 활성 사용자 필터링, 비밀번호 변경 로직 검증
  - `MealScheduleServiceTest`: 현재 사용자 체크 여부, 비활성 사용자 체크 차단 검증
  - `MealCheckServiceTest`: 중복 체크 방지, 정상 저장 검증
- **데모 계정 가드 테스트**
  - `DemoAccountGuardTest`: demo_admin 계정일 때 예외 발생 여부
  - `UserServiceDemoGuardTest`, `MealScheduleServiceDemoGuardTest`, `MealCheckServiceDemoGuardTest`, `AuthServiceDemoGuardTest`:  
    서비스 메서드들이 데모 가드를 반드시 호출하는지 검증
  - `DataInitializerTest`: admin / demo_admin 계정 초기화 로직 검증
- **프론트엔드 테스트**
  - `App.test.js`: 로그인 화면 기본 렌더링 검증 (React Testing Library)

### 🛠 로컬 개발 환경 설정

- **사전 요구사항**
  - Docker & Docker Compose
  - Node.js 18+
  - Java 17+
  - Maven 3.8+

- **1. Docker Compose로 전체 스택 실행**

```bash
docker-compose up -d
```

- **2. 개별 서비스 실행 (선택)**

```bash
# Backend
cd backend
mvn spring-boot:run

# Frontend
cd frontend
npm install
npm start
```

### 🚀 배포 (EC2 + Docker Compose)

- **환경**: AWS EC2 t3.micro (프리티어) + Docker Compose
- 자동 배포 스크립트: `scripts/deploy-to-ec2.sh`

```bash
# 1. 스크립트 실행 권한 부여
chmod +x scripts/deploy-to-ec2.sh

# 2. 배포 실행
./scripts/deploy-to-ec2.sh <EC2_IP> ~/.ssh/your-key.pem
```

### 🔒 보안 / 환경 변수 관리

- **민감 정보는 모두 환경 변수로 관리**
  - DB 접속 정보/JWT 시크릿/관리자 계정 비밀번호
- `.env` 파일은 **깃에 커밋하지 않도록 `.gitignore`에 포함**되어 있습니다.
- 공개 저장소에는 실제 비밀번호/토큰이 노출되지 않도록 설계했습니다.

### 📈 성과 및 회고

- **성과**
  - 인증/권한/데모 계정/통계까지 포함한 **실 서비스 수준의 백오피스형 웹 앱** 설계·구현
  - 테스트 코드 도입으로 핵심 비즈니스 로직과 데모 계정 보호 로직을 안정적으로 유지
  - Docker + EC2 + 배포 스크립트로 **반복 가능한 배포 파이프라인** 구축

- **개선 여지**
  - 스케줄/참여 이력에 대한 더 다양한 통계(부서별, 시간대별) 추가
  - 관리자 페이지 UI/UX를 더 직관적으로 개선 (필터링, 검색 등)

