package org.dockerImageTests.utils;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CustomKafkaContainer extends GenericContainer<CustomKafkaContainer> {
    private static final int KAFKA_PORT = 9093;
    private static final int KAFKA_PLAIN_PORT = 19092;
    private static final int KAFKA_SSL_PORT = 19093;
    private static final int KAFKA_REST_PORT = 8082;

    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

    public CustomKafkaContainer(String image, Map<String,String> env) {
        super(DockerImageName.parse(image));
        // set up container ports and environment variables
        Network network = Network.newNetwork();
         withNetwork(network) ;
        withNetworkAliases("kafka-1");
        withImagePullPolicy(PullPolicy.defaultPolicy());
        withEnv(env);
        withExposedPorts(KAFKA_REST_PORT,KAFKA_PORT,KAFKA_PLAIN_PORT,KAFKA_SSL_PORT);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint("sh");
        });
        withCommand("-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);

    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        // Customize the container behavior before it starts
        Integer mappedOpenPort = getMappedPort(KAFKA_PLAIN_PORT);
        Integer mappedSslPort = getMappedPort(KAFKA_SSL_PORT);

        // use the mapped port to configure the application
        String url = String.format("PLAINTEXT://%s:%s,SSL://%s:%s",getHost(),mappedOpenPort,getHost(),mappedSslPort);
        System.out.println(url);
        String command = "#!/bin/bash\n";
        // exporting KAFKA_ADVERTISED_LISTENERS with the container hostname
        command +=
                String.format(
                        "export KAFKA_ADVERTISED_LISTENERS=%s,SSL-INT://kafka-1:9093,BROKER://kafka-1:9092\n",
                        url
                );

        command += "/etc/confluent/docker/run \n";
        copyFileToContainer(Transferable.of(command, 0777), STARTER_SCRIPT);
    }
}

