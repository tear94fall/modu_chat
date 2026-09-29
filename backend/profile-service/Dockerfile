FROM amazoncorretto:21

ARG JAR_FILE=profile-api/build/libs/profile-api-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} profile-service.jar

ENTRYPOINT ["java","-jar","/profile-service.jar"]
