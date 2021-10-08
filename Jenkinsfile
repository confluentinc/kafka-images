#!/usr/bin/env groovy

dockerfile {
    dockerPush = true
    dockerRepos = ['confluentinc/cp-server-connect', 'confluentinc/cp-server-connect-base',
        'confluentinc/cp-kafka-connect', 'confluentinc/cp-kafka-connect-base',
        'confluentinc/cp-enterprise-kafka', 'confluentinc/cp-kafka',
        'confluentinc/cp-server', 'confluentinc/cp-zookeeper']
    mvnPhase = 'package'
    mvnSkipDeploy = true
    nodeLabel = 'docker-debian-jdk8-compose'
    slackChannel = 'kafka-warn'
    upstreamProjects = []
    dockerPullDeps = ['confluentinc/cp-base-new']
    usePackages = true
    cron = '' // Disable the cron because this job requires parameters
    cpImages = true
    osTypes = ['ubi8']
    nanoVersion = true
}
