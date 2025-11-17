#!/bin/bash

# 빠른 재배포 스크립트 (컨테이너를 내리지 않고 빌드 캐시 활용)
# 사용법: ./scripts/quick-redeploy.sh [EC2_IP] [SSH_KEY_PATH]

set -e

EC2_IP="${1}"
SSH_KEY="${2:-~/.ssh/id_rsa}"
SSH_USER="${SSH_USER:-ec2-user}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

if [ -z "${EC2_IP}" ]; then
    echo -e "${RED}사용법: $0 <EC2_IP> [SSH_KEY_PATH]${NC}"
    echo "예시: $0 13.125.123.45 ~/.ssh/my-key.pem"
    exit 1
fi

echo -e "${GREEN}MealCheck - 빠른 재배포 스크립트 (빌드 캐시 활용)${NC}"
echo "========================================"
echo "EC2 IP: ${EC2_IP}"
echo "SSH Key: ${SSH_KEY}"
echo "========================================"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${PROJECT_DIR}"

# 로컬에서 백엔드 JAR 빌드 (빠른 재배포용, EC2에서 Maven 빌드하지 않도록)
echo -e "${YELLOW}백엔드 JAR 빌드 중 (로컬 Maven)...${NC}"
if command -v mvn >/dev/null 2>&1; then
  (cd backend && mvn clean package -DskipTests)
else
  echo -e "${RED}mvn 명령을 찾을 수 없습니다. Maven을 설치하거나 PATH를 확인하세요.${NC}"
  exit 1
fi

# 프로젝트 파일 전송 (최적화: 빠른 압축, 불필요한 파일 제외)
echo -e "${YELLOW}변경된 파일만 전송 중...${NC}"

# 빠른 압축 레벨(1) 사용 (단, backend/target은 포함)
tar --exclude='node_modules' \
    --exclude='.git' \
    --exclude='*.tar.gz' \
    --exclude='frontend/build' \
    --exclude='frontend/.eslintcache' \
    --exclude='backend/.mvn' \
    --exclude='backend/mvnw' \
    --exclude='backend/mvnw.cmd' \
    --exclude='.idea' \
    --exclude='.vscode' \
    --exclude='*.iml' \
    --exclude='*.swp' \
    --exclude='*.swo' \
    --exclude='.DS_Store' \
    --exclude='*.log' \
    --exclude='logs' \
    --exclude='*.db' \
    --exclude='*.sqlite' \
    --exclude='.env.local' \
    --exclude='docker-compose.override.yml' \
    -czf /tmp/meal-check.tar.gz . 2>/dev/null

echo -e "${YELLOW}파일 전송 중... (진행 상황 표시)${NC}"
# SSH 연결 옵션 (안정성 강화)
SSH_OPTS="-i ${SSH_KEY} \
    -o ConnectTimeout=15 \
    -o StrictHostKeyChecking=no \
    -o ServerAliveInterval=5 \
    -o ServerAliveCountMax=6 \
    -o TCPKeepAlive=yes \
    -o BatchMode=yes \
    -o LogLevel=ERROR"

# SSH 연결 테스트 먼저 수행
echo -e "${YELLOW}SSH 연결 테스트 중...${NC}"
if ! ssh ${SSH_OPTS} "${SSH_USER}@${EC2_IP}" "echo 'SSH 연결 확인'" 2>&1; then
    echo -e "${RED}SSH 연결 실패. EC2 인스턴스 상태를 확인하세요.${NC}"
    echo "진단 스크립트 실행: ./scripts/test-ssh-connection.sh ${EC2_IP} ${SSH_KEY}"
    exit 1
fi

# scp 전송 (진행 상황 표시, 타임아웃 설정)
echo -e "${YELLOW}파일 전송 중...${NC}"
scp ${SSH_OPTS} \
    -o Compression=no \
    /tmp/meal-check.tar.gz "${SSH_USER}@${EC2_IP}:~/" 2>&1 | grep -E "(Sending|100%|error|failed)" || true

echo -e "${YELLOW}파일 압축 해제 중...${NC}"
ssh ${SSH_OPTS} "${SSH_USER}@${EC2_IP}" << 'EOF'
    cd ~/meal-check
    tar -xzf ~/meal-check.tar.gz
    rm -f ~/meal-check.tar.gz
EOF

rm -f /tmp/meal-check.tar.gz
echo -e "${GREEN}✓ 파일 전송 완료${NC}"

# Docker Compose 빠른 재배포 (빌드 캐시 활용, 컨테이너는 재시작만)
echo -e "${YELLOW}빠른 재배포 중 (빌드 캐시 활용)...${NC}"
ssh ${SSH_OPTS} "${SSH_USER}@${EC2_IP}" << 'EOF'
    cd ~/meal-check
    # BuildKit을 사용하여 병렬 빌드 및 캐시 최적화
    export DOCKER_BUILDKIT=1
    export COMPOSE_DOCKER_CLI_BUILD=1
    
    if docker compose version &>/dev/null; then
        # Docker Compose v2
        # 빌드만 수행 (캐시 활용)
        docker compose -f docker-compose.prod.yml build --parallel
        # 컨테이너 재시작 (기존 컨테이너는 유지하고 재시작만)
        docker compose -f docker-compose.prod.yml up -d --no-deps --force-recreate
        docker compose -f docker-compose.prod.yml ps
    else
        # Docker Compose v1
        DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1 docker-compose -f docker-compose.prod.yml build --parallel
        docker-compose -f docker-compose.prod.yml up -d --no-deps --force-recreate
        docker-compose -f docker-compose.prod.yml ps
    fi
EOF

echo ""
echo -e "${GREEN}========================================"
echo "빠른 재배포 완료!"
echo "========================================${NC}"
echo ""
echo "접속 URL: http://${EC2_IP}"
echo ""
echo "로그 확인:"
echo "  ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} 'cd ~/meal-check && docker-compose -f docker-compose.prod.yml logs -f'"

