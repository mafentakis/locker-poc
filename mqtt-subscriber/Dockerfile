FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
COPY common/ common/
COPY mqtt-subscriber/ mqtt-subscriber/
RUN mvn -pl mqtt-subscriber -am package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/mqtt-subscriber/target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
