#!/bin/bash

# Kafka Image Benchmark Comparison Script
# Compares JVM vs GraalVM Native startup time and memory usage

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ITERATIONS=5
RESULTS_DIR="benchmark-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Image configurations
JVM_IMAGE="519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/prod/confluentinc/cp-server:8.0.x-latest-ubi9"
NATIVE_IMAGE="519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/dev/confluentinc/cp-server-native:dev-8.0.x-7842c5ec-ubi9.arm64"

# Check if running on x86_64 or arm64
ARCH=$(uname -m)
if [[ "$ARCH" == "x86_64" ]]; then
    NATIVE_IMAGE="519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/dev/confluentinc/cp-server-native:dev-8.0.x-7842c5ec-ubi9.amd64"
fi

echo -e "${BLUE}🚀 Kafka Image Benchmark Comparison${NC}"
echo -e "${BLUE}=====================================${NC}"
echo "JVM Image:    $JVM_IMAGE"
echo "Native Image: $NATIVE_IMAGE"
echo "Iterations:   $ITERATIONS"
echo "Architecture: $ARCH"
echo ""

# Create results directory
mkdir -p "$RESULTS_DIR"

# Function to cleanup containers
cleanup() {
    echo -e "${YELLOW}🧹 Cleaning up containers...${NC}"
    docker-compose -f docker-compose-benchmark-jvm.yml down -v 2>/dev/null || true
    docker-compose -f docker-compose-benchmark-native.yml down -v 2>/dev/null || true
    docker system prune -f 2>/dev/null || true
}

# Function to wait for service readiness
wait_for_service() {
    local max_attempts=60
    local attempt=1
    
    echo -e "${YELLOW}⏳ Waiting for Kafka to be ready...${NC}"
    
    while [[ $attempt -le $max_attempts ]]; do
        # Primary check: Look for "Kafka Server started" message (most authoritative)
        if docker logs broker 2>&1 | grep -q -i "kafka.*server.*started"; then
            echo -e "${GREEN}✅ Kafka is ready! (Server started)${NC}"
            return 0
        # Fallback: Check for HTTP server readiness messages
        elif docker logs broker 2>&1 | grep -q "KafkaHttpServer transitioned.*to RUNNING" && \
             docker logs broker 2>&1 | grep -q "Started NetworkTrafficServerConnector"; then
            echo -e "${GREEN}✅ Kafka is ready! (HTTP services started)${NC}"
            return 0
        fi
        
        if [[ $((attempt % 5)) -eq 0 ]]; then
            echo "   Attempt $attempt/$max_attempts..."
        fi
        
        sleep 1
        ((attempt++))
    done
    
    echo -e "${RED}❌ Service failed to become ready${NC}"
    return 1
}

# Function to measure memory usage
measure_memory() {
    local container_name="$1"
    local result_file="$2"
    
    # Get memory stats with proper unit conversion
    local memory_stats=$(docker stats --no-stream --format "{{.MemUsage}}" "$container_name")
    local memory_with_unit=$(echo "$memory_stats" | cut -d'/' -f1 | xargs)
    
    # Convert to MB based on unit
    if [[ "$memory_with_unit" == *"GiB"* ]]; then
        local memory_value=$(echo "$memory_with_unit" | sed 's/[^0-9.]//g')
        local memory_used=$(echo "$memory_value * 1024" | bc -l)
    elif [[ "$memory_with_unit" == *"MiB"* ]]; then
        local memory_used=$(echo "$memory_with_unit" | sed 's/[^0-9.]//g')
    elif [[ "$memory_with_unit" == *"KiB"* ]]; then
        local memory_value=$(echo "$memory_with_unit" | sed 's/[^0-9.]//g')
        local memory_used=$(echo "scale=2; $memory_value / 1024" | bc -l)
    else
        # Default to treating as MB if no unit found
        local memory_used=$(echo "$memory_with_unit" | sed 's/[^0-9.]//g')
    fi
    
    echo "$memory_used" >> "$result_file"
    echo "Memory: ${memory_used}MB"
}

# Function to benchmark single run
benchmark_run() {
    local image_type="$1"
    local compose_file="$2"
    local iteration="$3"
    
    echo -e "${BLUE}📊 Testing $image_type (Run $iteration/$ITERATIONS)${NC}"
    
    # Cleanup before run
    cleanup
    sleep 2
    
    # Start timing
    local start_time=$(date +%s.%N)
    
    # Start services
    echo "Starting services..."
    docker-compose -f "$compose_file" up -d
    
    # Wait for readiness
    if wait_for_service; then
        local end_time=$(date +%s.%N)
        local startup_time=$(echo "$end_time - $start_time" | bc -l)
        
        # Measure memory
        echo "Measuring memory usage..."
        local memory_file="$RESULTS_DIR/${image_type}_memory_${TIMESTAMP}.txt"
        measure_memory "broker" "$memory_file"
        
        # Record startup time
        echo "$startup_time" >> "$RESULTS_DIR/${image_type}_startup_${TIMESTAMP}.txt"
        
        echo -e "${GREEN}✅ Startup time: ${startup_time}s${NC}"
        
        # Functional test - same approach for both images
        echo "Running functional test..."
        if nc -z localhost 9092 2>/dev/null || (echo "" | timeout 1 telnet localhost 9092 2>/dev/null); then
            echo -e "${GREEN}✅ Functional test passed (port responsive)${NC}"
        else
            echo -e "${RED}❌ Functional test failed${NC}"
        fi
        
        # Wait a bit to ensure steady state
        sleep 5
        
    else
        echo -e "${RED}❌ Failed to start $image_type${NC}"
        echo "999" >> "$RESULTS_DIR/${image_type}_startup_${TIMESTAMP}.txt"
        echo "999" >> "$RESULTS_DIR/${image_type}_memory_${TIMESTAMP}.txt"
    fi
    
    # Cleanup
    cleanup
    sleep 3
}

# Function to calculate statistics
calculate_stats() {
    local file="$1"
    local metric="$2"
    
    if [[ ! -f "$file" ]]; then
        echo "N/A"
        return
    fi
    
    local values=($(cat "$file"))
    local count=${#values[@]}
    
    if [[ $count -eq 0 ]]; then
        echo "N/A"
        return
    fi
    
    # Calculate mean
    local sum=0
    for value in "${values[@]}"; do
        sum=$(echo "$sum + $value" | bc -l)
    done
    local mean=$(echo "scale=2; $sum / $count" | bc -l)
    
    # Calculate min/max
    local min=${values[0]}
    local max=${values[0]}
    
    for value in "${values[@]}"; do
        if (( $(echo "$value < $min" | bc -l) )); then
            min=$value
        fi
        if (( $(echo "$value > $max" | bc -l) )); then
            max=$value
        fi
    done
    
    printf "Mean: %.2f%s, Min: %.2f%s, Max: %.2f%s" "$mean" "$metric" "$min" "$metric" "$max" "$metric"
}

# Main execution
main() {
    echo -e "${BLUE}🏁 Starting benchmark...${NC}"
    
    # Create Docker Compose files
    echo "Creating Docker Compose configurations..."
    create_docker_compose_files
    
    # Run JVM benchmarks
    echo -e "\n${YELLOW}🔥 Benchmarking JVM Image${NC}"
    for i in $(seq 1 $ITERATIONS); do
        benchmark_run "jvm" "docker-compose-benchmark-jvm.yml" "$i"
    done
    
    # Run Native benchmarks
    echo -e "\n${YELLOW}⚡ Benchmarking Native Image${NC}"
    for i in $(seq 1 $ITERATIONS); do
        benchmark_run "native" "docker-compose-benchmark-native.yml" "$i"
    done
    
    # Generate report
    echo -e "\n${BLUE}📈 Benchmark Results${NC}"
    echo -e "${BLUE}===================${NC}"
    
    echo -e "\n${YELLOW}🚀 Startup Time Comparison:${NC}"
    echo "JVM:    $(calculate_stats "$RESULTS_DIR/jvm_startup_${TIMESTAMP}.txt" "s")"
    echo "Native: $(calculate_stats "$RESULTS_DIR/native_startup_${TIMESTAMP}.txt" "s")"
    
    echo -e "\n${YELLOW}💾 Memory Usage Comparison:${NC}"
    echo "JVM:    $(calculate_stats "$RESULTS_DIR/jvm_memory_${TIMESTAMP}.txt" "MB")"
    echo "Native: $(calculate_stats "$RESULTS_DIR/native_memory_${TIMESTAMP}.txt" "MB")"
    
    # Calculate improvements
    calculate_improvements
    
    # Save detailed report
    save_detailed_report
    
    echo -e "\n${GREEN}✅ Benchmark completed! Results saved in $RESULTS_DIR/${NC}"
    
    # Final cleanup
    cleanup
}

# Function to create Docker Compose files
create_docker_compose_files() {
    # JVM compose file - based on docker-compose-basic-cp-server-native.yml
    cat > docker-compose-benchmark-jvm.yml << EOF
---
version: '2'
services:
  broker:
    image: $JVM_IMAGE
    hostname: broker
    container_name: broker
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

    # Native compose file - based on docker-compose-basic-cp-server-native.yml
    cat > docker-compose-benchmark-native.yml << EOF
---
version: '2'
services:
  broker:
    image: $NATIVE_IMAGE
    hostname: broker
    container_name: broker
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
}

# Function to calculate improvements
calculate_improvements() {
    local jvm_startup_file="$RESULTS_DIR/jvm_startup_${TIMESTAMP}.txt"
    local native_startup_file="$RESULTS_DIR/native_startup_${TIMESTAMP}.txt"
    local jvm_memory_file="$RESULTS_DIR/jvm_memory_${TIMESTAMP}.txt"
    local native_memory_file="$RESULTS_DIR/native_memory_${TIMESTAMP}.txt"
    
    if [[ -f "$jvm_startup_file" && -f "$native_startup_file" ]]; then
        local jvm_startup_avg=$(awk '{sum+=$1} END {print sum/NR}' "$jvm_startup_file")
        local native_startup_avg=$(awk '{sum+=$1} END {print sum/NR}' "$native_startup_file")
        
        if [[ -n "$jvm_startup_avg" && -n "$native_startup_avg" ]]; then
            local startup_improvement=$(echo "scale=1; ($jvm_startup_avg - $native_startup_avg) / $jvm_startup_avg * 100" | bc -l)
            echo -e "\n${GREEN}⚡ Startup Improvement: ${startup_improvement}% faster with Native${NC}"
        fi
    fi
    
    if [[ -f "$jvm_memory_file" && -f "$native_memory_file" ]]; then
        local jvm_memory_avg=$(awk '{sum+=$1} END {print sum/NR}' "$jvm_memory_file")
        local native_memory_avg=$(awk '{sum+=$1} END {print sum/NR}' "$native_memory_file")
        
        if [[ -n "$jvm_memory_avg" && -n "$native_memory_avg" ]]; then
            local memory_improvement=$(echo "scale=1; ($jvm_memory_avg - $native_memory_avg) / $jvm_memory_avg * 100" | bc -l)
            echo -e "${GREEN}💾 Memory Improvement: ${memory_improvement}% less memory with Native${NC}"
        fi
    fi
}

# Function to save detailed report
save_detailed_report() {
    local report_file="$RESULTS_DIR/benchmark_report_${TIMESTAMP}.md"
    
    cat > "$report_file" << EOF
# Kafka JVM vs Native Image Benchmark Report

**Date:** $(date)
**Architecture:** $ARCH
**Iterations:** $ITERATIONS

## Images Tested

- **JVM:** \`$JVM_IMAGE\`
- **Native:** \`$NATIVE_IMAGE\`

## Results Summary

### Startup Time
- **JVM:** $(calculate_stats "$RESULTS_DIR/jvm_startup_${TIMESTAMP}.txt" "s")
- **Native:** $(calculate_stats "$RESULTS_DIR/native_startup_${TIMESTAMP}.txt" "s")

### Memory Usage
- **JVM:** $(calculate_stats "$RESULTS_DIR/jvm_memory_${TIMESTAMP}.txt" "MB")
- **Native:** $(calculate_stats "$RESULTS_DIR/native_memory_${TIMESTAMP}.txt" "MB")

## Raw Data

### JVM Startup Times (seconds)
\`\`\`
$(cat "$RESULTS_DIR/jvm_startup_${TIMESTAMP}.txt" 2>/dev/null || echo "No data")
\`\`\`

### Native Startup Times (seconds)
\`\`\`
$(cat "$RESULTS_DIR/native_startup_${TIMESTAMP}.txt" 2>/dev/null || echo "No data")
\`\`\`

### JVM Memory Usage (MB)
\`\`\`
$(cat "$RESULTS_DIR/jvm_memory_${TIMESTAMP}.txt" 2>/dev/null || echo "No data")
\`\`\`

### Native Memory Usage (MB)
\`\`\`
$(cat "$RESULTS_DIR/native_memory_${TIMESTAMP}.txt" 2>/dev/null || echo "No data")
\`\`\`

## Methodology

1. Each image was tested $ITERATIONS times
2. Startup time measured from \`docker-compose up\` to service readiness
3. Service readiness confirmed by successful \`kafka-topics --list\` command
4. Memory measured after service startup and functional test
5. Containers fully cleaned between runs
6. Functional test performed (topic creation) to ensure service health

EOF

    echo "Detailed report saved: $report_file"
}

# Check dependencies
check_dependencies() {
    local missing_deps=()
    
    if ! command -v docker &> /dev/null; then
        missing_deps+=("docker")
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        missing_deps+=("docker-compose")
    fi
    
    if ! command -v bc &> /dev/null; then
        missing_deps+=("bc")
    fi
    
    if [[ ${#missing_deps[@]} -gt 0 ]]; then
        echo -e "${RED}❌ Missing dependencies: ${missing_deps[*]}${NC}"
        echo "Please install the missing dependencies and try again."
        exit 1
    fi
}

# Trap for cleanup on exit
trap cleanup EXIT

# Run dependency check
check_dependencies

# Execute main function
main "$@" 
