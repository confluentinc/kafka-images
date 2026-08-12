# Confluent Local Docker Image

Docker image to quickly start Apache Kafka® in KRaft mode with zero configuration setup.

Please refer to the officially supported [CP-Server](https://hub.docker.com/r/confluentinc/cp-server) image for Confluent Enterprise Kafka and see the [CP-Kafka](https://hub.docker.com/r/confluentinc/cp-kafka) image for Apache Kafka.

Confluent-Local image deploys Apache Kafka along with Confluent Community RestProxy. It is experimental, built for local development workflows and is not officially supported for production workloads. 

## Using the image
This Docker image starts with KRaft as the default mode. Check [here](https://docs.confluent.io/platform/current/installation/docker/config-reference.html#confluent-enterprise-ak-configuration) to modify the default configurations. 

### `IS_DYNAMIC_QUORUM`

Optional boolean (`true` or `false`, defaults to `false`) that controls how the image formats the storage directory at container start. When set to `true`, the image passes `--no-initial-controllers` to `kafka-storage format`, so the node joins the existing controller quorum instead of writing its own initial voter set.

**Only set `IS_DYNAMIC_QUORUM=true` after the cluster has completed migration to a dynamic controller quorum ([KIP-853](https://cwiki.apache.org/confluence/display/KAFKA/KIP-853%3A+KRaft+Controller+Membership+Changes)).** Until that migration is complete the quorum membership is still static (`controller.quorum.voters`), and formatting without an initial controller set produces a node that cannot join the quorum. Leave the variable unset or `false` on clusters that have not migrated.

## Resources

[What is Apache Kafka?](https://developer.confluent.io/learn-kafka)

[What Does Kafka Do?](https://developer.confluent.io/)


## Contribute

[How to contribute to the source code?](https://github.com/confluentinc/kafka-images)

[How to raise/track an issue?](https://github.com/confluentinc/kafka-images/issues)

## License

Usage of this image is subject to the license terms of the software contained within. Please refer to Confluent's Docker images documentation [reference](https://docs.confluent.io/platform/current/installation/docker/image-reference.html) for further information.
