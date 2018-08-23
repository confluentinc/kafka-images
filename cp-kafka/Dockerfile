#
# Copyright 2017 Confluent Inc.
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
#

ARG DOCKER_UPSTREAM_REGISTRY
ARG DOCKER_UPSTREAM_TAG

FROM ${DOCKER_UPSTREAM_REGISTRY}confluentinc/cp-base-new:${DOCKER_UPSTREAM_TAG}

EXPOSE 9092

ENV COMPONENT=kafka

MAINTAINER partner-support@confluent.io
LABEL io.confluent.docker=true

ARG GIT_COMMIT=unknown
LABEL io.confluent.docker.git.id=$GIT_COMMIT
ARG BUILD_NUMBER=-1
LABEL io.confluent.docker.build.number=$BUILD_NUMBER

ARG CONFLUENT_PACKAGES_REPO
ARG PROJECT_VERSION
ARG ARTIFACT_ID

RUN SHORT_VERSION=$(echo "${PROJECT_VERSION}" | sed 's/-SNAPSHOT//' | sed 's/\.[[:digit:]]*$//' ) \
    && apt-get update \
    && apt-get install -y apt-transport-https \
    && echo "APT::Get::AllowUnauthenticated \"true\";" > /etc/apt/apt.conf.d/allow_unauthenticated \
    && echo "deb [arch=amd64] ${CONFLUENT_PACKAGES_REPO}/deb/${SHORT_VERSION} stable main" >> /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y confluent-kafka-2.11 \
    && mkdir -p /var/lib/${COMPONENT}/data /etc/${COMPONENT}/secrets \
    && chmod -R ag+w /etc/${COMPONENT} /var/lib/${COMPONENT}/data /etc/${COMPONENT}/secrets \
    && apt-get clean && rm -rf /tmp/* /var/lib/apt/lists/* \

VOLUME ["/var/lib/${COMPONENT}/data", "/etc/${COMPONENT}/secrets"]

COPY include/etc/confluent/docker /etc/confluent/docker

CMD ["/etc/confluent/docker/run"]
