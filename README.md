# Docker images for Apache Kafka

This repo provides build files for [Apache Kafka](https://www.confluent.io/what-is-apache-kafka/) and Confluent Docker images. The images can be found on [Docker Hub](https://hub.docker.com/u/confluentinc/), and sample Docker Compose files [here](examples).

## Docker Image reference

Information on using the Docker images is available in [the documentation](https://docs.confluent.io/platform/current/installation/docker/installation.html). 

## Build files
### Properties

Properties are inherited from a top-level POM. Properties may be overridden on the command line (`-Ddocker.registry=testing.example.com:8080/`), or in a subproject's POM.

- *docker.skip-build*: (Optional) Set to `false` to include Docker images as part of build. Default is 'false'.
- *docker.skip-test*: (Optional) Set to `false` to include Docker image integration tests as part of the build. Requires Python 2.7, `tox`. Default is 'true'.
- *docker.registry*: (Optional) Specify a registry other than `placeholder/`. Used as `DOCKER_REGISTRY` during `docker build` and testing. Trailing `/` is required. Defaults to `placeholder/`.
- *docker.tag*: (Optional) Tag for built images. Used as `DOCKER_TAG` during `docker build` and testing. Defaults to the value of `project.version`.
- *docker.upstream-registry*: (Optional) Registry to pull base images from. Trailing `/` is required. Used as `DOCKER_UPSTREAM_REGISTRY` during `docker build`. Defaults to the value of `docker.registry`.
- *docker.upstream-tag*: (Optional) Use the given tag when pulling base images. Used as `DOCKER_UPSTREAM_TAG` during `docker build`. Defaults to the value of `docker.tag`.
- *docker.test-registry*: (Optional) Registry to pull test dependency images from. Trailing `/` is required. Used as `DOCKER_TEST_REGISTRY` during testing. Defaults to the value of `docker.upstream-registry`.
- *docker.test-tag*: (Optional) Use the given tag when pulling test dependency images. Used as `DOCKER_TEST_TAG` during testing. Defaults to the value of `docker.upstream-tag`.
- *docker.os_type*: (Optional) Specify which operating system to use as the base image by using the Dockerfile with this extension. Valid values are `ubi8`. Default value is `ubi8`.
- *CONFLUENT_PACKAGES_REPO*: (Required) Specify the location of the Confluent Platform packages repository. Depending on the type of OS for the image you are building you will need to either provide a Debian or RPM repository. For example this is the repository for the 5.4.0 release of the Debian packages: `https://s3-us-west-2.amazonaws.com/confluent-packages-5.4.0/deb/5.4` This is the repository for the 5.4.0 release of the RPM's: `https://s3-us-west-2.amazonaws.com/confluent-packages-5.4.0/rpm/5.4`
- *CONFLUENT_VERSION*: (Required) Specify the full Confluent Platform release version. Example: 5.4.0


### Building

This project uses `maven-assembly-plugin` and `dockerfile-maven-plugin` to build Docker images via Maven.

To build SNAPSHOT images, configure `.m2/settings.xml` for SNAPSHOT dependencies. These must be available at build time.

```
mvn clean package -Pdocker -DskipTests # Build local images
```