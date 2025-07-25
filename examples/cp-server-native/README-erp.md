# Embedded REST Proxy (ERP) + GraalVM Native Kafka Demo

This example demonstrates Kafka's Embedded REST Proxy functionality with GraalVM native images, showcasing REST API v3 endpoints for topic management and message production.

## What This Demonstrates

- **Embedded REST Proxy (ERP)**: Built-in REST API within Kafka broker
- **REST API v3**: Modern RESTful interface for Kafka operations
- **GraalVM Native**: Fast startup with REST API support
- **JSON Messaging**: REST-based message production
- **Cross-Protocol**: REST-produced messages consumed via console tools

## Architecture

```
┌─────────────────────┐    ┌──────────────────┐
│     producer        │    │     consumer     │
│   (curl/REST)       │    │  (kafka-console) │
│                     │    │                  │
│  ┌───────────────┐  │    │                  │
│  │ REST API v3   │  │    │                  │
│  │ :28090        │◄─┼────┤                  │
│  └───────────────┘  │    │                  │
│  ┌───────────────┐  │    │                  │
│  │     broker    │  │    │                  │
│  │     :29092    │◄─┼────┤                  │
│  └───────────────┘  │    │                  │
└─────────────────────┘    └──────────────────┘
```

## Quick Start

```bash
# Start all services
docker-compose -f docker-compose-erp-cp-server-native.yml up -d

# Watch automated testing
docker logs -f producer    # REST API calls and JSON message production
docker logs -f consumer    # Console-based message consumption
```

## Automated Testing Flow

The setup automatically:

1. **Broker**: Starts Kafka with Embedded REST Proxy enabled
2. **Producer** (curl-based):
   - Tests Metadata Server availability 
   - Explores REST API v3 endpoints:
     - `GET /kafka/v3/clusters` - List clusters
     - `GET /kafka/v3/clusters/{id}` - Cluster details  
     - `GET /kafka/v3/clusters/{id}/brokers` - Broker information
   - Creates `rest-proxy-topic` via REST API
   - Produces 3 JSON messages via REST API
   - Stays alive for manual REST testing
3. **Consumer** (kafka-console):
   - Consumes the 3 REST-produced messages
   - Demonstrates cross-protocol compatibility
   - Stays alive for manual testing

### Expected Output

**Producer logs:**
```
=== Testing Metadata Server availability ===
✅ Metadata Server is ready!
=== Testing Embedded REST Proxy v3 APIs ===
🔍 GET /kafka/v3/clusters - List clusters
🔍 GET /kafka/v3/clusters/{cluster_id} - Get cluster details
🔍 GET /kafka/v3/clusters/{cluster_id}/brokers - List brokers
=== Creating topic via REST API ===
=== Listing topics via REST API ===
=== Producing messages via REST API ===
📝 Producing Message 1: REST API Message 1 - Hello from Embedded REST Proxy!
📝 Producing Message 2: REST API Message 2 - Testing GraalVM Native REST endpoints
📝 Producing Message 3: REST API Message 3 - Embedded REST Proxy test completed
🚀 Messages sent via REST API!
Producer container staying alive for manual testing.
```

**Consumer logs:**
```
=== Starting to consume messages from REST-produced topic ===
{"message": "REST API Message 1 - Hello from Embedded REST Proxy!", "timestamp": "2025-01-22T10:00:00+00:00"}
{"message": "REST API Message 2 - Testing GraalVM Native REST endpoints", "timestamp": "2025-01-22T10:01:00+00:00"}
{"message": "REST API Message 3 - Embedded REST Proxy test completed", "timestamp": "2025-01-22T10:02:00+00:00"}
=== Consumer finished successfully ===
```

## Manual Testing

### Access Containers
```bash
docker exec -it producer sh      # curl/REST API testing
docker exec -it consumer bash    # Kafka console tools
docker exec -it broker bash      # Kafka broker management
```

### Manual REST API Testing

#### Cluster Operations
```bash
# Inside producer container (has curl)

# List all clusters
curl -s http://broker:28090/kafka/v3/clusters | jq

# Get cluster details  
curl -s http://broker:28090/kafka/v3/clusters/vHCgQyIrRHG8Jv27qI2h3Q | jq

# List brokers
curl -s http://broker:28090/kafka/v3/clusters/vHCgQyIrRHG8Jv27qI2h3Q/brokers | jq
```

#### Topic Operations
```bash
# List topics
curl -s http://broker:28090/kafka/v3/clusters/vHCgQyIrRHG8Jv27qI2h3Q/topics | jq

# Get topic details
curl -s http://broker:28090/kafka/v3/clusters/vHCgQyIrRHG8Jv27qI2h3Q/topics/rest-proxy-topic | jq

# List topic partitions
curl -s http://broker:28090/kafka/v3/clusters/vHCgQyIrRHG8Jv27qI2h3Q/topics/rest-proxy-topic/partitions | jq

# Create new topic
curl -X POST http://broker:28090/kafka/v3/clusters/vHCgQyIrRHG8Jv27qI2h3Q/topics \
  -H "Content-Type: application/json" \
  -d '{
    "topic_name": "my-rest-topic",
    "partitions_count": 3,
    "replication_factor": 1
  }'
```

#### Message Production
```bash
# Produce JSON message
curl -X POST http://broker:28090/kafka/v3/clusters/vHCgQyIrRHG8Jv27qI2h3Q/topics/rest-proxy-topic/records \
  -H "Content-Type: application/json" \
  -d '{
    "value": {
      "type": "JSON",
      "data": {"message": "Hello from manual REST API!", "timestamp": "'$(date -Iseconds)'"}
    }
  }'

# Produce with key
curl -X POST http://broker:28090/kafka/v3/clusters/vHCgQyIrRHG8Jv27qI2h3Q/topics/rest-proxy-topic/records \
  -H "Content-Type: application/json" \
  -d '{
    "key": {
      "type": "STRING",
      "data": "user123"
    },
    "value": {
      "type": "JSON", 
      "data": {"user": "user123", "action": "login", "timestamp": "'$(date -Iseconds)'"}
    }
  }'
```

### Manual Kafka Console Testing
```bash
# Inside consumer container

# Consume all messages from beginning
kafka-console-consumer --topic rest-proxy-topic --bootstrap-server broker:29092 --from-beginning

# Consume with key information
kafka-console-consumer --topic rest-proxy-topic --bootstrap-server broker:29092 \
  --property print.key=true --property key.separator=" : " --from-beginning

# Create and consume from new topic
kafka-topics --create --topic manual-test --partitions 1 --replication-factor 1 --bootstrap-server broker:29092
kafka-console-consumer --topic manual-test --bootstrap-server broker:29092
```

## Endpoints

| Service | Container | Port | Purpose | Protocol |
|---------|-----------|------|---------|----------|
| Embedded REST Proxy | `broker` | 28090 | REST API v3 | HTTP |
| Kafka API | `broker` | 29092 | Kafka Protocol | TCP |
| External Kafka | `broker` | 9092 | Host Access | TCP |
| JMX | `broker` | 9101 | Metrics | TCP |

## REST API v3 Endpoints

### Core Endpoints
- `GET /kafka/v3/clusters` - List clusters
- `GET /kafka/v3/clusters/{cluster_id}` - Cluster info
- `GET /kafka/v3/clusters/{cluster_id}/brokers` - Broker list

### Topic Management  
- `GET /kafka/v3/clusters/{cluster_id}/topics` - List topics
- `POST /kafka/v3/clusters/{cluster_id}/topics` - Create topic
- `GET /kafka/v3/clusters/{cluster_id}/topics/{topic}` - Topic details
- `GET /kafka/v3/clusters/{cluster_id}/topics/{topic}/partitions` - Partitions

### Message Production
- `POST /kafka/v3/clusters/{cluster_id}/topics/{topic}/records` - Produce message

### Example JSON Message Format
```json
{
  "key": {
    "type": "STRING",
    "data": "key123"
  },
  "value": {
    "type": "JSON",
    "data": {
      "message": "Hello World",
      "timestamp": "2025-01-22T10:00:00Z",
      "metadata": {
        "source": "rest-api",
        "version": "1.0"
      }
    }
  }
}
```

## Key Features

- ✅ **Embedded REST Proxy**: No separate REST Proxy deployment needed
- ✅ **REST API v3**: Modern, standardized REST interface
- ✅ **JSON Support**: Rich JSON message formats
- ✅ **Cross-Protocol**: REST ↔ Console tool compatibility  
- ✅ **GraalVM Native**: Fast startup with full REST functionality
- ✅ **Production Ready**: Enterprise-grade REST API

## Use Cases

- **Web Applications**: Direct HTTP integration with Kafka
- **Microservices**: REST-based messaging between services
- **Data Integration**: HTTP-based data ingestion
- **Monitoring**: REST API for cluster monitoring
- **DevOps**: HTTP-based Kafka management

## Cleanup

```bash
# Stop all services
docker-compose -f docker-compose-erp-cp-server-native.yml down

# Remove volumes
docker-compose -f docker-compose-erp-cp-server-native.yml down -v
```

## Troubleshooting

### REST API not accessible
- Check port 28090 is not in use: `netstat -an | grep 28090`
- Verify broker started successfully: `docker logs broker`
- Test connectivity: `curl http://localhost:28090/kafka/v3/clusters`

### Message production fails
- Check JSON format is valid
- Verify Content-Type header: `application/json`
- Ensure topic exists or can be auto-created
- Check broker logs for errors

### Cross-protocol issues
- REST-produced messages appear as JSON in console consumer (expected)
- Use `jq` to format JSON output: `curl ... | jq`
- Different protocols have different serialization formats

### Performance considerations
- REST API has higher latency than native Kafka protocol
- Use for convenience/integration, not high-throughput scenarios
- Consider batching for better performance 
