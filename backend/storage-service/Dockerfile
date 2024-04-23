FROM openjdk:17-alpine

ARG JAR_FILE=build/libs/storage-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} storage-service.jar

ENTRYPOINT ["java","-jar","/storage-service.jar"]