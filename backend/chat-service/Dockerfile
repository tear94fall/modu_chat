FROM openjdk:17-jdk-alpine

ARG JAR_FILE=build/libs/chat-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} chat-service.jar

ENTRYPOINT ["java","-jar","/chat-service.jar"]