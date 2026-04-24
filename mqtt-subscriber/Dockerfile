FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY docker/proxy-certs/ /tmp/proxy-certs/
RUN for cert in /tmp/proxy-certs/*.crt; do \
      alias=$(basename "$cert" .crt); \
      keytool -importcert -noprompt -trustcacerts \
        -alias "$alias" -file "$cert" \
        -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit; \
    done
COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY common/ common/
RUN mvn -f common/pom.xml install -DskipTests -q
COPY mqtt-subscriber/ mqtt-subscriber/
RUN mvn -f mqtt-subscriber/pom.xml package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/mqtt-subscriber/target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
