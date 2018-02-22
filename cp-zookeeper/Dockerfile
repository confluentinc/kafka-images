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

ARG DOCKER_REGISTRY
ARG DOCKER_TAG

FROM ${DOCKER_REGISTRY}confluentinc/cp-kafka:${DOCKER_TAG}

EXPOSE 2181 2888 3888

ENV COMPONENT=zookeeper

MAINTAINER partner-support@confluent.io
LABEL io.confluent.docker=true

ARG GIT_COMMIT=unknown
LABEL io.confluent.docker.git.id=$GIT_COMMIT
ARG BUILD_NUMBER=-1
LABEL io.confluent.docker.build.number=$BUILD_NUMBER

RUN mkdir -p /var/lib/${COMPONENT}/data /var/lib/${COMPONENT}/log /etc/${COMPONENT}/secrets \
    && chmod -R ag+w /etc/kafka /etc/${COMPONENT} /var/lib/${COMPONENT}/data /var/lib/${COMPONENT}/log /etc/${COMPONENT}/secrets

VOLUME ["/var/lib/${COMPONENT}/data", "/var/lib/${COMPONENT}/log", "/etc/${COMPONENT}/secrets"]

COPY include/etc/confluent/docker /etc/confluent/docker

CMD ["/etc/confluent/docker/run"]
