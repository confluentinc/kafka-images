# Kafka JVM vs Native Image Benchmark Report

**Date:** Sun Jul 27 10:37:42 IST 2025
**Architecture:** arm64
**Iterations:** 5

## Images Tested

- **JVM:** `519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/prod/confluentinc/cp-server:8.0.x-latest-ubi9`
- **Native:** `519856050701.dkr.ecr.us-west-2.amazonaws.com/docker/dev/confluentinc/cp-server-native:dev-8.0.x-7842c5ec-ubi9.arm64`

## Results Summary

### Startup Time
- **JVM:** Mean: 12.84s, Min: 9.78s, Max: 15.61s
- **Native:** Mean: 2.54s, Min: 1.89s, Max: 3.00s

### Memory Usage
- **JVM:** Mean: 662.30MB, Min: 575.30MB, Max: 757.80MB
- **Native:** Mean: 285.22MB, Min: 280.80MB, Max: 289.70MB

## Raw Data

### JVM Startup Times (seconds)
```
15.613874000
12.781309000
14.595273000
9.778474000
11.450065000
```

### Native Startup Times (seconds)
```
2.938014000
2.999358000
2.989427000
1.900864000
1.886806000
```

### JVM Memory Usage (MB)
```
575.3
757.8
616.6
742.8
619
```

### Native Memory Usage (MB)
```
283.3
280.8
286.1
286.2
289.7
```

### Image Sizes
```
JVM Image Size: 2400.1 MB
Native Image Size: 726.7 MB
Size Reduction: 70.0%
```

## Methodology

1. Each image was tested 5 times
2. Startup time measured from `docker-compose up` to service readiness
3. Service readiness confirmed by successful `kafka-topics --list` command
4. Memory measured after service startup and functional test
5. Containers fully cleaned between runs
6. Functional test performed (topic creation) to ensure service health

