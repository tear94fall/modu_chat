FROM amazoncorretto:21

ARG JAR_FILE=build/libs/gateway-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} gateway-service.jar

ENTRYPOINT ["java","-jar","/gateway-service.jar"]