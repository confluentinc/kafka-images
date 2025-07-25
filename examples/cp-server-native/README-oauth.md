# OAuth with Keycloak + GraalVM Native Kafka Demo

This example demonstrates OAuth authentication with Keycloak integrated with `cp-server-native` GraalVM native Kafka, including RBAC permissions and automated testing.

## What This Demonstrates

- **OAuth Authentication**: Keycloak OIDC integration with Kafka
- **RBAC (Role-Based Access Control)**: Confluent Server authorization
- **GraalVM Native**: Fast startup Kafka with OAuth support
- **Automated RBAC Setup**: Automatic permission configuration
- **Secure Messaging**: End-to-end OAuth-protected message flow

## Architecture

```
┌─────────────────────┐   ┌───────────┐          ┌─────────────────┐          ┌──────────┐
│    oauth-setup      │   │ keycloak  │          │     broker      │          │ consumer │
│  (RBAC Config)      │──►│   :8080   │◄─────────┤  :9095 (OAuth)  │◄─────────┤  OAuth   │
└─────────────────────┘   │ (OAuth)   │          │  :8091 (MDS)    │          │ (reads)  │
          │               └───────────┘          └─────────────────┘          └──────────┘
          │                                          ▲     ▲ 
          └──────────────────────────────────────────┘     │ 
                                                           │
                                                 ┌─────────────────┐
                                                 │    producer     │
                                                 │     OAuth       │
                                                 │    (writes)     │
                                                 └─────────────────┘
```

## Prerequisites

Ensure you have the following files in the directory:
- `oauth-client.properties` - OAuth client configuration
- `keycloak-realm-export.json` - Keycloak realm configuration
- `create-certificates.sh` - SSL certificates script

## Quick Start

```bash
# Start all services
docker-compose -f docker-compose-oauth-cp-server-native.yml up -d

# Watch automated setup and testing
docker logs -f oauth-setup    # RBAC permissions setup
docker logs -f producer       # OAuth message production  
docker logs -f consumer       # OAuth message consumption
```

## Automated Testing Flow

The setup automatically performs:

1. **Keycloak**: Starts OAuth provider with imported realm
2. **Broker**: Starts Kafka with OAuth and RBAC enabled
3. **OAuth Setup**: 
   - Gets superuser OAuth token from Keycloak
   - Configures RBAC permissions for `client_app1` user
   - Grants topic and consumer group permissions
4. **Producer**:
   - Creates `oauth-test-topic` using OAuth authentication
   - Produces 3 test messages with OAuth
   - Stays alive for manual testing
5. **Consumer**:
   - Consumes exactly 3 messages using OAuth
   - Stays alive for manual testing

### Expected Output

**OAuth Setup logs:**
```
=== Getting superuser OAuth token ===
Token obtained, setting up RBAC permissions...
=== Granting topic permissions ===
=== Granting consumer group permissions ===
✅ OAuth RBAC permissions setup completed!
```

**Producer logs:**
```
Creating topic: oauth-test-topic
Listing topics:
📝 Message 1: OAuth Message 1: Hello from OAuth Producer!
📝 Message 2: OAuth Message 2: Testing OAuth authentication
📝 Message 3: OAuth Message 3: Secure messaging with OAuth
🚀 Sending messages to Kafka...
🚀 3 messages sent to oauth-test-topic!
Producer container staying alive for manual testing.
```

**Consumer logs:**
```
Starting consumer for oauth-test-topic (consuming 3 messages)...
OAuth Message 1: Hello from OAuth Producer!
OAuth Message 2: Testing OAuth authentication
OAuth Message 3: Secure messaging with OAuth
Consumer finished capturing 3 messages!
```

## Manual Testing

### Access Containers
```bash
docker exec -it broker bash      # Kafka broker with OAuth
docker exec -it producer bash    # OAuth producer testing
docker exec -it consumer bash    # OAuth consumer testing
```

### Manual Producer Commands
```bash
# Inside producer container
kafka-console-producer --bootstrap-server broker:9095 --topic oauth-test-topic --producer.config /tmp/oauth-client.properties

# Type messages and press Enter
# Authentication is handled automatically via oauth-client.properties
```

### Manual Consumer Commands
```bash
# Inside consumer container

# Consume all messages from beginning
kafka-console-consumer --bootstrap-server broker:9095 --topic oauth-test-topic --from-beginning --consumer.config /tmp/oauth-client.properties

# Consume only new messages  
kafka-console-consumer --bootstrap-server broker:9095 --topic oauth-test-topic --consumer.config /tmp/oauth-client.properties
```

### Topic Management with OAuth
```bash
# List topics (requires OAuth authentication)
kafka-topics --list --bootstrap-server broker:9095 --command-config /tmp/oauth-client.properties

# Create new topic with OAuth
kafka-topics --create --topic my-secure-topic --partitions 1 --replication-factor 1 --bootstrap-server broker:9095 --command-config /tmp/oauth-client.properties

# Describe topic
kafka-topics --describe --topic oauth-test-topic --bootstrap-server broker:9095 --command-config /tmp/oauth-client.properties
```

### RBAC Management
```bash
# Check MDS (Metadata Server) health
curl http://localhost:8091/v1/metadata/id

# List current role bindings (requires superuser token)
curl -H "Authorization: Bearer <TOKEN>" http://localhost:8091/security/1.0/principals/User:client_app1/roles
```

## Endpoints

| Service | Container | Port | Purpose | Auth |
|---------|-----------|------|---------|------|
| Keycloak | `keycloak` | 8080 | OAuth Provider | admin:admin |
| Kafka (OAuth) | `broker` | 9095 | OAuth Kafka API | OAuth required |
| Kafka (Internal) | `broker` | 9093 | Internal/PLAIN | PLAIN auth |
| MDS API | `broker` | 8091 | RBAC Management | OAuth required |
| JMX | `broker` | 9101 | Metrics | - |

## OAuth Configuration

### Keycloak Access
- **URL**: http://localhost:8080
- **Admin**: admin / admin
- **Realm**: cp
- **Client**: client_app1

### OAuth Client Config (`oauth-client.properties`)
```properties
sasl.mechanism=OAUTHBEARER
security.protocol=SASL_PLAINTEXT
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required \
  oauth.client.id="client_app1" \
  oauth.client.secret="client_app1_secret" \
  oauth.token.endpoint.uri="http://keycloak:8080/realms/cp/protocol/openid-connect/token";
```

## Key Features

- ✅ **OAuth 2.0/OIDC**: Full OAuth integration with Keycloak
- ✅ **RBAC**: Fine-grained permissions via Confluent Server
- ✅ **Native Performance**: GraalVM native image benefits
- ✅ **Automated Setup**: Complete RBAC configuration
- ✅ **Production Ready**: Enterprise security patterns
- ✅ **Manual Testing**: Interactive OAuth-enabled containers

## Cleanup

```bash
# Stop all services
docker-compose -f docker-compose-oauth-cp-server-native.yml down

# Remove volumes and networks
docker-compose -f docker-compose-oauth-cp-server-native.yml down -v
```

## Troubleshooting

### OAuth authentication fails
- Check Keycloak is running: `curl http://localhost:8080/health`
- Verify oauth-client.properties file exists and is mounted
- Check broker logs for OAuth errors: `docker logs broker`

### RBAC permissions denied
- Ensure oauth-setup completed successfully: `docker logs oauth-setup`
- Verify token is valid and user has correct permissions
- Check MDS API: `curl http://localhost:8091/v1/metadata/id`

### Services won't start
- Ensure ports 8080, 9095, 8091, 9101 are available
- Check create-certificates.sh script is present and executable
- Verify keycloak-realm-export.json exists

### Producer/Consumer hangs
- OAuth token may have expired (check broker logs)
- Verify network connectivity between containers
- Check oauth-client.properties configuration 
