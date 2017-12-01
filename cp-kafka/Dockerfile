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

FROM ${DOCKER_UPSTREAM_REGISTRY}confluentinc/cp-base:${DOCKER_UPSTREAM_TAG}

EXPOSE 9092

ENV COMPONENT=kafka

MAINTAINER partner-support@confluent.io
LABEL io.confluent.docker=true

ARG GIT_COMMIT=unknown
LABEL io.confluent.docker.git.id=$GIT_COMMIT
ARG BUILD_NUMBER=-1
LABEL io.confluent.docker.build.number=$BUILD_NUMBER

ARG PACKAGING_BUILD_DOWNLOAD
ARG PROJECT_VERSION
ARG ARTIFACT_ID

ARG KAFKA_COMMIT=unknown
ENV KAFKA_COMMIT=${KAFKA_COMMIT}

RUN echo "===> Getting confluent tarball package - ${PACKAGING_BUILD_DOWNLOAD}" \
    && wget -q ${PACKAGING_BUILD_DOWNLOAD} -O - | tar --strip-components 1 -C /usr -xvzf - \
    && mv /usr/etc/kafka /etc/kafka && rmdir /usr/etc \
    && echo "===> Setting up ${COMPONENT} dirs..." \
    && mkdir -p /var/lib/${COMPONENT}/data /etc/${COMPONENT}/secrets\
    && chmod -R ag+w /etc/${COMPONENT} /var/lib/${COMPONENT}/data /etc/${COMPONENT}/secrets

VOLUME ["/var/lib/${COMPONENT}/data", "/etc/${COMPONENT}/secrets"]

COPY include/etc/confluent/docker /etc/confluent/docker

ADD target/${ARTIFACT_ID}-${PROJECT_VERSION}-package/share/java/${ARTIFACT_ID}/* /usr/share/java/${COMPONENT}/
ADD target/${ARTIFACT_ID}-${PROJECT_VERSION}-package/share/doc/* /usr/share/doc/${COMPONENT}/

CMD ["/etc/confluent/docker/run"]
