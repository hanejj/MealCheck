#!/bin/bash

# EC2에 Docker Compose로 배포하는 스크립트
# 사용법: ./scripts/deploy-to-ec2.sh [EC2_IP] [SSH_KEY_PATH]

set -e

EC2_IP="${1}"
SSH_KEY="${2:-~/.ssh/id_rsa}"
SSH_USER="${SSH_USER:-ec2-user}"  # Amazon Linux 2는 ec2-user, Ubuntu는 ubuntu

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

if [ -z "${EC2_IP}" ]; then
    echo -e "${RED}사용법: $0 <EC2_IP> [SSH_KEY_PATH]${NC}"
    echo "예시: $0 13.125.123.45 ~/.ssh/my-key.pem"
    exit 1
fi

echo -e "${GREEN}MealCheck - EC2 배포 스크립트${NC}"
echo "========================================"
echo "EC2 IP: ${EC2_IP}"
echo "SSH Key: ${SSH_KEY}"
echo "SSH User: ${SSH_USER}"
echo "========================================"

# SSH 연결 테스트
echo -e "${YELLOW}SSH 연결 테스트 중...${NC}"
if ! ssh -i "${SSH_KEY}" -o StrictHostKeyChecking=no "${SSH_USER}@${EC2_IP}" "echo 'SSH 연결 성공'" 2>/dev/null; then
    echo -e "${RED}SSH 연결 실패. 키 파일과 사용자를 확인하세요.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ SSH 연결 성공${NC}"

# EC2에 필요한 패키지 설치
echo -e "${YELLOW}EC2에 Docker 및 Docker Compose 설치 확인 중...${NC}"
ssh -i "${SSH_KEY}" "${SSH_USER}@${EC2_IP}" << 'EOF'
    # Docker 설치 확인
    if ! command -v docker &> /dev/null; then
        echo "Docker 설치 중..."
        sudo yum update -y
        sudo yum install -y docker
        sudo systemctl start docker
        sudo systemctl enable docker
        sudo usermod -aG docker $USER
        echo "Docker 설치 완료. 재로그인 후 다시 실행하세요."
        exit 1
    fi
    
    # Docker Compose 설치 확인
    if ! command -v docker-compose &> /dev/null; then
        echo "Docker Compose 설치 중..."
        sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        sudo chmod +x /usr/local/bin/docker-compose
        echo "Docker Compose 설치 완료"
    fi
    
    echo "Docker 및 Docker Compose 준비 완료"
EOF

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${PROJECT_DIR}"

# 로컬에서 백엔드 JAR 빌드 (EC2에서 Maven 빌드하지 않도록)
echo -e "${YELLOW}백엔드 JAR 빌드 중 (로컬 Maven)...${NC}"
if command -v mvn >/dev/null 2>&1; then
  (cd backend && mvn clean package -DskipTests)
else
  echo -e "${RED}mvn 명령을 찾을 수 없습니다. Maven을 설치하거나 PATH를 확인하세요.${NC}"
  exit 1
fi

# 프로젝트 파일 전송
echo -e "${YELLOW}프로젝트 파일 전송 중...${NC}"

# tar+scp 사용 (EC2에 rsync가 없을 수 있음)
echo "프로젝트 파일 압축 중..."

tar --exclude='node_modules' --exclude='.git' \
    --exclude='*.tar.gz' \
    -czf /tmp/meal-check.tar.gz .

echo "파일 전송 중..."
scp -i "${SSH_KEY}" /tmp/meal-check.tar.gz "${SSH_USER}@${EC2_IP}:~/"

echo "파일 압축 해제 중..."
ssh -i "${SSH_KEY}" "${SSH_USER}@${EC2_IP}" << 'EOF'
    mkdir -p ~/meal-check
    cd ~/meal-check
    tar -xzf ~/meal-check.tar.gz
    rm ~/meal-check.tar.gz
EOF

rm /tmp/meal-check.tar.gz
echo -e "${GREEN}✓ 파일 전송 완료${NC}"

# 환경 변수 파일 생성 (없는 경우)
echo -e "${YELLOW}환경 변수 파일 설정 중...${NC}"
ssh -i "${SSH_KEY}" "${SSH_USER}@${EC2_IP}" << EOF
    cd ~/meal-check
    
    # .env 파일이 없으면 생성
    if [ ! -f .env ]; then
        cat > .env << 'ENVEOF'
# 데이터베이스 설정
MYSQL_ROOT_PASSWORD=changeme_root_password
MYSQL_DATABASE=mealcheck
MYSQL_USER=mealcheck
MYSQL_PASSWORD=changeme_password

# Spring Boot 설정
SPRING_PROFILES_ACTIVE=prod

# 관리자 계정 비밀번호 (필수: 실제 배포 시 강한 비밀번호로 변경)
APP_ADMIN_PASSWORD=change_me_admin_password
APP_DEMO_ADMIN_PASSWORD=change_me_demo_password

# Frontend API URL (EC2 퍼블릭 IP로 설정)
REACT_APP_API_URL=http://${EC2_IP}:8080/api
ENVEOF
        echo ".env 파일 생성 완료"
    else
        echo ".env 파일이 이미 존재합니다"
    fi
EOF

# Docker Compose로 배포 (빌드 캐시 활용)
echo -e "${YELLOW}Docker Compose로 배포 중 (빌드 캐시 활용)...${NC}"
ssh -i "${SSH_KEY}" "${SSH_USER}@${EC2_IP}" << 'EOF'
    cd ~/meal-check
    
    # 기존 컨테이너 중지 및 제거
    docker-compose -f docker-compose.prod.yml down 2>/dev/null || true
    
    # BuildKit을 사용하여 병렬 빌드 및 캐시 최적화
    export DOCKER_BUILDKIT=1
    export COMPOSE_DOCKER_CLI_BUILD=1
    
    # Docker Compose 버전 확인 및 적절한 명령 사용
    if docker compose version &>/dev/null; then
        # Docker Compose v2 (docker compose)
        docker compose -f docker-compose.prod.yml build --parallel
        docker compose -f docker-compose.prod.yml up -d --quiet-pull
        docker compose -f docker-compose.prod.yml ps
    else
        # Docker Compose v1 (docker-compose)
        DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1 docker-compose -f docker-compose.prod.yml build --parallel
        docker-compose -f docker-compose.prod.yml up -d
        docker-compose -f docker-compose.prod.yml ps
    fi
EOF

echo ""
echo -e "${GREEN}========================================"
echo "배포 완료!"
echo "========================================${NC}"
echo ""
echo "접속 URL:"
echo "  http://${EC2_IP}"
echo ""
echo "상태 확인:"
echo "  ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} 'cd ~/meal-check && docker-compose -f docker-compose.prod.yml ps'"
echo ""
echo "로그 확인:"
echo "  ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} 'cd ~/meal-check && docker-compose -f docker-compose.prod.yml logs -f'"
echo ""
echo -e "${YELLOW}⚠️  보안 그룹 설정 확인:${NC}"
echo "  - 포트 80 (HTTP) 인바운드 규칙 추가"
echo "  - 포트 8080 (Backend API) 인바운드 규칙 추가 (선택사항)"
echo ""
echo -e "${YELLOW}⚠️  .env 파일 수정 필요:${NC}"
echo "  ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP}"
echo "  cd ~/meal-check"
echo "  vi .env  # 실제 비밀번호로 변경"

