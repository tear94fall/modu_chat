FROM openjdk:17-alpine

ARG JAR_FILE=build/libs/ws-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} ws-service.jar

ENTRYPOINT ["java","-jar","/ws-service.jar"]