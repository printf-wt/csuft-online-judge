FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends bash curl docker.io \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 csuftoj \
    && useradd --uid 10001 --gid 10001 --create-home --shell /usr/sbin/nologin csuftoj

WORKDIR /app
COPY --from=build /build/target/csuft-oj-0.0.1-SNAPSHOT.jar /app/app.jar
COPY scripts/csuft-oj-sandbox.sh /usr/local/bin/csuft-oj-sandbox

RUN chmod 0755 /usr/local/bin/csuft-oj-sandbox \
    && mkdir -p /var/lib/csuft-oj/testcases /var/lib/csuft-oj/judge-temp /var/log/csuft-oj \
    && chown -R csuftoj:csuftoj /var/lib/csuft-oj /var/log/csuft-oj /app

USER 10001:10001
EXPOSE 8080 8081

HEALTHCHECK --interval=20s --timeout=5s --start-period=40s --retries=3 \
    CMD curl --fail --silent http://127.0.0.1:8081/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:--XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError} -jar /app/app.jar"]
