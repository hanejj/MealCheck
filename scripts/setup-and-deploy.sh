#!/bin/bash

# 전체 배포 스크립트: 키 페어 생성 → EC2 인스턴스 생성 → 배포
# 사용법: ./scripts/setup-and-deploy.sh

set -e

KEY_NAME="${KEY_NAME:-mealcheck-key}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}MealCheck 전체 배포 스크립트${NC}"
echo "========================================"

# 1. 키 페어 확인/생성
echo -e "${YELLOW}[1/4] 키 페어 확인 중...${NC}"

KEY_EXISTS=$(aws ec2 describe-key-pairs --key-names "${KEY_NAME}" --region "${AWS_REGION}" 2>/dev/null || echo "")

if [ -z "${KEY_EXISTS}" ]; then
    echo "키 페어가 없습니다. 생성 중..."
    
    # 로컬에 키 파일이 있는지 확인
    if [ -f ~/.ssh/${KEY_NAME}.pem ]; then
        echo -e "${YELLOW}로컬 키 파일이 있습니다. AWS에 업로드 중...${NC}"
        aws ec2 import-key-pair \
            --key-name "${KEY_NAME}" \
            --public-key-material fileb://~/.ssh/${KEY_NAME}.pem.pub \
            --region "${AWS_REGION}" 2>/dev/null || {
            echo -e "${RED}키 업로드 실패. 새 키를 생성합니다.${NC}"
            rm -f ~/.ssh/${KEY_NAME}.pem ~/.ssh/${KEY_NAME}.pem.pub
        }
    fi
    
    # 키가 없으면 새로 생성
    if [ ! -f ~/.ssh/${KEY_NAME}.pem ]; then
        echo "새 키 페어 생성 중..."
        mkdir -p ~/.ssh
        aws ec2 create-key-pair \
            --key-name "${KEY_NAME}" \
            --query 'KeyMaterial' \
            --output text \
            --region "${AWS_REGION}" > ~/.ssh/${KEY_NAME}.pem
        
        chmod 400 ~/.ssh/${KEY_NAME}.pem
        echo -e "${GREEN}✓ 키 페어 생성 완료: ~/.ssh/${KEY_NAME}.pem${NC}"
    fi
else
    echo "키 페어가 이미 존재합니다: ${KEY_NAME}"
    if [ ! -f ~/.ssh/${KEY_NAME}.pem ]; then
        echo -e "${RED}경고: 로컬에 키 파일이 없습니다.${NC}"
        echo "AWS 콘솔에서 키를 다운로드하거나, 기존 키 파일 경로를 지정하세요."
        exit 1
    fi
fi

# 2. EC2 인스턴스 확인/생성
echo -e "${YELLOW}[2/4] EC2 인스턴스 확인 중...${NC}"

EXISTING_INSTANCE=$(aws ec2 describe-instances \
    --filters "Name=tag:Name,Values=mealcheck-server" "Name=instance-state-name,Values=running,stopped" \
    --query "Reservations[*].Instances[*].[InstanceId,PublicIpAddress,State.Name]" \
    --output text \
    --region "${AWS_REGION}" 2>/dev/null | head -1)

if [ -n "${EXISTING_INSTANCE}" ]; then
    INSTANCE_ID=$(echo "${EXISTING_INSTANCE}" | awk '{print $1}')
    INSTANCE_STATE=$(echo "${EXISTING_INSTANCE}" | awk '{print $3}')
    PUBLIC_IP=$(echo "${EXISTING_INSTANCE}" | awk '{print $2}')
    
    echo "기존 인스턴스 발견: ${INSTANCE_ID}"
    echo "상태: ${INSTANCE_STATE}"
    
    if [ "${INSTANCE_STATE}" = "stopped" ]; then
        echo "인스턴스 시작 중..."
        aws ec2 start-instances --instance-ids "${INSTANCE_ID}" --region "${AWS_REGION}" > /dev/null
        aws ec2 wait instance-running --instance-ids "${INSTANCE_ID}" --region "${AWS_REGION}"
        PUBLIC_IP=$(aws ec2 describe-instances \
            --instance-ids "${INSTANCE_ID}" \
            --query "Reservations[0].Instances[0].PublicIpAddress" \
            --output text \
            --region "${AWS_REGION}")
    fi
    
    echo -e "${GREEN}✓ 인스턴스 준비 완료: ${PUBLIC_IP}${NC}"
else
    echo "EC2 인스턴스 생성 중..."
    ./scripts/create-ec2-instance.sh "${KEY_NAME}"
    
    # 퍼블릭 IP 가져오기
    INSTANCE_ID=$(aws ec2 describe-instances \
        --filters "Name=tag:Name,Values=mealcheck-server" "Name=instance-state-name,Values=running" \
        --query "Reservations[0].Instances[0].InstanceId" \
        --output text \
        --region "${AWS_REGION}")
    
    PUBLIC_IP=$(aws ec2 describe-instances \
        --instance-ids "${INSTANCE_ID}" \
        --query "Reservations[0].Instances[0].PublicIpAddress" \
        --output text \
        --region "${AWS_REGION}")
    
    echo -e "${YELLOW}인스턴스가 완전히 준비될 때까지 30초 대기...${NC}"
    sleep 30
fi

# 3. 배포
echo -e "${YELLOW}[3/4] 애플리케이션 배포 중...${NC}"
./scripts/deploy-to-ec2.sh "${PUBLIC_IP}" ~/.ssh/${KEY_NAME}.pem

# 4. 완료
echo ""
echo -e "${GREEN}========================================"
echo "배포 완료!"
echo "========================================${NC}"
echo ""
echo "접속 URL:"
echo "  http://${PUBLIC_IP}"
echo ""
echo "인스턴스 정보:"
echo "  인스턴스 ID: ${INSTANCE_ID}"
echo "  퍼블릭 IP: ${PUBLIC_IP}"
echo ""
echo "관리 명령어:"
echo "  상태 확인: ssh -i ~/.ssh/${KEY_NAME}.pem ec2-user@${PUBLIC_IP} 'cd ~/meal-check && docker-compose -f docker-compose.prod.yml ps'"
echo "  로그 확인: ssh -i ~/.ssh/${KEY_NAME}.pem ec2-user@${PUBLIC_IP} 'cd ~/meal-check && docker-compose -f docker-compose.prod.yml logs -f'"
echo ""

