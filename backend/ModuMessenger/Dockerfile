FROM openjdk:14-jdk-alpine
ARG JAR_FILE=build/libs/ModuMessenger-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} mod-chat.jar
ENTRYPOINT ["java","-jar","/mod-chat.jar"]