FROM openjdk:14-jdk-alpine

ARG JAR_FILE=build/libs/auth-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} auth-service.jar

ENTRYPOINT ["java","-jar","/auth-service.jar"]