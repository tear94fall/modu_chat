FROM amazoncorretto:21

ARG JAR_FILE=build/libs/member-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} member-service.jar

ENTRYPOINT ["java","-jar","/member-service.jar"]