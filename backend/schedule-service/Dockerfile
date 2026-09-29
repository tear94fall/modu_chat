FROM amazoncorretto:21

ARG JAR_FILE=schedule-api/build/libs/schedule-api-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} schedule-service.jar

ENTRYPOINT ["java","-jar","/schedule-service.jar"]
