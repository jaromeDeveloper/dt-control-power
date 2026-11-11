FROM eclipse-temurin:21.0.13_9-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21.0.13_9-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && mkdir -p /app /var/lib/dt-control-power/uploads \
    && chown -R app:app /app /var/lib/dt-control-power

WORKDIR /app

COPY --from=build --chown=app:app /workspace/target/backend-*.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx768m -XX:MaxMetaspaceSize=256m"

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
