FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY services/core-api/pom.xml services/core-api/pom.xml
COPY services/core-api/src services/core-api/src
RUN mvn -f services/core-api/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 rto \
    && mkdir -p /var/lib/rto/documents \
    && chown -R rto:rto /app /var/lib/rto
COPY --from=build /workspace/services/core-api/target/core-api-0.1.0-SNAPSHOT.jar /app/core-api.jar

USER rto
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/core-api.jar"]
