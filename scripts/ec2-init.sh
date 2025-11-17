#!/bin/bash

# EC2 인스턴스 초기 설정 스크립트
# User Data로 사용하거나 수동 실행 가능

set -e

echo "EC2 초기 설정 시작..."

# 시스템 업데이트
yum update -y

# Docker 설치
yum install -y docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# Docker Compose 설치
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Git 설치 (선택사항)
yum install -y git

echo "EC2 초기 설정 완료!"
echo "재로그인 후 docker 명령어를 사용할 수 있습니다."

