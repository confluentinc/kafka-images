#!/bin/bash

# Quick Kafka Benchmark - Simplified version for rapid testing

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
ITERATIONS=${1:-3}  # Default to 3 iterations, can be overridden
RESULTS_DIR="quick-benchmark-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Image configurations
ARCH=$(uname -m)
JVM_IMAGE="519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/prod/confluentinc/cp-server:8.0.x-latest-ubi9"

if [[ "$ARCH" == "x86_64" ]]; then
    NATIVE_IMAGE="519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/dev/confluentinc/cp-server-native:dev-8.0.x-7842c5ec-ubi9.amd64"
else
    NATIVE_IMAGE="519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/dev/confluentinc/cp-server-native:dev-8.0.x-7842c5ec-ubi9.arm64"
fi

echo -e "${BLUE}⚡ Quick Kafka Benchmark${NC}"
echo -e "${BLUE}========================${NC}"
echo "Iterations: $ITERATIONS"
echo "Architecture: $ARCH"
echo ""

# Create results directory
mkdir -p "$RESULTS_DIR"

# Function to run single benchmark
run_benchmark() {
    local image_type="$1"
    local image="$2"
    
    echo -e "${YELLOW}Testing $image_type...${NC}"
    
    # Create simple compose file - based on docker-compose-basic-cp-server-native.yml
    cat > "docker-compose-quick-${image_type}.yml" << EOF
---
version: '2'
services:
  broker:
    image: $image
    hostname: broker
    container_name: broker-quick-${image_type}
    ports:
      - "9092:9092"
      - "9101:9101"
      - "8090:8090"
    environment:
      COMPONENT: 'kafka'
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: 'CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT'
      KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://broker:29092,PLAINTEXT_HOST://localhost:9092'
      KAFKA_PROCESS_ROLES: 'broker,controller'
      KAFKA_CONTROLLER_QUORUM_VOTERS: '1@broker:29093'
      KAFKA_LISTENERS: 'PLAINTEXT://broker:29092,CONTROLLER://broker:29093,PLAINTEXT_HOST://0.0.0.0:9092'
      KAFKA_INTER_BROKER_LISTENER_NAME: 'PLAINTEXT'
      KAFKA_CONTROLLER_LISTENER_NAMES: 'CONTROLLER'
      CLUSTER_ID: '4L6g3nShT-eMCtK--X86sw'
      KAFKA_CONFLUENT_METADATA_SERVER_LISTENERS: 'http://localhost:28090'
      CONFLUENT_METRICS_REPORTER_BOOTSTRAP_SERVERS: 'localhost:29092'
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_CONFLUENT_LICENSE_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_CONFLUENT_BALANCER_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      CONFLUENT_METRICS_REPORTER_TOPIC_REPLICAS: 1
      KAFKA_CONFLUENT_COMMAND_TOPIC_REPLICATION: 1
      KAFKA_CONFLUENT_LINK_METADATA_TOPIC_REPLICATION: 1
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_CONFLUENT_BALANCER_TOPIC_REPLICATION: 1
      KAFKA_CLUSTER_LINK_METADATA_TOPIC_REPLICATION: 1
      KAFKA_CONFLUENT_CLUSTER_LINK_METADATA_TOPIC_REPLICATION: 1
      KAFKA_CONFLUENT_DURABILITY_TOPIC_REPLICATION: 1
      KAFKA_CONFLUENT_TIER_METADATA_REPLICATION: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION: 1
      KAFKA_CONFLUENT_LICENSE_TOPIC_REPLICATION: 1
      KAFKA_CONFLUENT_COMMAND_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_CONFLUENT_LINK_METADATA_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_CLUSTER_LINK_METADATA_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_CONFLUENT_CLUSTER_LINK_METADATA_TOPIC_REPLICATION_FACTOR: 1
EOF
    
    local total_startup=0
    local total_memory=0
    local successful_runs=0
    
    for i in $(seq 1 $ITERATIONS); do
        echo "  Run $i/$ITERATIONS..."
        
        # Cleanup
        docker-compose -f "docker-compose-quick-${image_type}.yml" down -v &>/dev/null || true
        sleep 2
        
        # Start and time
        start_time=$(date +%s.%N)
        docker-compose -f "docker-compose-quick-${image_type}.yml" up -d &>/dev/null
        
        # Wait for readiness - check for Kafka Server started message (most authoritative)
        for attempt in {1..60}; do
            # Primary check: Look for "Kafka Server started" message (most definitive)
            if docker logs "broker-quick-${image_type}" 2>&1 | grep -q -i "kafka.*server.*started"; then
                kafka_ready=true
            # Fallback: Check for HTTP server readiness messages
            elif docker logs "broker-quick-${image_type}" 2>&1 | grep -q "KafkaHttpServer transitioned.*to RUNNING" && \
                 docker logs "broker-quick-${image_type}" 2>&1 | grep -q "Started NetworkTrafficServerConnector"; then
                kafka_ready=true
            else
                kafka_ready=false
            fi
            
            if [[ "$kafka_ready" == "true" ]]; then
                end_time=$(date +%s.%N)
                startup_time=$(echo "$end_time - $start_time" | bc -l)
                
                # Get memory usage with proper unit conversion
                memory_stats=$(docker stats --no-stream --format "{{.MemUsage}}" "broker-quick-${image_type}")
                memory_with_unit=$(echo "$memory_stats" | cut -d'/' -f1 | xargs)
                
                # Convert to MB based on unit
                if [[ "$memory_with_unit" == *"GiB"* ]]; then
                    memory_value=$(echo "$memory_with_unit" | sed 's/[^0-9.]//g')
                    memory_mb=$(echo "$memory_value * 1024" | bc -l)
                elif [[ "$memory_with_unit" == *"MiB"* ]]; then
                    memory_mb=$(echo "$memory_with_unit" | sed 's/[^0-9.]//g')
                elif [[ "$memory_with_unit" == *"KiB"* ]]; then
                    memory_value=$(echo "$memory_with_unit" | sed 's/[^0-9.]//g')
                    memory_mb=$(echo "scale=2; $memory_value / 1024" | bc -l)
                else
                    # Default to treating as MB if no unit found
                    memory_mb=$(echo "$memory_with_unit" | sed 's/[^0-9.]//g')
                fi
                
                total_startup=$(echo "$total_startup + $startup_time" | bc -l)
                total_memory=$(echo "$total_memory + $memory_mb" | bc -l)
                successful_runs=$((successful_runs + 1))
                
                echo "    Startup: ${startup_time}s, Memory: ${memory_mb}MB"
                break
            fi
            sleep 1
        done
        
        # Cleanup
        docker-compose -f "docker-compose-quick-${image_type}.yml" down -v &>/dev/null || true
        sleep 1
    done
    
    if [[ $successful_runs -gt 0 ]]; then
        local avg_startup=$(echo "scale=2; $total_startup / $successful_runs" | bc -l)
        local avg_memory=$(echo "scale=1; $total_memory / $successful_runs" | bc -l)
        
        echo "$avg_startup" > "$RESULTS_DIR/${image_type}_startup_avg.txt"
        echo "$avg_memory" > "$RESULTS_DIR/${image_type}_memory_avg.txt"
        
        echo -e "${GREEN}  Average - Startup: ${avg_startup}s, Memory: ${avg_memory}MB${NC}"
    else
        echo "  No successful runs!"
    fi
    
    # Cleanup temp file
    rm -f "docker-compose-quick-${image_type}.yml"
}

# Run benchmarks
run_benchmark "jvm" "$JVM_IMAGE"
echo ""
run_benchmark "native" "$NATIVE_IMAGE"

# Show comparison
echo ""
echo -e "${BLUE}📊 Results Summary${NC}"
echo -e "${BLUE}==================${NC}"

if [[ -f "$RESULTS_DIR/jvm_startup_avg.txt" && -f "$RESULTS_DIR/native_startup_avg.txt" ]]; then
    jvm_startup=$(cat "$RESULTS_DIR/jvm_startup_avg.txt")
    native_startup=$(cat "$RESULTS_DIR/native_startup_avg.txt")
    jvm_memory=$(cat "$RESULTS_DIR/jvm_memory_avg.txt")
    native_memory=$(cat "$RESULTS_DIR/native_memory_avg.txt")
    
    startup_improvement=$(echo "scale=1; ($jvm_startup - $native_startup) / $jvm_startup * 100" | bc -l)
    memory_improvement=$(echo "scale=1; ($jvm_memory - $native_memory) / $jvm_memory * 100" | bc -l)
    
    echo "JVM Startup:     ${jvm_startup}s"
    echo "Native Startup:  ${native_startup}s"
    echo "Startup Improvement: ${startup_improvement}%"
    echo ""
    echo "JVM Memory:      ${jvm_memory}MB"
    echo "Native Memory:   ${native_memory}MB"
    echo "Memory Improvement: ${memory_improvement}%"
fi

echo ""
echo -e "${GREEN}✅ Quick benchmark completed!${NC}" 
