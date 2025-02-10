FROM amazoncorretto:21

ARG JAR_FILE=build/libs/schedule-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} schedule-service.jar

ENTRYPOINT ["java","-jar","/schedule-service.jar"]