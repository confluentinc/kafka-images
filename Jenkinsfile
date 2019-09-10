#!/usr/bin/env groovy

dockerfile {
    dockerPush = true
    dockerRepos = ['confluentinc/cp-kafka-connect', 'confluentinc/cp-kafka-connect-base',
        'confluentinc/cp-kafka', 'confluentinc/cp-zookeeper']
    mvnPhase = 'package'
    mvnSkipDeploy = true
    nodeLabel = 'docker-oraclejdk8-compose-swarm'
    slackChannel = 'tools-notifications' //TODO: change to correct team
    upstreamProjects = [] //TODO: after roll out update
    dockerPullDeps = ['confluentinc/cp-base-new']
    usePackages = true
}
