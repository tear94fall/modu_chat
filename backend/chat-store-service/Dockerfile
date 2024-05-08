FROM openjdk:17-alpine

ARG JAR_FILE=build/libs/chat-store-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} chat-store-service.jar

ENTRYPOINT ["java","-jar","/chat-store-service.jar"]