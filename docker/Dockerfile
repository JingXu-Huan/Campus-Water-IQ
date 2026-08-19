# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS build

ARG MODULE
ARG ARTIFACT_NAME

WORKDIR /workspace
COPY . .

# common is a library and must stay a normal JAR. Repackage only the selected
# executable module in the final Maven invocation.
RUN --mount=type=cache,id=campus-water-m2,target=/root/.m2 \
    test -n "${MODULE}" \
    && test -n "${ARTIFACT_NAME}" \
    && mvn -B -ntp -DskipTests -pl common -am install \
    && mvn -B -ntp -DskipTests -pl "${MODULE}" package spring-boot:repackage \
    && APP_JAR="$(find "${MODULE}/target" -maxdepth 1 -type f -name "${ARTIFACT_NAME}-*.jar" ! -name "*.original" -print -quit)" \
    && test -n "${APP_JAR}" \
    && cp "${APP_JAR}" /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime

ARG MODULE

RUN useradd --system --uid 10001 --create-home appuser
WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"

LABEL org.opencontainers.image.title="${MODULE}"
USER appuser

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
