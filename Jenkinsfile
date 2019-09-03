#!/usr/bin/env groovy

dockerfile {
    dockerPush = true
    dockerRepos = ['confluentinc/kafka-connect', 'confluentinc/kafka-connect-base',
        'confluentinc/kafka', 'confluentinc/zookeeper']
    mvnPhase = 'package'
    mvnSkipDeploy = true
    //TODO: change node after testing
    nodeLabel = 'docker-oraclejdk8-eli-compose'
    slackChannel = 'tools' //TODO: change to correct team
    upstreamProjects = ['confluentinc/common']
    dockerPullDeps = ['confluentinc/cp-base-new']
    usePackages = true
}
