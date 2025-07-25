# Confluent Server Native Docker Examples

This directory contains comprehensive Docker Compose examples demonstrating various Confluent Server configurations with cp-server GraalVM native image. Each example includes automated testing and interactive manual testing capabilities.

## 🚀 Available Scenarios

| Scenario | File | Focus | Authentication | Key Features |
|----------|------|-------|---------------|--------------|
| **[Basic](#basic-kafka-native)** | `docker-compose-basic-cp-server-native.yml` | Simple Kafka | None | Fast startup, basic topics |
| **[MDS File Store](#mds-file-store--sasl)** | `docker-compose-mds-file-store.yml` | SASL Auth | SASL/PLAIN | File-based users, MDS API |
| **[Embedded REST Proxy](#embedded-rest-proxy-erp)** | `docker-compose-erp-cp-server-native.yml` | REST API | None | REST API v3, JSON messaging |
| **[OAuth + RBAC](#oauth--rbac)** | `docker-compose-oauth-cp-server-native.yml` | Enterprise Security | OAuth/OIDC | Keycloak, RBAC permissions |

## 📋 Quick Start Guide

Each scenario follows the same pattern:

```bash
# 1. Start services
docker-compose -f <compose-file> up -d

# 2. Watch automated testing
docker logs -f producer
docker logs -f consumer

# 3. Manual testing
docker exec -it broker bash      # Kafka management
docker exec -it producer bash    # Send messages
docker exec -it consumer bash    # Receive messages
```

## 🏗️ Architecture Overview

All scenarios use consistent container names and similar architectures:

```
┌──────────┐    ┌─────────┐    ┌──────────┐
│ producer │    │ broker  │    │ consumer │
│          ├───►│         │───►│          │
│          │    │         │    │          │
└──────────┘    └─────────┘    └──────────┘
```

## 📖 Detailed Documentation

### Basic Kafka Native
📄 **[README-basic.md](README-basic.md)**

The simplest setup for getting started with GraalVM native Kafka.

**Features:**
- ✅ GraalVM native image (fast startup)
- ✅ Single partition for guaranteed message ordering
- ✅ 3 test messages with automated verification
- ✅ Interactive containers for experimentation

**Use Cases:**
- Learning Kafka basics
- Development environments
- Performance testing native images

```bash
docker-compose -f docker-compose-basic-cp-server-native.yml up -d
```

### MDS File Store + SASL
📄 **[README-mds.md](README-mds.md)**

Demonstrates authentication and Metadata Server capabilities.

**Features:**
- ✅ SASL/PLAIN authentication (alice:alice-secret)
- ✅ File-based user management
- ✅ MDS API endpoints (:8090)
- ✅ 5 authenticated test messages
- ✅ Foundation for RBAC

**Use Cases:**
- Secure Kafka environments
- User authentication
- MDS API exploration
- RBAC preparation

```bash
docker-compose -f docker-compose-mds-file-store.yml up -d
```

### Embedded REST Proxy (ERP)
📄 **[README-erp.md](README-erp.md)**

Shows REST API v3 integration for HTTP-based Kafka operations.

**Features:**
- ✅ Embedded REST Proxy (:28090)
- ✅ REST API v3 endpoints
- ✅ JSON message production via HTTP
- ✅ Cross-protocol testing (REST → Console)
- ✅ Cluster management via REST

**Use Cases:**
- Web application integration
- HTTP-based messaging
- Microservices communication
- REST API development

```bash
docker-compose -f docker-compose-erp-cp-server-native.yml up -d
```

### OAuth + RBAC
📄 **[README-oauth.md](README-oauth.md)**

Enterprise-grade security with OAuth and Role-Based Access Control.

**Features:**
- ✅ Keycloak OAuth provider (:8080)
- ✅ OAuth/OIDC authentication
- ✅ RBAC permissions via MDS
- ✅ Automated permission setup
- ✅ Secure message flow

**Use Cases:**
- Enterprise deployments
- OAuth integration
- Fine-grained permissions
- Production security

```bash
docker-compose -f docker-compose-oauth-cp-server-native.yml up -d
```

## 🔧 Common Commands

### Container Access
```bash
# Access any container for manual testing
docker exec -it broker bash      # Kafka broker
docker exec -it producer bash    # Producer tools
docker exec -it consumer bash    # Consumer tools
```

### Monitoring
```bash
# Watch logs in real-time
docker logs -f <container-name>

# Check container status
docker-compose -f <compose-file> ps

# View resource usage
docker stats
```

### Cleanup
```bash
# Stop services
docker-compose -f <compose-file> down

# Remove volumes too
docker-compose -f <compose-file> down -v

# Clean up Docker system
docker system prune -f
```

## 🎯 Use Case Guide

### Choose **Basic** if you want:
- Quick Kafka testing
- Learning Kafka fundamentals
- Performance benchmarking
- Simple development setup

### Choose **MDS** if you want:
- User authentication
- Secure Kafka access
- MDS API exploration
- Foundation for RBAC

### Choose **ERP** if you want:
- REST API integration
- HTTP-based messaging
- Web application connectivity
- Cross-protocol testing

### Choose **OAuth** if you want:
- Enterprise security
- OAuth/OIDC integration
- Fine-grained permissions
- Production deployment patterns

## 🛠️ Prerequisites

### System Requirements
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **Memory**: 4GB+ available
- **Ports**: Various (see individual READMEs)

### Architecture Support
- **x86_64**: Fully supported
- **ARM64/M1**: Supported (may require image updates)

## 🔍 Troubleshooting

### Common Issues

**Port conflicts:**
```bash
# Check what's using ports
netstat -an | grep -E "(9092|8080|8090|28090)"

# Stop conflicting services
sudo lsof -ti:9092 | xargs kill
```

**Memory issues:**
```bash
# Increase Docker memory to 4GB+
# Docker Desktop: Settings → Resources → Memory

# Check available memory
docker system df
```

**Container startup problems:**
```bash
# Check logs for specific errors
docker logs <container-name>

# Restart specific container
docker-compose -f <compose-file> restart <service-name>
```

### Getting Help

1. Check the specific scenario README
2. Look at container logs: `docker logs <container>`
3. Verify prerequisites are met
4. Check port availability
5. Ensure sufficient memory allocation

## 📚 Additional Resources

- [Confluent Documentation](https://docs.confluent.io/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [GraalVM Native Image](https://www.graalvm.org/native-image/)
- [Docker Compose Reference](https://docs.docker.com/compose/)

---

**Next Steps:** Choose a scenario above and follow its detailed README for specific instructions! 
