FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY common/ common/
RUN mvn -f common/pom.xml install -DskipTests -q
COPY rest-client/ rest-client/
RUN mvn -f rest-client/pom.xml package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/rest-client/target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
