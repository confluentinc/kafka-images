#!/usr/bin/env groovy

dockerfile {
    dockerPullDeps = ['confluentinc/cp-base']
    dockerRegistry = '368821881613.dkr.ecr.us-west-2.amazonaws.com/'
    dockerRepos = ['confluentinc/cp-kafkacat', 'confluentinc/cp-kerberos', 'confluentinc/cp-jmxterm', 'confluentinc/cp-kafka', 'confluentinc/cp-enterprise-kafka', 'confluentinc/cp-zookeeper']
    mvnPhase = 'integration-test'
    nodeLabel = 'docker-oraclejdk8-compose'
    slackChannel = ''
    properties = [
        buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '7', numToKeepStr: '7')),
        parameters([string(defaultValue: 'latest', description: 'BUILD_NUMBER from upstream packaging job', name: 'PACKAGING_BUILD_NUMBER')])
    ]
}
