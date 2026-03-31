# Kafka JVM vs Native Image Benchmark Report

**Date:** Fri Aug  1 18:13:01 IST 2025
**Architecture:** arm64
**Iterations:** 5

## Images Tested

- **JVM:** `519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/prod/confluentinc/cp-server:8.0.x-6701-ubi9`
- **Native:** `519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/dev/confluentinc/cp-server-native:dev-8.0.x-ba754285-ubi9.arm64`

## Results Summary

### Startup Time
- **JVM:** Mean: 8.594s, Min: 7.999s, Max: 10.587s
- **Native:** Mean: 1.573s, Min: 1.540s, Max: 1.603s

### Memory Usage
- **JVM:** Mean: 648.680MB, Min: 602.500MB, Max: 712.700MB
- **Native:** Mean: 287.180MB, Min: 286.900MB, Max: 287.400MB

## Raw Data

### JVM Startup Times (seconds)
```
10.586584000
8.201001000
8.132294000
8.051808000
7.998810000
```

### Native Startup Times (seconds)
```
1.576951000
1.595573000
1.602526000
1.539796000
1.552288000
```

### JVM Memory Usage (MB)
```
602.5
642.5
638.2
712.7
647.5
```

### Native Memory Usage (MB)
```
287.4
286.9
287.4
287.3
286.9
```

### Image Sizes
```
JVM Image Size: 2443.31196212768554687500 MB
Native Image Size: 607.09318065643310546875 MB
Size Reduction: 75.15285849426431544000%
```

## Methodology

1. Each image was tested 5 times
2. Startup time measured from `docker-compose up` to service readiness
3. Service readiness confirmed by "Kafka Server started" log message
4. Functional test validates complete Kafka functionality using separate client container:
   - API connectivity (topic listing)
   - Topic creation/deletion
   - Message production
   - Message consumption
5. Memory measured after service startup and functional test
6. Containers fully cleaned between runs

