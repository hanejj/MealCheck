#!/bin/bash

# Docker Compose 재배포 스크립트 (완전 삭제 후 재배포)
# 사용법: ./scripts/redeploy.sh [EC2_IP] [SSH_KEY_PATH] [--remove-volumes]

set -e

EC2_IP="${1}"
SSH_KEY="${2:-~/.ssh/id_rsa}"
SSH_USER="${SSH_USER:-ec2-user}"
REMOVE_VOLUMES="${3}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

if [ -z "${EC2_IP}" ]; then
    echo -e "${RED}사용법: $0 <EC2_IP> [SSH_KEY_PATH] [--remove-volumes]${NC}"
    echo "예시: $0 13.125.123.45 ~/.ssh/my-key.pem"
    echo "예시: $0 13.125.123.45 ~/.ssh/my-key.pem --remove-volumes  # 데이터베이스 데이터도 삭제"
    exit 1
fi

echo -e "${GREEN}MealCheck - 완전 삭제 후 재배포 스크립트${NC}"
echo "========================================"
echo "EC2 IP: ${EC2_IP}"
echo "SSH Key: ${SSH_KEY}"
if [ "${REMOVE_VOLUMES}" = "--remove-volumes" ]; then
    echo -e "${RED}⚠️  경고: 데이터베이스 볼륨도 삭제됩니다!${NC}"
fi
echo "========================================"

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

# 프로젝트 파일 전송 (최적화: 빠른 압축, 불필요한 파일 제외)
echo -e "${YELLOW}파일 압축 중...${NC}"

# 빠른 압축 레벨(1) 사용, 더 많은 불필요한 파일 제외 (단, backend/target은 포함)
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

echo -e "${YELLOW}파일 전송 중...${NC}"
scp -i "${SSH_KEY}" -o Compression=no /tmp/meal-check.tar.gz "${SSH_USER}@${EC2_IP}:~/" 2>/dev/null

echo -e "${YELLOW}파일 압축 해제 중...${NC}"
ssh -i "${SSH_KEY}" "${SSH_USER}@${EC2_IP}" << 'EOF'
    cd ~/meal-check
    tar -xzf ~/meal-check.tar.gz 2>/dev/null
    rm -f ~/meal-check.tar.gz
EOF

rm -f /tmp/meal-check.tar.gz
echo -e "${GREEN}✓ 파일 전송 완료${NC}"

# Docker Compose 완전 삭제 및 재배포
echo -e "${YELLOW}기존 배포 삭제 중...${NC}"
if [ "${REMOVE_VOLUMES}" = "--remove-volumes" ]; then
    ssh -i "${SSH_KEY}" "${SSH_USER}@${EC2_IP}" << 'EOF'
        cd ~/meal-check
        if docker compose version &>/dev/null; then
            docker compose -f docker-compose.prod.yml down -v --remove-orphans 2>/dev/null || true
        else
            docker-compose -f docker-compose.prod.yml down -v --remove-orphans 2>/dev/null || true
        fi
EOF
else
    ssh -i "${SSH_KEY}" "${SSH_USER}@${EC2_IP}" << 'EOF'
        cd ~/meal-check
        if docker compose version &>/dev/null; then
            docker compose -f docker-compose.prod.yml down --remove-orphans 2>/dev/null || true
        else
            docker-compose -f docker-compose.prod.yml down --remove-orphans 2>/dev/null || true
        fi
EOF
fi

echo -e "${GREEN}✓ 기존 배포 삭제 완료${NC}"

# Docker Compose 재배포 (빌드 캐시 활용)
echo -e "${YELLOW}Docker Compose 재배포 중 (빌드 캐시 활용)...${NC}"
ssh -i "${SSH_KEY}" "${SSH_USER}@${EC2_IP}" << 'EOF'
    cd ~/meal-check
    # BuildKit을 사용하여 병렬 빌드 및 캐시 최적화
    export DOCKER_BUILDKIT=1
    export COMPOSE_DOCKER_CLI_BUILD=1
    if docker compose version &>/dev/null; then
        docker compose -f docker-compose.prod.yml build --parallel
        docker compose -f docker-compose.prod.yml up -d --quiet-pull
        docker compose -f docker-compose.prod.yml ps
    else
        DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1 docker-compose -f docker-compose.prod.yml build --parallel
        docker-compose -f docker-compose.prod.yml up -d
        docker-compose -f docker-compose.prod.yml ps
    fi
EOF

echo ""
echo -e "${GREEN}========================================"
echo "재배포 완료!"
echo "========================================${NC}"
echo ""
echo "접속 URL: http://${EC2_IP}"
echo ""
echo "로그 확인:"
echo "  ssh -i ${SSH_KEY} ${SSH_USER}@${EC2_IP} 'cd ~/meal-check && docker-compose -f docker-compose.prod.yml logs -f'"

