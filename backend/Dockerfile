# Spring Boot runtime-only 이미지 (로컬에서 빌드한 JAR만 실행)
FROM eclipse-temurin:17-jre
WORKDIR /app

# 보안을 위해 non-root 사용자 생성
RUN groupadd -g 1001 appgroup && useradd -u 1001 -g appgroup -s /bin/bash appuser

# 로컬에서 빌드된 JAR 복사 (deploy 스크립트가 먼저 mvn package 수행)
COPY target/*.jar app.jar

# 권한 설정
RUN chown -R appuser:appgroup /app
USER appuser

# 포트 및 헬스체크
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

