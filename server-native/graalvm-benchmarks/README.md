# 🚀 Kafka JVM vs GraalVM Native Benchmark Suite

This directory contains comprehensive benchmarking tools to compare **JVM-based Confluent Server** with **GraalVM Native Confluent Server** images.

## 📊 Latest Benchmark Results

### **Performance Comparison (5 iterations, arm64)**

| Metric | JVM | Native | Improvement |
|--------|-----|--------|-------------|
| **Startup Time** | 9.62s | 1.85s | **⚡ 81% faster** |
| **Memory Usage** | 646.6MB | 289.3MB | **💾 55% lower** |
| **Image Size** | 2,428.3MB | 726.7MB | **📦 70% smaller** |
| **Startup Range** | 8.6s - 9.9s | 1.8s - 1.9s | Consistent performance |

### **Key Findings:**
- **81% faster startup** with GraalVM native compilation
- **55% lower memory footprint** 
- **70% smaller image size** (2.4GB vs 726.7MB)
- **Consistent performance** across multiple runs
- **True operational readiness** measured via "Kafka Server started" log message

---

## 🛠 How to Run Benchmarks

### **Quick Benchmark (Recommended for Testing)**
```bash
# Run 5 iterations (recommended)
./quick-benchmark.sh 5

# Single run for testing
./quick-benchmark.sh 1

# More iterations for statistical significance
./quick-benchmark.sh 10
```

### **Comprehensive Benchmark (Full Analysis)**
```bash
# Run full benchmark suite with detailed reporting
./detailed-benchmark.sh

# Results saved to timestamped directory in benchmark-results/
```

### **Advanced Analysis with Visualization**
```bash
# After running comprehensive benchmark
python3 analyze_results.py

# Export results to JSON
python3 analyze_results.py --export-json
```

---

## 📏 Measurement Methodology

### **🕐 Startup Time Calculation**

**What we measure:** Time from `docker-compose up` until Kafka is fully operational.

**Primary Readiness Indicator:**
```bash
# Most authoritative - indicates core Kafka server is ready
"[KafkaRaftServer nodeId=1] Kafka Server started"
```

**Fallback Readiness Indicators:**
```bash
# HTTP services ready (if primary not found)
"KafkaHttpServer transitioned from STARTING to RUNNING"
"Started NetworkTrafficServerConnector"
```

**Measurement Process:**
1. Record timestamp before `docker-compose up -d`
2. Poll container logs every second for readiness message
3. Calculate `startup_time = end_time - start_time`
4. Timeout after 60 seconds if not ready

### **💾 Memory Usage Calculation**

**What we measure:** Container memory usage after Kafka is fully started.

**Data Source:**
```bash
docker stats --no-stream --format "{{.MemUsage}}" container_name
# Returns format: "289.3MiB / 7.667GiB"
```

**Unit Conversion Logic:**
- `KiB` → `MB`: `value / 1024`
- `MiB` → `MB`: `value` (direct)
- `GiB` → `MB`: `value * 1024`

**Memory Measurement:**
1. Wait for Kafka to be fully ready
2. Get container memory stats
3. Parse and convert to MB for consistent reporting
4. Record peak memory usage

**Image Size Measurement:**
1. Pull both images if not already available
2. Use `docker image inspect --format '{{.Size}}'` to get size in bytes
3. Convert bytes to MB: `size_bytes / 1024 / 1024`
4. Calculate size reduction percentage: `(jvm_bytes - native_bytes) / jvm_bytes * 100`

---

## 🔧 Manual Benchmark Process

### **Prerequisites**
```bash
# Ensure Docker images are available
docker pull 519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/prod/confluentinc/cp-server:8.0.x-latest-ubi9
docker pull 519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/dev/confluentinc/cp-server-native:dev-8.0.x-7842c5ec-ubi9.arm64

# Install dependencies
python3 -m pip install matplotlib numpy
```

### **Manual JVM Test**
```bash
# 1. Create JVM docker-compose file
cat > manual-jvm-test.yml << EOF
---
version: '2'
services:
  broker:
    image: 519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/prod/confluentinc/cp-server:8.0.x-latest-ubi9
    hostname: broker
    container_name: broker
    ports:
      - "9092:9092"
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

# 2. Start and time
start_time=$(date +%s.%N)
docker-compose -f manual-jvm-test.yml up -d

# 3. Wait for "Kafka Server started" message
while ! docker logs broker 2>&1 | grep -q -i "kafka.*server.*started"; do
    sleep 1
done
end_time=$(date +%s.%N)

# 4. Calculate startup time
startup_time=$(echo "$end_time - $start_time" | bc -l)
echo "JVM Startup Time: ${startup_time}s"

# 5. Get memory usage
memory=$(docker stats --no-stream --format "{{.MemUsage}}" broker | cut -d'/' -f1 | xargs)
echo "JVM Memory Usage: $memory"

# 6. Cleanup
docker-compose -f manual-jvm-test.yml down
rm manual-jvm-test.yml
```

### **Manual Native Test**
```bash
# Same process but with native image:
# Replace image with: 519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/dev/confluentinc/cp-server-native:dev-8.0.x-7842c5ec-ubi9.arm64
```

---

## 📁 File Structure

```
examples/confluent-server/
├── detailed-benchmark.sh       # Comprehensive benchmark suite
├── quick-benchmark.sh         # Fast iteration testing
├── analyze_results.py         # Python analysis and visualization
├── BENCHMARK_README.md        # This documentation
├── benchmark-results/         # Timestamped results directory
│   └── benchmark_YYYYMMDD_HHMMSS/
│       ├── detailed_report.md
│       ├── jvm_results.csv
│       ├── native_results.csv
│       └── summary_stats.json
└── docker-compose-basic-cp-server-native.yml  # Reference configuration
```

---

## 🔍 Understanding the Results

### **Environment Variables Critical for Accurate Testing**

The complete environment variable set is **essential** for proper Kafka startup and the "Kafka Server started" message:

```yaml
# Core Kafka Configuration
COMPONENT: 'kafka'
KAFKA_NODE_ID: 1
KAFKA_PROCESS_ROLES: 'broker,controller'
CLUSTER_ID: '4L6g3nShT-eMCtK--X86sw'

# Network Configuration  
KAFKA_LISTENERS: 'PLAINTEXT://broker:29092,CONTROLLER://broker:29093,PLAINTEXT_HOST://0.0.0.0:9092'
KAFKA_ADVERTISED_LISTENERS: 'PLAINTEXT://broker:29092,PLAINTEXT_HOST://localhost:9092'

# Confluent-Specific Settings (Required for full startup)
KAFKA_CONFLUENT_METADATA_SERVER_LISTENERS: 'http://localhost:28090'
CONFLUENT_METRICS_REPORTER_BOOTSTRAP_SERVERS: 'localhost:29092'

# Replication Factors (All set to 1 for single-broker setup)
KAFKA_CONFLUENT_LICENSE_TOPIC_REPLICATION_FACTOR: 1
KAFKA_CONFLUENT_BALANCER_TOPIC_REPLICATION_FACTOR: 1
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
# ... and 12 more replication factor settings
```

### **Why These Results Matter**

1. **Realistic Startup Times**: Using "Kafka Server started" ensures we measure true operational readiness
2. **Complete Configuration**: All Confluent-specific settings ensure full feature initialization
3. **Production-Representative**: Single-broker setup with proper replication factors
4. **Consistent Measurement**: Same methodology for both JVM and native images

---

## 🚨 Troubleshooting

### **Common Issues**

**Container fails to start:**
```bash
# Check for hostname resolution issues in logs
docker logs broker

# Common error: "Unresolved address" 
# Fix: Ensure hostnames match between listeners and advertised listeners
```

**No "Kafka Server started" message:**
```bash
# Verify complete environment variables are set
# Check: All replication factors, metadata server listeners, metrics reporter settings
```

**Memory measurement shows unrealistic values:**
```bash
# Check unit conversion in scripts
# Ensure parsing docker stats output correctly: "289.3MiB / 7.667GiB"
```

### **Image Availability**

```bash
# JVM Image (Production)
docker pull 519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/prod/confluentinc/cp-server:8.0.x-latest-ubi9

# Native Image (Development - ARM64)
docker pull 519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/dev/confluentinc/cp-server-native:dev-8.0.x-7842c5ec-ubi9.arm64

# Native Image (Development - AMD64)  
docker pull 519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/dev/confluentinc/cp-server-native:dev-8.0.x-7842c5ec-ubi9.amd64
```

---

## 🎯 Conclusion

The GraalVM native Confluent Server demonstrates significant performance improvements:

- **⚡ 81% faster startup** - Critical for serverless and rapid scaling scenarios
- **💾 55% memory efficiency** - Important for resource-constrained environments  
- **📦 70% smaller image size** - Faster downloads, reduced storage costs, improved CI/CD
- **🔄 Consistent performance** - Reliable across multiple test iterations

These benchmarks provide concrete evidence of GraalVM native compilation benefits for Kafka workloads, making it an excellent choice for modern cloud-native deployments where startup speed, memory efficiency, and image size matter. 
