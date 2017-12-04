#!/usr/bin/env groovy

docker_oraclejdk8 {
    dockerPullDeps = ['confluentinc/cp-base']
    dockerPush = true
    dockerRegistry = '368821881613.dkr.ecr.us-west-2.amazonaws.com/'
    dockerRepos = ['confluentinc/cp-kerberos', 'confluentinc/cp-jmxterm', 'confluentinc/cp-kafka', 'confluentinc/cp-enterprise-kafka', 'confluentinc/cp-zookeeper']
    dockerUpstreamRegistry = '368821881613.dkr.ecr.us-west-2.amazonaws.com/'
    dockerUpstreamTag = '4.0.x-latest'
    mvnPhase = 'package'
    slackChannel = ''
    properties = [
        buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '7', numToKeepStr: '7')),
        parameters([string(defaultValue: 'latest', description: 'BUILD_NUMBER from upstream packaging job', name: 'PACKAGING_BUILD_NUMBER'),
                    string(defaultValue: 'unknown', description: 'KAFKA_COMMIT from upstream packaging job', name: 'KAFKA_COMMIT')])
    ]
}
