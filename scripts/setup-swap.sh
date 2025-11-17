#!/bin/bash

# t3.micro 등 메모리 작은 EC2에서 스왑(swap) 생성/설정 스크립트
# 사용법 (EC2 안에서 실행):
#   chmod +x scripts/setup-swap.sh
#   sudo scripts/setup-swap.sh

set -e

SWAP_FILE="/swapfile"
SWAP_SIZE="2G"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}스왑(swap) 생성 및 설정 스크립트${NC}"
echo "========================================"
echo "스왑 파일: ${SWAP_FILE}"
echo "스왑 크기: ${SWAP_SIZE}"
echo "========================================"

if [ "$(id -u)" -ne 0 ]; then
  echo -e "${RED}sudo 권한이 필요합니다. root 또는 sudo로 실행하세요.${NC}"
  exit 1
fi

if swapon --show | grep -q "${SWAP_FILE}"; then
  echo -e "${YELLOW}이미 스왑이 활성화되어 있습니다:${NC}"
  swapon --show
  exit 0
fi

echo -e "${YELLOW}1) 스왑 파일 생성 중...${NC}"
fallocate -l "${SWAP_SIZE}" "${SWAP_FILE}"
chmod 600 "${SWAP_FILE}"
mkswap "${SWAP_FILE}"

echo -e "${YELLOW}2) 스왑 활성화 중...${NC}"
swapon "${SWAP_FILE}"

echo -e "${YELLOW}3) 부팅 시 자동 적용 설정 중...${NC}"
if ! grep -q "${SWAP_FILE}" /etc/fstab; then
  echo "${SWAP_FILE} swap swap defaults 0 0" >> /etc/fstab
fi

echo ""
echo -e "${GREEN}스왑 설정 완료!${NC}"
echo "현재 스왑 상태:"
swapon --show
echo ""
echo "메모리/스왑 요약:"
free -h


