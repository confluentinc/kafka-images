ARG DOCKER_UPSTREAM_REGISTRY
ARG DOCKER_UPSTREAM_TAG

FROM ${DOCKER_UPSTREAM_REGISTRY}confluentinc/cp-base:${DOCKER_UPSTREAM_TAG}

ARG GIT_COMMIT=unknown
LABEL io.confluent.docker.git.id=$GIT_COMMIT
ARG BUILD_NUMBER=-1
LABEL io.confluent.docker.build.number=$BUILD_NUMBER
LABEL io.confluent.docker=true

WORKDIR /opt

RUN wget http://downloads.sourceforge.net/project/cyclops-group/jmxterm/1.0-alpha-4/jmxterm-1.0-alpha-4-uber.jar -O /opt/jmxterm-1.0-alpha-4-uber.jar
