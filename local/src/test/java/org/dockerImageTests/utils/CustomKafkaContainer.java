package org.dockerImageTests.utils;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomKafkaContainer extends GenericContainer<CustomKafkaContainer> {
    private static final int KAFKA_PORT = 9093;
    private static final int KAFKA_REST_PORT = 8082;

    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

    private static final String DOCKER_REGISTRY = System.getenv("DOCKER_REGISTRY");

    private static final String DOCKER_TAG = System.getenv("DOCKER_TAG");
    public CustomKafkaContainer() {
        super(DockerImageName.parse(String.format("%s:%s",DOCKER_REGISTRY,DOCKER_TAG)));
        System.out.println("Using image " + String.format("%s:%s",DOCKER_REGISTRY,DOCKER_TAG));
        Map<String,String> env = new HashMap<String,String >();
        env.put("KAFKA_NODE_ID","1");
        env.put( "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT,HOST:PLAINTEXT");
        env.put("KAFKA_ADVERTISED_LISTENERS","PLAINTEXT://broker:29092,PLAINTEXT_HOST://localhost:9092");
        env.put("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        env.put("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
        env.put("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1");
        env.put("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1");
        env.put("KAFKA_PROCESS_ROLES", "broker,controller");
        env.put("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@broker:29093");
        env.put("KAFKA_LISTENERS", "PLAINTEXT://broker:29092,CONTROLLER://broker:29093,PLAINTEXT_HOST://0.0.0.0:9092,HOST://0.0.0.0:9093");
        env.put("KAFKA_INTER_BROKER_LISTENER_NAME", "PLAINTEXT");
        env.put("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER");
        env.put("KAFKA_LOG_DIRS", "/tmp/kraft-combined-logs");
        env.put("KAFKA_REST_HOST_NAME", "rest-proxy");
        env.put("KAFKA_REST_BOOTSTRAP_SERVERS", "broker:29092");
        env.put("KAFKA_REST_LISTENERS", "http://0.0.0.0:8082");
        env.put("CLUSTER_ID", "4L6g3nShT-eMCtK--X86sw");
        env.put("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");
        // set up container ports and environment variables
        Network network = Network.newNetwork();
         withNetwork(network) ;
        withNetworkAliases("broker");
        withImagePullPolicy(PullPolicy.defaultPolicy());
        withEnv(env);
        withExposedPorts(KAFKA_REST_PORT,KAFKA_PORT);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint("sh");
        });
        withCommand("-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);

    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        // Customize the container behavior before it starts
        Integer mappedPort = getMappedPort(KAFKA_PORT);

        // use the mapped port to configure the application
        String url = "HOST://localhost:" + mappedPort;
        withEnv("KAFKA_ADVERTISED_LISTENERS",String.format("PLAINTEXT://broker:29092,PLAINTEXT_HOST://%s",url));

        String command = "#!/bin/bash\n";
        // exporting KAFKA_ADVERTISED_LISTENERS with the container hostname
        command +=
                String.format(
                        "export KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://broker:29092,PLAINTEXT_HOST://localhost:9092,%s\n",
                        url
                );

        command += "sed -i '/KAFKA_ZOOKEEPER_CONNECT/d' /etc/confluent/docker/configure\n";
        String clusterId = "";
        try {
                clusterId = execInContainer("kafka-storage", "random-uuid").getStdout().trim();
        } catch (IOException | InterruptedException e) {
            logger().error("Failed to execute `kafka-storage random-uuid`. Exception message: {}", e.getMessage());
        }
        command +=
                "echo 'kafka-storage format --ignore-formatted -t \"" +
                        clusterId +
                        "\" -c /etc/kafka/kafka.properties' >> /etc/confluent/docker/configure\n";
        command += "echo '' > /etc/confluent/docker/ensure \n";
        // Run the original command
        command += "/etc/confluent/docker/run \n";
        copyFileToContainer(Transferable.of(command, 0777), STARTER_SCRIPT);
    }
}

