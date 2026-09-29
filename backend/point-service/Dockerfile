FROM amazoncorretto:21

ARG JAR_FILE=point-api/build/libs/point-api-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} point-service.jar

ENTRYPOINT ["java","-jar","/point-service.jar"]
