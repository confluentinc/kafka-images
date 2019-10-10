#
# Copyright 2019 Confluent Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

ARG DOCKER_UPSTREAM_REGISTRY
ARG DOCKER_UPSTREAM_TAG=latest
ARG DOCKER_TAG

FROM ${DOCKER_UPSTREAM_REGISTRY}confluentinc/cp-kafka:${DOCKER_TAG}

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


RUN echo "===> Installing JDBC, Elasticsearch and Hadoop connectors ..." \
    && yum -q -y update \
    && yum install -y \
        confluent-kafka-connect-jjj-${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL} \
        confluent-kafka-connect-jdbc-${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL} \
        confluent-kafka-connect-hdfs-${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL} \
        confluent-kafka-connect-elasticsearch-${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL} \
        confluent-kafka-connect-storage-common-${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL} \
        confluent-kafka-connect-s3-${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL} \
        confluent-kafka-connect-jms-${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL} \
    && echo "===> Cleaning up ..."  \
    && yum clean all \
    && rm -rf /tmp/*

ENV CONNECT_PLUGIN_PATH=/usr/share/java/,/usr/share/confluent-hub-components/

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
