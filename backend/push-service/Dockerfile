FROM amazoncorretto:21

ARG JAR_FILE=build/libs/push-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} push-service.jar

ENTRYPOINT ["java","-jar","/push-service.jar"]