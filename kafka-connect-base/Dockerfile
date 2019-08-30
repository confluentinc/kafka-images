ARG DOCKER_UPSTREAM_REGISTRY
ARG DOCKER_UPSTREAM_TAG=latest
ARG DOCKER_TAG

FROM ${DOCKER_UPSTREAM_REGISTRY}confluentinc/kafka-image:${DOCKER_TAG}

ARG PROJECT_VERSION
ARG ARTIFACT_ID

# Make sure you use an appropriate contact address.
MAINTAINER partner-support@confluent.io
LABEL io.confluent.docker=true
ARG COMMIT_ID=unknown
LABEL io.confluent.docker.git.id=$COMMIT_ID
ARG BUILD_NUMBER=-1
LABEL io.confluent.docker.build.number=$BUILD_NUMBER

ARG CONFLUENT_VERSION
ARG CONFLUENT_PACKAGES_REPO
ARG CONFLUENT_PLATFORM_LABEL
ARG CONFLUENT_DEB_VERSION
ARG ALLOW_UNSIGNED

ENV COMPONENT=kafka-connect

# Default kafka-connect rest.port
EXPOSE 8083


RUN echo "===> Installing ${COMPONENT}..." \
    && apt-get update \
    && echo "===> Adding confluent repository...${CONFLUENT_PACKAGES_REPO}" \
    && if [ "x$ALLOW_UNSIGNED" = "xtrue" ]; then echo "APT::Get::AllowUnauthenticated \"true\";" > /etc/apt/apt.conf.d/allow_unauthenticated; else curl -s -L ${CONFLUENT_PACKAGES_REPO}/archive.key -o /tmp/archive.key && apt-key add /tmp/archive.key; fi \
    && echo "deb [arch=amd64] ${CONFLUENT_PACKAGES_REPO} stable main" >> /etc/apt/sources.list \
    && cat /etc/apt/sources.list \
    && apt-get install -y apt-transport-https \
    && apt-get -qq update \
    && echo "===> Installing Schema Registry (for Avro jars) ..." \
    && apt-get install -y confluent-schema-registry=${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL}-${CONFLUENT_DEB_VERSION} \
    && echo "===> Installing Controlcenter for monitoring interceptors ..." \
    && apt-get install -y confluent-control-center=${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL}-${CONFLUENT_DEB_VERSION} \
    && echo "===> Installing Confluent Hub client ..." \
    && apt-get install -y confluent-hub-client=${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL}-${CONFLUENT_DEB_VERSION} \
    && echo "===> Cleaning up ..." \
    && apt-get clean && rm -rf /tmp/* /var/lib/apt/lists/* \
    echo "===> Setting up ${COMPONENT} dirs ..." \
    && mkdir -p /etc/${COMPONENT} /etc/${COMPONENT}/secrets /etc/${COMPONENT}/jars \
    && chmod -R ag+w /etc/${COMPONENT} /etc/${COMPONENT}/secrets /etc/${COMPONENT}/jars

VOLUME ["/etc/${COMPONENT}/jars", "/etc/${COMPONENT}/secrets"]

COPY include/etc/confluent/docker /etc/confluent/docker

CMD ["/etc/confluent/docker/run"]

# Polling period  : 5 seconds
# Timeout period  :10 seconds (if the polling does not return within this time, treat as a failed poll)
# Start-up period : 2 minutes (during which failures are not counted as failures)
# Retry period    : 8 minutes (after which container is deemed unhealthy)
# All settings can be overriden at run-time in Docker/Docker Compose. 
HEALTHCHECK --start-period=120s --interval=5s --timeout=10s --retries=96 \
    CMD /etc/confluent/docker/healthcheck.sh
