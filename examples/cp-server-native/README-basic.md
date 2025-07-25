# Basic Kafka Native Image Demo

This example demonstrates a simple Kafka setup using GraalVM native images with automated producer and consumer testing.

## What This Demonstrates

- **GraalVM Native Kafka**: Fast startup, low memory usage Kafka broker
- **Basic Topics**: Creating and using Kafka topics
- **Producer/Consumer**: Simple message production and consumption
- **Manual Testing**: Interactive containers for further experimentation

## Architecture

```
┌─────────┐    ┌──────────┐    ┌──────────┐
│ broker  │    │ producer │    │ consumer │
│  :9092  │◄───┤          │───►│          │
│         │    │          │    │          │
└─────────┘    └──────────┘    └──────────┘
```

## Quick Start

```bash
# Start all services
docker-compose -f docker-compose-basic-cp-server-native.yml up -d

# Watch automated testing
docker logs -f producer    # See messages being produced
docker logs -f consumer    # See messages being consumed
```

## Automated Testing

The setup automatically:

1. **Broker**: Starts Kafka with GraalVM native image
2. **Producer**: 
   - Creates `demo-topic` (1 partition for guaranteed ordering)
   - Produces 3 test messages
   - Stays alive for manual testing
3. **Consumer**:
   - Consumes exactly 3 messages from `demo-topic`
   - Stays alive for manual testing

### Expected Output

**Producer logs:**
```
=== Creating topic: demo-topic ===
=== Listing all topics ===
=== Producing 3 unique messages ===
📝 Producing Message 1: Hello from Docker Producer
📝 Producing Message 2: Testing Kafka Native Binary
📝 Producing Message 3: Producer container completed
=== Producer finished successfully ===
Producer container staying alive for manual testing.
```

**Consumer logs:**
```
=== Starting to consume messages from beginning ===
Message 1: Hello from Docker Producer
Message 2: Testing Kafka Native Binary
Message 3: Producer container completed
=== Consumer finished successfully ===
Consumer container staying alive for manual testing.
```

## Manual Testing

Once automated testing completes, all containers stay alive for interactive use:

### Access Containers
```bash
docker exec -it broker bash      # Kafka broker management
docker exec -it producer bash    # Producer commands
docker exec -it consumer bash    # Consumer commands
```

### Manual Producer Commands
```bash
# Inside producer container
kafka-console-producer --topic demo-topic --bootstrap-server broker:29092

# Type messages and press Enter
# Press Ctrl+C to exit
```

### Manual Consumer Commands
```bash
# Inside consumer container

# Consume all messages from beginning
kafka-console-consumer --topic demo-topic --bootstrap-server broker:29092 --from-beginning

# Consume only new messages
kafka-console-consumer --topic demo-topic --bootstrap-server broker:29092
```

### Topic Management
```bash
# Inside any container

# List topics
kafka-topics --list --bootstrap-server broker:29092

# Describe topic
kafka-topics --describe --topic demo-topic --bootstrap-server broker:29092

# Create new topic
kafka-topics --create --topic my-test --partitions 1 --replication-factor 1 --bootstrap-server broker:29092
```

## Endpoints

| Service | Container | Port | Purpose |
|---------|-----------|------|---------|
| Kafka | `broker` | 9092 | Kafka API |
| JMX | `broker` | 9101 | Metrics |
| Metadata Server | `broker` | 8090 | REST API (if enabled) |

## Key Features

- ✅ **Fast Startup**: GraalVM native images start in seconds
- ✅ **Low Memory**: Reduced memory footprint vs JVM
- ✅ **Guaranteed Order**: Single partition ensures message ordering
- ✅ **Automated Testing**: Validates setup automatically
- ✅ **Manual Ready**: All containers stay alive for exploration

## Cleanup

```bash
# Stop all services
docker-compose -f docker-compose-basic-cp-server-native.yml down

# Remove volumes (optional)
docker-compose -f docker-compose-basic-cp-server-native.yml down -v
```

## Troubleshooting

### Services won't start
- Check ports 9092, 9101, 8090 aren't in use
- Ensure Docker has enough memory (4GB+ recommended)

### Messages out of order
- This example uses 1 partition to guarantee order
- Multiple partitions provide better throughput but no cross-partition ordering

### Producer/Consumer fails
- Wait for broker to fully start (check `docker logs broker`)
- Ensure topic exists: `kafka-topics --list --bootstrap-server broker:29092` 
