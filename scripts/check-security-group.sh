#!/bin/bash

# 보안 그룹 SSH 규칙 확인 및 추가 스크립트
# 사용법: ./scripts/check-security-group.sh [INSTANCE_ID]

set -e

INSTANCE_ID="${1}"
REGION="${AWS_REGION:-ap-northeast-2}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

if [ -z "${INSTANCE_ID}" ]; then
    echo -e "${RED}사용법: $0 <INSTANCE_ID>${NC}"
    echo "예시: $0 i-0c183915238105010"
    exit 1
fi

echo -e "${GREEN}보안 그룹 SSH 규칙 확인 및 설정${NC}"
echo "========================================"

# 인스턴스 정보 조회
echo -e "${YELLOW}인스턴스 정보 조회 중...${NC}"
INSTANCE_INFO=$(aws ec2 describe-instances \
    --instance-ids "${INSTANCE_ID}" \
    --region "${REGION}" \
    --query 'Reservations[0].Instances[0]' \
    --output json)

PUBLIC_IP=$(echo "${INSTANCE_INFO}" | jq -r '.PublicIpAddress')
SECURITY_GROUP_ID=$(echo "${INSTANCE_INFO}" | jq -r '.SecurityGroups[0].GroupId')
SECURITY_GROUP_NAME=$(echo "${INSTANCE_INFO}" | jq -r '.SecurityGroups[0].GroupName')

echo "인스턴스 ID: ${INSTANCE_ID}"
echo "퍼블릭 IP: ${PUBLIC_IP}"
echo "보안 그룹: ${SECURITY_GROUP_NAME} (${SECURITY_GROUP_ID})"
echo ""

# 현재 SSH 규칙 확인
echo -e "${YELLOW}현재 SSH 규칙 확인 중...${NC}"
SSH_RULE=$(aws ec2 describe-security-groups \
    --group-ids "${SECURITY_GROUP_ID}" \
    --region "${REGION}" \
    --query 'SecurityGroups[0].IpPermissions[?FromPort==`22`]' \
    --output json)

if [ "${SSH_RULE}" != "[]" ] && [ "${SSH_RULE}" != "null" ]; then
    echo -e "${GREEN}✓ SSH 규칙이 이미 존재합니다:${NC}"
    echo "${SSH_RULE}" | jq '.'
else
    echo -e "${YELLOW}SSH 규칙이 없습니다. 추가 중...${NC}"
    
    # 현재 IP 주소 가져오기
    MY_IP=$(curl -s https://checkip.amazonaws.com/ 2>/dev/null || echo "0.0.0.0/0")
    echo "현재 IP: ${MY_IP}"
    echo ""
    
    read -p "모든 IP에서 SSH 접속을 허용하시겠습니까? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        CIDR="0.0.0.0/0"
        echo -e "${YELLOW}모든 IP에서 SSH 접속 허용 규칙 추가 중...${NC}"
    else
        CIDR="${MY_IP}/32"
        echo -e "${YELLOW}현재 IP(${MY_IP})에서만 SSH 접속 허용 규칙 추가 중...${NC}"
    fi
    
    aws ec2 authorize-security-group-ingress \
        --group-id "${SECURITY_GROUP_ID}" \
        --protocol tcp \
        --port 22 \
        --cidr "${CIDR}" \
        --region "${REGION}" \
        --output table
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ SSH 규칙 추가 완료${NC}"
    else
        echo -e "${RED}✗ SSH 규칙 추가 실패${NC}"
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}========================================"
echo "보안 그룹 규칙 확인 완료"
echo "========================================${NC}"
echo ""
echo "전체 인바운드 규칙:"
aws ec2 describe-security-groups \
    --group-ids "${SECURITY_GROUP_ID}" \
    --region "${REGION}" \
    --query 'SecurityGroups[0].IpPermissions[*].[IpProtocol,FromPort,ToPort,IpRanges[0].CidrIp]' \
    --output table

