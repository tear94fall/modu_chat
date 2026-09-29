FROM amazoncorretto:21

ARG JAR_FILE=push-api/build/libs/push-api-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} push-service.jar

ENTRYPOINT ["java","-jar","/push-service.jar"]