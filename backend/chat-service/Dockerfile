FROM amazoncorretto:21

ARG JAR_FILE=chat-api/build/libs/chat-api-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} chat-service.jar

ENTRYPOINT ["java","-jar","/chat-service.jar"]
