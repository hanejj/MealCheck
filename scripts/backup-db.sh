#!/bin/bash

# 데이터베이스 백업 스크립트
# 사용법: ./scripts/backup-db.sh [로컬|s3]

set -e

BACKUP_METHOD="${1:-로컬}"
BACKUP_DIR="${BACKUP_DIR:-~/meal-check/backups}"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="mealcheck_backup_${DATE}.sql"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}데이터베이스 백업 스크립트${NC}"
echo "========================================"
echo "백업 방법: ${BACKUP_METHOD}"
echo "백업 파일: ${BACKUP_FILE}"
echo "========================================"

# 백업 디렉토리 생성
mkdir -p "${BACKUP_DIR}"

# 데이터베이스 백업
echo -e "${YELLOW}데이터베이스 백업 중...${NC}"
cd ~/meal-check

# .env 파일에서 비밀번호 읽기
if [ -f .env ]; then
    source .env
    DB_PASSWORD="${MYSQL_ROOT_PASSWORD:-changeme}"
else
    DB_PASSWORD="changeme"
fi

docker-compose -f docker-compose.prod.yml exec -T mysql mysqldump \
    -u root \
    -p"${DB_PASSWORD}" \
    --single-transaction \
    --routines \
    --triggers \
    mealcheck > "${BACKUP_DIR}/${BACKUP_FILE}"

# 백업 파일 압축
echo -e "${YELLOW}백업 파일 압축 중...${NC}"
gzip -f "${BACKUP_DIR}/${BACKUP_FILE}"
BACKUP_FILE_GZ="${BACKUP_FILE}.gz"

# 백업 파일 크기 확인
BACKUP_SIZE=$(du -h "${BACKUP_DIR}/${BACKUP_FILE_GZ}" | cut -f1)
echo -e "${GREEN}✓ 백업 완료: ${BACKUP_FILE_GZ} (${BACKUP_SIZE})${NC}"

# 오래된 백업 파일 삭제 (30일 이상)
echo -e "${YELLOW}오래된 백업 파일 정리 중...${NC}"
find "${BACKUP_DIR}" -name "mealcheck_backup_*.sql.gz" -mtime +30 -delete
echo -e "${GREEN}✓ 30일 이상 된 백업 파일 삭제 완료${NC}"

# S3 백업 (선택사항)
if [ "${BACKUP_METHOD}" = "s3" ]; then
    if ! command -v aws &> /dev/null; then
        echo -e "${RED}S3 백업을 위해 AWS CLI가 필요합니다.${NC}"
        exit 1
    fi
    
    S3_BUCKET="${S3_BUCKET:-mealcheck-backups}"
    echo -e "${YELLOW}S3에 백업 업로드 중...${NC}"
    
    # S3 버킷 생성 (없는 경우)
    aws s3 mb "s3://${S3_BUCKET}" 2>/dev/null || true
    
    # 백업 업로드
    aws s3 cp "${BACKUP_DIR}/${BACKUP_FILE_GZ}" "s3://${S3_BUCKET}/${BACKUP_FILE_GZ}"
    
    echo -e "${GREEN}✓ S3 백업 완료: s3://${S3_BUCKET}/${BACKUP_FILE_GZ}${NC}"
fi

echo ""
echo -e "${GREEN}========================================"
echo "백업 완료!"
echo "========================================${NC}"
echo ""
echo "백업 파일 위치: ${BACKUP_DIR}/${BACKUP_FILE_GZ}"
echo "백업 크기: ${BACKUP_SIZE}"
echo ""
echo "복원 방법:"
echo "  gunzip < ${BACKUP_DIR}/${BACKUP_FILE_GZ} | docker-compose -f docker-compose.prod.yml exec -T mysql mysql -u root -p mealcheck"

