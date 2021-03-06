FROM openjdk:15-alpine

RUN apk add --no-cache bash && mkdir /opt/mateo-camunda-bridge
COPY target/mateo-camunda-bridge-0.0.3-SNAPSHOT.jar /opt/mateo-camunda-bridge/
COPY application.yml /opt/mateo-camunda-bridge/

EXPOSE 8082

WORKDIR /opt/mateo-camunda-bridge
CMD  [ "java", "-Dspring.profiles.active=default", "-jar", "mateo-camunda-bridge-0.0.3-SNAPSHOT.jar" ]
