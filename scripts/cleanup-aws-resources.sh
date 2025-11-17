#!/bin/bash

# AWS 리소스 완전 정리 스크립트
# EC2 인스턴스, EBS 볼륨, 보안 그룹 등을 모두 삭제
# 사용법: ./scripts/cleanup-aws-resources.sh [--delete-security-group]

set -e

DELETE_SG="${1}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
INSTANCE_NAME="mealcheck-server"
SG_NAME="mealcheck-sg"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${RED}⚠️  AWS 리소스 완전 삭제 스크립트${NC}"
echo "========================================"
echo "리전: ${AWS_REGION}"
echo "인스턴스 이름: ${INSTANCE_NAME}"
echo "보안 그룹: ${SG_NAME}"
if [ "${DELETE_SG}" = "--delete-security-group" ]; then
    echo -e "${RED}⚠️  보안 그룹도 삭제됩니다!${NC}"
fi
echo "========================================"
echo ""

# 확인 메시지
if [ "${AUTO_CONFIRM}" != "yes" ]; then
    read -p "정말로 모든 리소스를 삭제하시겠습니까? (yes/no): " CONFIRM
    if [ "${CONFIRM}" != "yes" ]; then
        echo "취소되었습니다."
        exit 0
    fi
else
    echo "자동 확인 모드: 리소스 삭제를 진행합니다."
fi

# 1. EC2 인스턴스 찾기 및 종료
echo -e "${YELLOW}[1/4] EC2 인스턴스 확인 중...${NC}"

INSTANCE_INFO=$(aws ec2 describe-instances \
    --filters "Name=tag:Name,Values=${INSTANCE_NAME}" "Name=instance-state-name,Values=running,stopped,stopping" \
    --query "Reservations[*].Instances[*].[InstanceId,State.Name]" \
    --output text \
    --region "${AWS_REGION}" 2>/dev/null | head -1)

if [ -n "${INSTANCE_INFO}" ]; then
    INSTANCE_ID=$(echo "${INSTANCE_INFO}" | awk '{print $1}')
    INSTANCE_STATE=$(echo "${INSTANCE_INFO}" | awk '{print $2}')
    
    echo "인스턴스 발견: ${INSTANCE_ID} (상태: ${INSTANCE_STATE})"
    
    # 인스턴스가 실행 중이면 종료
    if [ "${INSTANCE_STATE}" = "running" ]; then
        echo -e "${YELLOW}인스턴스 종료 중...${NC}"
        aws ec2 terminate-instances --instance-ids "${INSTANCE_ID}" --region "${AWS_REGION}" > /dev/null
        echo "인스턴스 종료 요청 완료. 삭제될 때까지 대기 중..."
        aws ec2 wait instance-terminated --instance-ids "${INSTANCE_ID}" --region "${AWS_REGION}"
        echo -e "${GREEN}✓ 인스턴스 종료 완료${NC}"
    elif [ "${INSTANCE_STATE}" = "stopping" ]; then
        echo "인스턴스가 이미 종료 중입니다. 대기 중..."
        aws ec2 wait instance-terminated --instance-ids "${INSTANCE_ID}" --region "${AWS_REGION}"
        echo -e "${GREEN}✓ 인스턴스 종료 완료${NC}"
    elif [ "${INSTANCE_STATE}" = "stopped" ]; then
        echo "인스턴스가 중지 상태입니다. 종료 중..."
        aws ec2 terminate-instances --instance-ids "${INSTANCE_ID}" --region "${AWS_REGION}" > /dev/null
        aws ec2 wait instance-terminated --instance-ids "${INSTANCE_ID}" --region "${AWS_REGION}"
        echo -e "${GREEN}✓ 인스턴스 종료 완료${NC}"
    fi
else
    echo "인스턴스를 찾을 수 없습니다."
fi

# 2. EBS 볼륨 삭제
echo -e "${YELLOW}[2/4] EBS 볼륨 확인 중...${NC}"

if [ -n "${INSTANCE_ID}" ]; then
    # 인스턴스에 연결된 볼륨 찾기
    VOLUMES=$(aws ec2 describe-volumes \
        --filters "Name=attachment.instance-id,Values=${INSTANCE_ID}" \
        --query "Volumes[*].VolumeId" \
        --output text \
        --region "${AWS_REGION}" 2>/dev/null || echo "")
    
    if [ -n "${VOLUMES}" ]; then
        echo "볼륨 발견: ${VOLUMES}"
        for VOLUME_ID in ${VOLUMES}; do
            # 볼륨이 detached 상태가 될 때까지 대기
            echo "볼륨 ${VOLUME_ID} 삭제 대기 중..."
            sleep 5
            
            # 볼륨 삭제
            if aws ec2 delete-volume --volume-id "${VOLUME_ID}" --region "${AWS_REGION}" 2>/dev/null; then
                echo -e "${GREEN}✓ 볼륨 ${VOLUME_ID} 삭제 완료${NC}"
            else
                echo -e "${YELLOW}⚠️  볼륨 ${VOLUME_ID} 삭제 실패 (이미 삭제되었거나 인스턴스에 아직 연결되어 있을 수 있음)${NC}"
            fi
        done
    else
        echo "인스턴스에 연결된 볼륨을 찾을 수 없습니다."
    fi
else
    # 인스턴스 ID가 없으면 태그로 볼륨 찾기
    echo "인스턴스 ID가 없어 태그로 볼륨 검색 중..."
    VOLUMES=$(aws ec2 describe-volumes \
        --filters "Name=tag:Name,Values=${INSTANCE_NAME}" \
        --query "Volumes[*].VolumeId" \
        --output text \
        --region "${AWS_REGION}" 2>/dev/null || echo "")
    
    if [ -n "${VOLUMES}" ]; then
        for VOLUME_ID in ${VOLUMES}; do
            VOLUME_STATE=$(aws ec2 describe-volumes \
                --volume-ids "${VOLUME_ID}" \
                --query "Volumes[0].State" \
                --output text \
                --region "${AWS_REGION}" 2>/dev/null || echo "unknown")
            
            if [ "${VOLUME_STATE}" = "available" ]; then
                echo "볼륨 ${VOLUME_ID} 삭제 중..."
                if aws ec2 delete-volume --volume-id "${VOLUME_ID}" --region "${AWS_REGION}" 2>/dev/null; then
                    echo -e "${GREEN}✓ 볼륨 ${VOLUME_ID} 삭제 완료${NC}"
                else
                    echo -e "${YELLOW}⚠️  볼륨 ${VOLUME_ID} 삭제 실패${NC}"
                fi
            else
                echo -e "${YELLOW}⚠️  볼륨 ${VOLUME_ID}는 ${VOLUME_STATE} 상태입니다 (스킵)${NC}"
            fi
        done
    else
        echo "볼륨을 찾을 수 없습니다."
    fi
fi

# 3. Elastic IP 해제 (있는 경우)
echo -e "${YELLOW}[3/4] Elastic IP 확인 중...${NC}"

if [ -n "${INSTANCE_ID}" ]; then
    ALLOCATION_IDS=$(aws ec2 describe-addresses \
        --filters "Name=instance-id,Values=${INSTANCE_ID}" \
        --query "Addresses[*].AllocationId" \
        --output text \
        --region "${AWS_REGION}" 2>/dev/null || echo "")
    
    if [ -n "${ALLOCATION_IDS}" ]; then
        for ALLOCATION_ID in ${ALLOCATION_IDS}; do
            echo "Elastic IP ${ALLOCATION_ID} 해제 중..."
            if aws ec2 release-address --allocation-id "${ALLOCATION_ID}" --region "${AWS_REGION}" 2>/dev/null; then
                echo -e "${GREEN}✓ Elastic IP 해제 완료${NC}"
            else
                echo -e "${YELLOW}⚠️  Elastic IP 해제 실패${NC}"
            fi
        done
    else
        echo "Elastic IP를 찾을 수 없습니다."
    fi
else
    echo "인스턴스 ID가 없어 Elastic IP를 확인할 수 없습니다."
fi

# 4. 보안 그룹 삭제 (옵션)
if [ "${DELETE_SG}" = "--delete-security-group" ]; then
    echo -e "${YELLOW}[4/4] 보안 그룹 삭제 중...${NC}"
    
    SG_ID=$(aws ec2 describe-security-groups \
        --filters "Name=group-name,Values=${SG_NAME}" \
        --query "SecurityGroups[0].GroupId" \
        --output text \
        --region "${AWS_REGION}" 2>/dev/null || echo "")
    
    if [ -n "${SG_ID}" ] && [ "${SG_ID}" != "None" ]; then
        echo "보안 그룹 ${SG_ID} 삭제 중..."
        
        # 보안 그룹 삭제 시도 (규칙은 자동으로 삭제됨)
        if aws ec2 delete-security-group --group-id "${SG_ID}" --region "${AWS_REGION}" 2>/dev/null; then
            echo -e "${GREEN}✓ 보안 그룹 삭제 완료${NC}"
        else
            echo -e "${YELLOW}⚠️  보안 그룹 삭제 실패 (다른 리소스에서 사용 중일 수 있음)${NC}"
            echo "   보안 그룹은 수동으로 삭제해야 할 수 있습니다."
        fi
    else
        echo "보안 그룹을 찾을 수 없습니다."
    fi
else
    echo -e "${YELLOW}[4/4] 보안 그룹은 유지됩니다 (--delete-security-group 옵션으로 삭제 가능)${NC}"
fi

echo ""
echo -e "${GREEN}========================================"
echo "리소스 정리 완료!"
echo "========================================${NC}"
echo ""
echo "다음 단계:"
echo "  ./scripts/setup-and-deploy.sh  # 새로 배포"
echo ""

