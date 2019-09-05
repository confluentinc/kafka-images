#!/usr/bin/env groovy

dockerfile {
    dockerPush = true
    dockerRepos = ['confluentinc/kafka-connect', 'confluentinc/kafka-connect-base',
        'confluentinc/kafka', 'confluentinc/zookeeper']
    mvnPhase = 'package'
    mvnSkipDeploy = true
    nodeLabel = 'docker-oraclejdk8-eli-compose'
    slackChannel = 'tools' //TODO: change to correct team
    upstreamProjects = [] //TODO: after roll out, this will be the packaging job
    dockerPullDeps = ['confluentinc/cp-base-new']
    usePackages = true
}
