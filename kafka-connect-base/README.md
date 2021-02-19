# Confluent Docker Image for Kafka Connect

Docker image for deploying and running Kafka Connect. 

_Functionally, the `cp-kafka-connect` and the `cp-kafka-connect-base` images are identical. Prior to Confluent Platform 6.0 the `cp-kafka-connect` image included several connectors pre-installed, but **this is no longer the case**._

## What is Kafka Connect?

Kafka Connect is part of Apache Kafka, and is used to integrate external systems with Kafka. You can find out more in [the documentation](https://docs.confluent.io/platform/current/connect/index.html#what-is-kafka-connect) and [this short video](https://rmoff.dev/what-is-kafka-connect).

## Using the image

Please see the [Confluent Platform documentation](https://docs.confluent.io/platform/current/installation/docker/installation.html) for further documentation on these images.

### Installing connectors

Kafka Connect is a pluggable framework with which you can use plugins for different connectors, transformations, and converters. You can find hundreds of these at [Confluent Hub](https://hub.confluent.io).

You will need to install plugins into the image in order to use them. This can be done in several ways: 

1. [Extend the image](https://docs.confluent.io/platform/current/installation/docker/development.html#extending-images)
2. Add the connector JARs via volumes mounted to the image ([example](https://github.com/confluentinc/demo-scene/blob/master/kafka-connect-zero-to-hero/docker-compose.yml#L82-L87))
3. Install the connector JARs at runtime ([example](https://github.com/confluentinc/demo-scene/blob/master/kafka-connect-zero-to-hero/docker-compose.yml#L89-L101))

### Configuration

[Reference](https://docs.confluent.io/platform/current/installation/docker/config-reference.html#kconnect-long-configuration)

## Contribute

Start by reading our guidelines on contributing to this project found here.

* Source Code: https://github.com/confluentinc/kafka-images
* Issue Tracker: https://github.com/confluentinc/kafka-images/issues

## License

This Docker image is licensed under the Apache 2 license. For more information on the licenses for each of the individual Confluent Platform components packaged in this image, please refer to the respective [Confluent Platform documentation](https://docs.confluent.io/platform/current/installation/docker/image-reference.html).