#!/bin/bash

# SSH 연결 테스트 및 진단 스크립트
# 사용법: ./scripts/test-ssh-connection.sh [EC2_IP] [SSH_KEY_PATH]

set -e

EC2_IP="${1}"
SSH_KEY="${2:-~/.ssh/mealcheck-key.pem}"
SSH_USER="${SSH_USER:-ec2-user}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

if [ -z "${EC2_IP}" ]; then
    echo -e "${RED}사용법: $0 <EC2_IP> [SSH_KEY_PATH]${NC}"
    exit 1
fi

echo -e "${GREEN}SSH 연결 진단 스크립트${NC}"
echo "========================================"
echo "EC2 IP: ${EC2_IP}"
echo "SSH Key: ${SSH_KEY}"
echo "========================================"
echo ""

# 1. 기본 연결 테스트
echo -e "${YELLOW}1. 기본 SSH 연결 테스트...${NC}"
if ssh -i "${SSH_KEY}" \
    -o ConnectTimeout=10 \
    -o StrictHostKeyChecking=no \
    -o ServerAliveInterval=5 \
    -o ServerAliveCountMax=3 \
    "${SSH_USER}@${EC2_IP}" "echo '연결 성공'" 2>&1; then
    echo -e "${GREEN}✓ 기본 연결 성공${NC}"
else
    echo -e "${RED}✗ 기본 연결 실패${NC}"
    echo ""
    echo "다음 사항을 확인하세요:"
    echo "1. EC2 인스턴스가 running 상태인지 확인"
    echo "2. 보안 그룹에서 SSH(포트 22)가 허용되어 있는지 확인"
    echo "3. 네트워크 연결 상태 확인"
    exit 1
fi

echo ""

# 2. 인스턴스 리소스 확인
echo -e "${YELLOW}2. 인스턴스 리소스 사용량 확인...${NC}"
ssh -i "${SSH_KEY}" \
    -o ConnectTimeout=10 \
    -o StrictHostKeyChecking=no \
    -o ServerAliveInterval=5 \
    -o ServerAliveCountMax=3 \
    "${SSH_USER}@${EC2_IP}" << 'EOF'
    echo "=== CPU 정보 ==="
    top -bn1 | head -5
    echo ""
    echo "=== 메모리 정보 ==="
    free -h
    echo ""
    echo "=== 디스크 사용량 ==="
    df -h | head -5
    echo ""
    echo "=== Docker 컨테이너 상태 ==="
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.CPU}}\t{{.MemUsage}}"
EOF

echo ""

# 3. SSH 서비스 상태 확인
echo -e "${YELLOW}3. SSH 서비스 상태 확인...${NC}"
ssh -i "${SSH_KEY}" \
    -o ConnectTimeout=10 \
    -o StrictHostKeyChecking=no \
    -o ServerAliveInterval=5 \
    -o ServerAliveCountMax=3 \
    "${SSH_USER}@${EC2_IP}" << 'EOF'
    if command -v systemctl &> /dev/null; then
        echo "=== SSH 서비스 상태 ==="
        sudo systemctl status sshd --no-pager | head -10
    else
        echo "systemctl을 사용할 수 없습니다."
    fi
    echo ""
    echo "=== 활성 SSH 연결 ==="
    sudo netstat -tnpa | grep :22 | head -5 || ss -tnpa | grep :22 | head -5
EOF

echo ""

# 4. 네트워크 연결 테스트
echo -e "${YELLOW}4. 네트워크 연결 테스트...${NC}"
ssh -i "${SSH_KEY}" \
    -o ConnectTimeout=10 \
    -o StrictHostKeyChecking=no \
    -o ServerAliveInterval=5 \
    -o ServerAliveCountMax=3 \
    "${SSH_USER}@${EC2_IP}" << 'EOF'
    echo "=== 네트워크 인터페이스 ==="
    ip addr show | grep -E "inet |state " | head -5
    echo ""
    echo "=== 네트워크 통계 ==="
    netstat -s | head -10 || ss -s
EOF

echo ""
echo -e "${GREEN}========================================"
echo "진단 완료"
echo "========================================${NC}"

