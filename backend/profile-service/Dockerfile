FROM openjdk:17-alpine

ARG JAR_FILE=build/libs/profile-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} profile-service.jar

ENTRYPOINT ["java","-jar","/profile-service.jar"]