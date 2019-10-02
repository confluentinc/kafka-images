# Docker images for Kafka

This repo provides build files for [Kafka](https://www.confluent.io/what-is-apache-kafka/) Docker images.

## Properties

Properties are inherited from a top-level POM. Properties may be overridden on the command line (`-Ddocker.registry=testing.example.com:8080/`), or in a subproject's POM.

- *docker.skip-build*: Set to `false` to include Docker images as part of build.
- *docker.skip-test*: Set to `false` to include Docker image integration tests as part of the build. Requires Python 2.7, `tox`.
- *docker.registry*: (Optional) Specify a registry other than `docker.io`. Used as `DOCKER_REGISTRY` during `docker build` and testing. Trailing `/` is required. Defaults to `docker.io/`.
- *docker.tag*: (Optional) Tag postfix for built images. Used as `DOCKER_TAG` during `docker build` and testing. Defaults to the value of `project.version`.
- *docker.upstream-registry*: (Optional) Registry to pull base images from. Trailing `/` is required. Used as `DOCKER_UPSTREAM_REGISTRY` during `docker build`. Defaults to the value of `docker.registry`.
- *docker.upstream-tag*: (Optional) Use the given tag postfix when pulling base images. Used as `DOCKER_UPSTREAM_TAG` during `docker build`. Defaults to the value of `docker.tag`.
- *docker.test-registry*: (Optional) Registry to pull test dependency images from. Trailing `/` is required. Used as `DOCKER_TEST_REGISTRY` during testing. Defaults to the value of `docker.upstream-registry`.
- *docker.test-tag*: (Optional) Use the given tag postfix when pulling test dependency images. Used as `DOCKER_TEST_TAG` during testing. Defaults to the value of `docker.upstream-tag`.


## Building

This project uses `maven-assembly-plugin` and `dockerfile-maven-plugin` to build Docker images via Maven.

To build SNAPSHOT images, configure `.m2/settings.xml` for SNAPSHOT dependencies. These must be available at build time.

```
mvn clean package -Pdocker  # Build local images
```