# Confluent Community Docker Image for Apache Kafka

Docker image for deploying and running the Community Version of Kafka packaged with the Confluent Community download. Please see the [cp-server](https://hub.docker.com/r/confluentinc/cp-server) image for additional commercial features that are only part of [Confluent Server](https://docs.confluent.io/platform/current/installation/available_packages.html#confluent-server).

## Using the image

* [Notes on using the image](https://docs.confluent.io/platform/current/installation/docker/installation.html) 
* [Configuration Reference](https://docs.confluent.io/platform/current/installation/docker/config-reference.html#confluent-ak-configuration)

### `IS_DYNAMIC_QUORUM`

Optional boolean (`true` or `false`, defaults to `false`) that controls how the image formats the storage directory at container start. When set to `true`, the image passes `--no-initial-controllers` to `kafka-storage format`, so the node joins the existing controller quorum instead of writing its own initial voter set.

**Only set `IS_DYNAMIC_QUORUM=true` after the cluster has completed migration to a dynamic controller quorum ([KIP-853](https://cwiki.apache.org/confluence/display/KAFKA/KIP-853%3A+KRaft+Controller+Membership+Changes)).** Until that migration is complete the quorum membership is still static (`controller.quorum.voters`), and formatting without an initial controller set produces a node that cannot join the quorum. Leave the variable unset or `false` on clusters that have not migrated.

## Resources

* [Docker Quick Start for Apache Kafka using Confluent Platform](https://docs.confluent.io/platform/current/quickstart/ce-docker-quickstart.html#ce-docker-quickstart)

* [Learn Kafka](https://developer.confluent.io/learn-kafka)

* [Confluent Developer](https://developer.confluent.io): blogs, tutorials, videos, and podcasts for learning all about Apache Kafka and Confluent Platform

* [confluentinc/cp-demo](https://github.com/confluentinc/cp-demo): GitHub demo that you can run locally. The demo uses this Docker image to showcase Confluent Server in a secured, end-to-end event streaming platform. It has an accompanying playbook that shows users how to use Confluent Control Center to manage and monitor Kafka connect, Schema Registry, REST Proxy, KSQL, and Kafka Streams.

* [confluentinc/examples](https://github.com/confluentinc/examples): additional curated examples in GitHub that you can run locally.

## Contribute

Start by reading our guidelines on contributing to this project found here.

* [Source Code](https://github.com/confluentinc/kafka-images)
* [Issue Tracker](https://github.com/confluentinc/kafka-images/issues)

## License

Usage of this image is subject to the license terms of the software contained within. Please refer to Confluent's Docker images documentation [reference](https://docs.confluent.io/platform/current/installation/docker/image-reference.html) for further information. The software to extend and build the custom Docker images is available under the Apache 2.0 License.
