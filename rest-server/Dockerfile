FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY common/ common/
RUN mvn -f common/pom.xml install -DskipTests -q
COPY rest-server/ rest-server/
RUN mvn -f rest-server/pom.xml package -DskipTests -q

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /src/rest-server/target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
