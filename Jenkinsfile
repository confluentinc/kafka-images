#!/usr/bin/env groovy

dockerfile {
    dockerPush = true
    dockerRepos = ['confluentinc/cp-server-connect', 'confluentinc/cp-server-connect-base',
        'confluentinc/cp-kafka-connect', 'confluentinc/cp-kafka-connect-base',
        'confluentinc/cp-enterprise-kafka', 'confluentinc/cp-kafka',
        'confluentinc/cp-server', 'confluentinc/cp-zookeeper']
    mvnPhase = 'package integration-test'
    mvnSkipDeploy = true
    nodeLabel = 'docker-oraclejdk8-compose-swarm'
    slackChannel = 'kafka-warn'
    upstreamProjects = []
    dockerPullDeps = ['confluentinc/cp-base-new', 'confluentinc/cp-jmxterm', 'confluentinc/cp-kerberos']
    usePackages = true
    cron = '' // Disable the cron because this job requires parameters
}
