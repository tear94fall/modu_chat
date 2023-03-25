FROM openjdk:14-jdk-alpine

ARG JAR_FILE=build/libs/eureka-server-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} eureka-server.jar

ENTRYPOINT ["java","-jar","/eureka-server.jar"]