FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -Popenliberty -DskipTests package

FROM icr.io/appcafe/open-liberty:kernel-slim-java21-openj9-ubi-minimal
COPY --chown=1001:0 --from=build /workspace/src/main/liberty/config/ /config/
COPY --chown=1001:0 --from=build /workspace/target/cargo-tracker.war /config/apps/cargo-tracker.war
COPY --chown=1001:0 --from=build /workspace/src/main/ai-docs/ /config/ai-docs/
COPY --chown=1001:0 --from=build /workspace/target/liberty/wlp/usr/shared/resources/hsqldb-*.jar /opt/ol/wlp/usr/shared/resources/

ENV AI_DOCS_DIR=/config/ai-docs

RUN features.sh
