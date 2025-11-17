#!/bin/bash

# EC2 t3.micro 인스턴스 생성 스크립트 (프리티어)
# 사용법: ./scripts/create-ec2-instance.sh [KEY_NAME]

set -e

KEY_NAME="${1}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
INSTANCE_TYPE="${INSTANCE_TYPE:-t3.micro}"
AMI_ID="${AMI_ID:-}"  # 자동 감지

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}EC2 인스턴스 생성 스크립트${NC}"
echo "========================================"

# 키 페어 확인
if [ -z "${KEY_NAME}" ]; then
    echo -e "${YELLOW}사용 가능한 키 페어:${NC}"
    aws ec2 describe-key-pairs --query "KeyPairs[*].KeyName" --output table
    echo ""
    echo -e "${RED}사용법: $0 <KEY_NAME>${NC}"
    echo "예시: $0 my-key"
    exit 1
fi

# 키 페어 존재 확인
if ! aws ec2 describe-key-pairs --key-names "${KEY_NAME}" --region "${AWS_REGION}" > /dev/null 2>&1; then
    echo -e "${RED}키 페어 '${KEY_NAME}'를 찾을 수 없습니다.${NC}"
    exit 1
fi

echo "리전: ${AWS_REGION}"
echo "인스턴스 타입: ${INSTANCE_TYPE} (프리티어)"
echo "키 페어: ${KEY_NAME}"
echo "========================================"

# 최신 Amazon Linux 2023 AMI ID 가져오기
if [ -z "${AMI_ID}" ]; then
    echo -e "${YELLOW}최신 Amazon Linux 2023 AMI 검색 중...${NC}"
    AMI_ID=$(aws ec2 describe-images \
        --owners amazon \
        --filters "Name=name,Values=al2023-ami-*-x86_64" "Name=state,Values=available" \
        --query "Images | sort_by(@, &CreationDate) | [-1].ImageId" \
        --output text \
        --region "${AWS_REGION}")
    
    if [ -z "${AMI_ID}" ]; then
        # Amazon Linux 2로 대체
        echo -e "${YELLOW}Amazon Linux 2 AMI 검색 중...${NC}"
        AMI_ID=$(aws ec2 describe-images \
            --owners amazon \
            --filters "Name=name,Values=amzn2-ami-hvm-*-x86_64-gp2" "Name=state,Values=available" \
            --query "Images | sort_by(@, &CreationDate) | [-1].ImageId" \
            --output text \
            --region "${AWS_REGION}")
    fi
fi

if [ -z "${AMI_ID}" ]; then
    echo -e "${RED}AMI를 찾을 수 없습니다.${NC}"
    exit 1
fi

echo "AMI ID: ${AMI_ID}"

# 보안 그룹 생성
SG_NAME="mealcheck-sg"
echo -e "${YELLOW}보안 그룹 확인/생성 중...${NC}"

SG_ID=$(aws ec2 describe-security-groups \
    --filters "Name=group-name,Values=${SG_NAME}" \
    --query "SecurityGroups[0].GroupId" \
    --output text \
    --region "${AWS_REGION}" 2>/dev/null || echo "")

if [ -z "${SG_ID}" ] || [ "${SG_ID}" = "None" ]; then
    echo "보안 그룹 생성 중..."
    SG_ID=$(aws ec2 create-security-group \
        --group-name "${SG_NAME}" \
        --description "MealCheck application security group" \
        --query 'GroupId' \
        --output text \
        --region "${AWS_REGION}")
    
    # SSH 인바운드 규칙 추가
    aws ec2 authorize-security-group-ingress \
        --group-id "${SG_ID}" \
        --protocol tcp \
        --port 22 \
        --cidr 0.0.0.0/0 \
        --region "${AWS_REGION}" > /dev/null
    
    # HTTP 인바운드 규칙 추가
    aws ec2 authorize-security-group-ingress \
        --group-id "${SG_ID}" \
        --protocol tcp \
        --port 80 \
        --cidr 0.0.0.0/0 \
        --region "${AWS_REGION}" > /dev/null
    
    # Backend API 인바운드 규칙 추가 (선택사항)
    aws ec2 authorize-security-group-ingress \
        --group-id "${SG_ID}" \
        --protocol tcp \
        --port 8080 \
        --cidr 0.0.0.0/0 \
        --region "${AWS_REGION}" > /dev/null
    
    echo -e "${GREEN}✓ 보안 그룹 생성 완료${NC}"
else
    echo "보안 그룹이 이미 존재합니다: ${SG_ID}"
fi

# User Data 스크립트 생성
USER_DATA=$(cat << 'EOF'
#!/bin/bash
yum update -y
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
EOF
)

# 인스턴스 생성
echo -e "${YELLOW}EC2 인스턴스 생성 중... (약 1-2분 소요)${NC}"

INSTANCE_ID=$(aws ec2 run-instances \
    --image-id "${AMI_ID}" \
    --instance-type "${INSTANCE_TYPE}" \
    --key-name "${KEY_NAME}" \
    --security-group-ids "${SG_ID}" \
    --user-data "${USER_DATA}" \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=mealcheck-server},{Key=Project,Value=mealcheck}]" \
    --query 'Instances[0].InstanceId' \
    --output text \
    --region "${AWS_REGION}")

if [ -z "${INSTANCE_ID}" ] || [ "${INSTANCE_ID}" = "None" ]; then
    echo -e "${RED}인스턴스 생성 실패${NC}"
    exit 1
fi

echo -e "${GREEN}✓ 인스턴스 생성 완료: ${INSTANCE_ID}${NC}"
echo -e "${YELLOW}인스턴스가 시작될 때까지 대기 중...${NC}"

# 인스턴스가 running 상태가 될 때까지 대기
aws ec2 wait instance-running --instance-ids "${INSTANCE_ID}" --region "${AWS_REGION}"

# 퍼블릭 IP 가져오기
PUBLIC_IP=$(aws ec2 describe-instances \
    --instance-ids "${INSTANCE_ID}" \
    --query "Reservations[0].Instances[0].PublicIpAddress" \
    --output text \
    --region "${AWS_REGION}")

echo ""
echo -e "${GREEN}========================================"
echo "EC2 인스턴스 생성 완료!"
echo "========================================${NC}"
echo ""
echo "인스턴스 ID: ${INSTANCE_ID}"
echo "퍼블릭 IP: ${PUBLIC_IP}"
echo ""
echo "다음 단계:"
echo "  1. 인스턴스가 완전히 준비될 때까지 약 1-2분 대기"
echo "  2. 배포 스크립트 실행:"
echo "     ./scripts/deploy-to-ec2.sh ${PUBLIC_IP} ~/.ssh/${KEY_NAME}.pem"
echo ""
echo -e "${YELLOW}⚠️  SSH 키 파일 경로 확인:${NC}"
echo "  ~/.ssh/${KEY_NAME}.pem 또는 ~/.ssh/${KEY_NAME}"
echo ""

