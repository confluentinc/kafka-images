# MDS File Store + SASL Authentication Demo

This example demonstrates Metadata Server (MDS) with file-based user store and SASL/PLAIN authentication using GraalVM native Kafka.

## What This Demonstrates

- **Metadata Server (MDS)**: Confluent's management and security API
- **File-Based User Store**: Simple user authentication via properties file
- **SASL/PLAIN**: Username/password authentication
- **RBAC Ready**: Foundation for Role-Based Access Control
- **Automated Testing**: Producer/consumer with authentication

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   producer      │    │     broker      │    │   consumer      │
│                 │    │   :9092 (SASL)  │    │                 │
│  alice:alice-   │───►│   :8090 (MDS)   │    │  alice:alice-   │
│  secret         │    │                 │◄───│  secret         │
│                 │    │ ┌─────────────┐ │    │                 │
│  (writes msgs)  │    │ │ users.prop  │ │    │  (reads msgs)   │
│                 │    │ │ server_jaas │ │    │                 │
│                 │    │ └─────────────┘ │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Prerequisites

Ensure you have the following files in the directory:
- `mds-config/users.properties` - User credentials file
- `mds-config/server_jaas.conf` - JAAS configuration for SASL

## Quick Start

```bash
# Start all services
docker-compose -f docker-compose-mds-file-store.yml up -d

# Watch automated testing
docker logs -f producer    # SASL-authenticated message production
docker logs -f consumer    # SASL-authenticated message consumption
```

## Automated Testing Flow

The setup automatically:

1. **Broker**: Starts Kafka with MDS and SASL/PLAIN authentication
2. **Producer**:
   - Creates SASL client configuration
   - Lists existing topics (with authentication)
   - Creates `poc-demo` topic
   - Tests MDS API endpoints
   - Produces 5 test messages with varied content
   - Stays alive for manual testing
3. **Consumer**:
   - Consumes exactly 5 messages using SASL authentication
   - Stays alive for manual testing

### Expected Output

**Producer logs:**
```
Creating client.properties...
Listing existing topics...
Creating poc-demo topic...

Created topic poc-demo.
Listing topics after creation...
poc-demo
Testing MDS API...
{"kind":"KafkaClusterList","metadata":{"self":"http://broker:8090/kafka/v3/clusters"...}
🚀 Sending 5 messages to kafka topic poc-demo...
📝 Message 1: Hello from automated producer
[SLF4J warnings]
📝 Message 2: Testing SASL authentication
[SLF4J warnings]
📝 Message 3: MDS file store demo
[SLF4J warnings]
📝 Message 4: Producer container test
[SLF4J warnings]
📝 Message 5: Automated testing complete
[SLF4J warnings]
🚀 5 messages sent to poc-demo topic!
Producer container staying alive for manual testing.
```

**Consumer logs:**
```
Starting consumer for poc-demo topic...
Message 1: Hello from automated producer
Message 2: Testing SASL authentication
Message 3: MDS file store demo
Message 4: Producer container test
Message 5: Automated testing complete
Consumer finished capturing 5 messages!
Consumer container staying alive for manual testing.
```

## Authentication Configuration

### User Credentials (`mds-config/users.properties`)
```properties
alice=alice-secret
bob=bob-secret
admin=admin-secret
```

### SASL Client Configuration (auto-generated)
```properties
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
  username="alice" \
  password="alice-secret";
```

## Manual Testing

### Access Containers
```bash
docker exec -it broker bash      # Kafka broker with MDS
docker exec -it producer bash    # SASL producer testing
docker exec -it consumer bash    # SASL consumer testing
```

### Manual Producer Commands
```bash
# Inside producer container (client.properties already exists)

# Interactive producer
kafka-console-producer --bootstrap-server broker:9092 --topic poc-demo --producer.config /tmp/client.properties

# Producer with different user (create new config)
cat > /tmp/bob-client.properties <<EOF
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="bob" password="bob-secret";
EOF

kafka-console-producer --bootstrap-server broker:9092 --topic poc-demo --producer.config /tmp/bob-client.properties
```

### Manual Consumer Commands
```bash
# Inside consumer container

# Consume all messages from beginning
kafka-console-consumer --bootstrap-server broker:9092 --topic poc-demo --from-beginning --consumer.config /tmp/client.properties

# Consume only new messages
kafka-console-consumer --bootstrap-server broker:9092 --topic poc-demo --consumer.config /tmp/client.properties

# Consumer with different user
cat > /tmp/bob-client.properties <<EOF
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="bob" password="bob-secret";
EOF

kafka-console-consumer --bootstrap-server broker:9092 --topic poc-demo --consumer.config /tmp/bob-client.properties --from-beginning
```

### Topic Management with SASL
```bash
# List topics (requires authentication)
kafka-topics --list --bootstrap-server broker:9092 --command-config /tmp/client.properties

# Create new topic
kafka-topics --create --topic secure-topic --partitions 3 --replication-factor 1 --bootstrap-server broker:9092 --command-config /tmp/client.properties

# Describe topic
kafka-topics --describe --topic poc-demo --bootstrap-server broker:9092 --command-config /tmp/client.properties

# Delete topic  
kafka-topics --delete --topic secure-topic --bootstrap-server broker:9092 --command-config /tmp/client.properties
```

### MDS API Testing
```bash
# Test MDS endpoints (from host or inside containers with curl)

# Cluster information
curl -s http://localhost:8090/kafka/v3/clusters | jq

# Broker information  
curl -s http://localhost:8090/kafka/v3/clusters/4L6g3nShT-eMCtK--X86sw/brokers | jq

# Topics via MDS API
curl -s http://localhost:8090/kafka/v3/clusters/4L6g3nShT-eMCtK--X86sw/topics | jq

# Health check
curl -s http://localhost:8090/v1/metadata/id
```

## Endpoints

| Service | Container | Port | Purpose | Auth Required |
|---------|-----------|------|---------|---------------|
| Kafka SASL | `broker` | 9092 | SASL Kafka API | Yes (alice:alice-secret) |
| MDS API | `broker` | 8090 | Metadata Server | No (read-only endpoints) |

## Security Features

- ✅ **SASL/PLAIN Authentication**: Username/password protection
- ✅ **File-based User Store**: Simple credential management
- ✅ **MDS Integration**: Ready for RBAC when needed
- ✅ **Hot Reload**: User changes without restart (if configured)
- ✅ **Multiple Users**: Support for multiple authenticated users

## User Management

### Adding New Users
1. Edit `mds-config/users.properties`:
   ```properties
   alice=alice-secret
   bob=bob-secret
   charlie=charlie-secret
   ```

2. Restart broker or use hot reload:
   ```bash
   docker-compose -f docker-compose-mds-file-store.yml restart broker
   ```

### Client Configuration for New Users
```bash
# Create client config for new user
cat > client-charlie.properties <<EOF
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="charlie" password="charlie-secret";
EOF
```

## Key Features

- ✅ **Simple Authentication**: Easy username/password setup
- ✅ **MDS Ready**: Foundation for advanced RBAC
- ✅ **File-based**: No external dependencies
- ✅ **Multi-user**: Support for multiple authenticated users
- ✅ **Hot Reload**: Update users without downtime (configurable)
- ✅ **GraalVM Native**: Fast startup with security features

## Migration Path

This setup provides a foundation for:
1. **LDAP Integration**: Replace file store with LDAP
2. **OAuth Integration**: Upgrade to OAuth/OIDC
3. **RBAC**: Add role-based permissions
4. **TLS**: Add encryption in transit

## Cleanup

```bash
# Stop all services
docker-compose -f docker-compose-mds-file-store.yml down

# Remove volumes
docker-compose -f docker-compose-mds-file-store.yml down -v
```

## Troubleshooting

### Authentication failures
- Check username/password in `users.properties`
- Verify JAAS configuration syntax
- Ensure `security.protocol=SASL_PLAINTEXT` in client config
- Check broker logs: `docker logs broker`

### MDS API not accessible
- Verify port 8090 is available
- Check MDS is enabled in broker config
- Test with simple curl: `curl http://localhost:8090/v1/metadata/id`

### Producer/Consumer hangs
- Authentication likely failing
- Check client configuration file exists and is correct
- Verify user exists in users.properties
- Check broker logs for authentication errors

### Topic operations fail
- All topic operations require authentication
- Ensure `--command-config` parameter is used
- Verify user has sufficient permissions (default: all users can create topics)

### File changes not taking effect
- Restart broker if hot reload not configured
- Check file permissions and syntax
- Verify file is properly mounted in container 
