FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY common/ common/
RUN mvn -f common/pom.xml install -DskipTests -q
COPY integration-test/ integration-test/
RUN mvn -f integration-test/pom.xml package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/integration-test/target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

