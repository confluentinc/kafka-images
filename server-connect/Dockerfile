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

FROM ${DOCKER_UPSTREAM_REGISTRY}confluentinc/cp-server-connect-base:${DOCKER_TAG}

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

RUN echo "===> Installing ${COMPONENT}..." \
    && apt-get -qq update \
    && echo "===> Adding confluent repository...${CONFLUENT_PACKAGES_REPO}" \
    && if [ "x$ALLOW_UNSIGNED" = "xtrue" ]; then echo "APT::Get::AllowUnauthenticated \"true\";" > /etc/apt/apt.conf.d/allow_unauthenticated; else curl -s -L ${CONFLUENT_PACKAGES_REPO}/archive.key -o /tmp/archive.key && apt-key add /tmp/archive.key; fi \
    && echo "deb [arch=amd64] ${CONFLUENT_PACKAGES_REPO} stable main" >> /etc/apt/sources.list \
    && cat /etc/apt/sources.list \
    && apt-get -qq update \
    && apt-get install -y apt-transport-https \
    && apt-get install -y procps \
    && echo "===> Installing JDBC, Elasticsearch and Hadoop connectors ..." \
    && apt-get install -y confluent-kafka-connect-jdbc=${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL}-${CONFLUENT_DEB_VERSION} \
    confluent-kafka-connect-elasticsearch=${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL}-${CONFLUENT_DEB_VERSION} \
    confluent-kafka-connect-storage-common=${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL}-${CONFLUENT_DEB_VERSION} \
    confluent-kafka-connect-s3=${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL}-${CONFLUENT_DEB_VERSION} \
    confluent-kafka-connect-jms=${CONFLUENT_VERSION}${CONFLUENT_PLATFORM_LABEL}-${CONFLUENT_DEB_VERSION} \
    && echo "===> Cleaning up ..." \
    && apt-get clean && rm -rf /tmp/* /var/lib/apt/lists/*
