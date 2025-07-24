#!/bin/bash

# Test actual Kafka readiness using external tools
IMAGE_TYPE="$1"
CONTAINER_NAME="$2"

# Method 1: Use JVM image tools from host to test native image
if command -v kafka-topics >/dev/null 2>&1; then
    # Use local kafka-topics if available
    kafka-topics --list --bootstrap-server localhost:9092 >/dev/null 2>&1
elif docker ps --filter "name=kafka-tools" --format "{{.Names}}" | grep -q kafka-tools; then
    # Use a tools container
    docker exec kafka-tools kafka-topics --list --bootstrap-server host.docker.internal:9092 >/dev/null 2>&1
else
    # Create a temporary JVM container to test readiness
    docker run --rm --network host \
        519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/prod/confluentinc/cp-server:8.0.x-latest-ubi9 \
        kafka-topics --list --bootstrap-server localhost:9092 >/dev/null 2>&1
fi
