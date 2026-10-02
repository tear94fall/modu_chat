FROM amazoncorretto:21

ARG JAR_FILE=point-api/build/libs/point-api-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} point-service.jar

# exec 형식: PID 1 이 java 라 SIGTERM 을 직접 받는다(server.shutdown=graceful).
# MaxRAMPercentage: 컨테이너 메모리 제한을 따라 힙을 잡는다.
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/point-service.jar"]
