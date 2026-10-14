# syntax=docker/dockerfile:1.7
# 1단계: 소스에서 jar 를 만든다. 어디서 빌드해도 같은 이미지(CI·로컬 공통). Gradle 캐시는 BuildKit 캐시 마운트로 재사용한다.
FROM amazoncorretto:21 AS build
WORKDIR /src
# gradlew 가 xargs 를 쓰는데 corretto(Amazon Linux 2023) 이미지에는 없다. 소스 COPY 앞이라 한 번만 깔리고 레이어가 캐시된다.
RUN dnf -y -q install findutils && dnf clean all
# 의존성 레이어: 빌드 스크립트만 먼저 복사해 소스가 바뀌어도 의존성 다운로드는 캐시된다
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY point-api/build.gradle.kts point-api/
COPY point-application/build.gradle.kts point-application/
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon :point-api:dependencies :point-application:dependencies -q >/dev/null 2>&1 || true
COPY . .
# 테스트는 워크플로에서 돈다(-x test). 실행 jar 는 point-api 모듈에서만 나온다(point-api/build/libs/point-api-<version>.jar)
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon -x test :point-api:bootJar \
 && cp point-api/build/libs/point-api-*-SNAPSHOT.jar /app.jar

# 2단계: 실행. exec 형식이라 PID 1 이 java(SIGTERM → graceful shutdown). MaxRAMPercentage 로 컨테이너 메모리 제한을 따른다.
FROM amazoncorretto:21
COPY --from=build /app.jar /point-service.jar
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/point-service.jar"]
