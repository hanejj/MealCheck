#!/bin/bash

# 완전 정리 후 재배포 스크립트
# AWS 리소스를 모두 삭제하고 새로 배포
# 사용법: ./scripts/fresh-deploy.sh [--delete-security-group]

set -e

DELETE_SG="${1}"
KEY_NAME="${KEY_NAME:-mealcheck-key}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}MealCheck - 완전 정리 후 재배포${NC}"
echo "========================================"
echo ""

# 1. AWS 리소스 정리
echo -e "${YELLOW}[1/2] AWS 리소스 정리 중...${NC}"
export AUTO_CONFIRM=yes
if [ "${DELETE_SG}" = "--delete-security-group" ]; then
    ./scripts/cleanup-aws-resources.sh --delete-security-group
else
    ./scripts/cleanup-aws-resources.sh
fi

# 잠시 대기 (리소스 삭제 완료 대기)
echo ""
echo -e "${YELLOW}리소스 삭제 완료 대기 중... (10초)${NC}"
sleep 10

# 2. 새로 배포
echo ""
echo -e "${YELLOW}[2/2] 새로 배포 중...${NC}"
./scripts/setup-and-deploy.sh

echo ""
echo -e "${GREEN}========================================"
echo "완전 재배포 완료!"
echo "========================================${NC}"

