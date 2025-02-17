# 1. 베이스 이미지 선택 (OpenJDK 17 사용)
FROM openjdk:17-jdk-slim

# 2. 환경변수로 JAR 파일 이름 설정 (빌드 과정에서 전달 가능)
ARG JAR_FILE=build/libs/monitor-0.0.3-SNAPSHOT.jar

# 3. JAR 파일을 컨테이너 내부로 복사
COPY ${JAR_FILE} charge-station.jar

# 4. 애플리케이션이 사용할 포트를 노출
EXPOSE 8080

# 5. 컨테이너가 실행될 때 수행할 명령 설정
ENTRYPOINT ["java","-jar","/charge-station.jar"]