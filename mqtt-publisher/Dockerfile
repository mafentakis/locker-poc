FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY common/ common/
RUN mvn -f common/pom.xml install -DskipTests -q
COPY mqtt-publisher/ mqtt-publisher/
RUN mvn -f mqtt-publisher/pom.xml package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/mqtt-publisher/target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
