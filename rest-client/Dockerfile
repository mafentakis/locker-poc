FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
COPY common/ common/
COPY rest-client/ rest-client/
RUN mvn -pl rest-client -am package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/rest-client/target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
