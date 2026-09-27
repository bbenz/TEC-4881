FROM icr.io/appcafe/open-liberty:kernel-slim-java21-openj9-ubi-minimal

ARG VERSION=3.1-SNAPSHOT

COPY --chown=1001:0 src/main/liberty/config/ /config/
COPY --chown=1001:0 target/cargo-tracker.war /config/apps/cargo-tracker.war
COPY --chown=1001:0 src/main/ai-docs/ /config/ai-docs/

ENV AI_DOCS_DIR=/config/ai-docs

RUN features.sh
