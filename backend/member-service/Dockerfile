FROM openjdk:17-alpine

ARG JAR_FILE=build/libs/member-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} member-service.jar

ENTRYPOINT ["java","-jar","/member-service.jar"]