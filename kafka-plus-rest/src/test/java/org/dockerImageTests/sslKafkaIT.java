package org.dockerImageTests;

//import org.junit.Test;
import io.confluent.common.utils.IntegrationTest;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.dockerImageTests.utils.Admin;
import org.dockerImageTests.utils.Consumer;
import org.dockerImageTests.utils.CustomKafkaContainer;
import org.dockerImageTests.utils.Producer;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.dockerImageTests.kafkaIT;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@Category(IntegrationTest.class)
@Tag("IntegrationTest")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class sslKafkaIT {
    private static final int KAFKA_PORT = 19093;
    private static final int KAFKA_REST_PORT = 8082;
    private static final String IMAGE_NAME = "confluentinc/cp-kafka-kraft";
    private static final String DOCKER_REGISTRY = System.getenv("DOCKER_REGISTRY");

    private static final String DOCKER_TAG = System.getenv("DOCKER_TAG");
    private static final String TOPIC_1 = "test-topic1";
    private static final String TOPIC_2 = "test-topic2";
    Admin admin;
    Consumer consumer;
    Producer producer;


    public GenericContainer container1;;
    @BeforeAll
    public void setup() {
        Yaml yaml = new Yaml();
        InputStream inputStream = getClass().getResourceAsStream("/sslconfigs.yml");
        Map<String,String> env = yaml.load(inputStream);
        String imageName = String.format("%s%s:%s",DOCKER_REGISTRY,IMAGE_NAME,DOCKER_TAG);
       // String imageName = String.format("placeholder/confluentinc/kafka-local:7.4.0-80-ubi8");
        container1=new CustomKafkaContainer(imageName,env)
                .withClasspathResourceMapping("/kafka-1-creds","/etc/kafka/secrets", BindMode.READ_ONLY)
                .withClasspathResourceMapping("/restproxy-creds","/etc/restproxy/secrets",BindMode.READ_ONLY);
        try {
            container1.start();
        }
        catch(Exception  e) {
            System.out.println(container1.getLogs());
        }
     //   Thread.sleep(3600000);
        String baseUrl = String.format("https://%s:%s",container1.getHost(),container1.getMappedPort(KAFKA_REST_PORT));
        String bootstrapUrl = String.format("%s:%s",container1.getHost(),container1.getMappedPort(KAFKA_PORT));
        Properties props = new Properties();
        String path = "src/test/resources/client-creds/kafka.client.truststore.pkcs12";
        File file = new File(path);
        String absolutePath1 = file.getAbsolutePath();
        System.out.println(absolutePath1);
        path = "src/test/resources/client-creds/kafka.client.keystore.pkcs12";
        File file1 = new File(path);
        String absolutePath2 = file1.getAbsolutePath();
        System.out.println(absolutePath2);
        props.put("ssl.keystore.type", "PKCS12");
        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SSL");
       // props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,getClass().getResource("/client-creds/kafka.client.truststore.pkcs12").getPath());
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "confluent");
      //  props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, getClass().getResource("/client-creds/kafka.client.keystore.pkcs12").getPath());
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "confluent");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,absolutePath1);
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, absolutePath2);
        admin =  new Admin(bootstrapUrl,baseUrl,props,true);
        consumer = new Consumer(bootstrapUrl,"test-1",baseUrl,props,true);
        producer = new Producer(baseUrl,bootstrapUrl,props,true);
    }
    @AfterAll
    public void teardown(){
        System.out.println("tearing down");
        container1.stop();
        producer.close();
    }

    @Test
    public void kafkaApiSslTest() throws Exception {
        admin.createTopic(TOPIC_1,3, (short) 1);
        TimeUnit.MILLISECONDS.sleep(100);
        List<String> topics = admin.listTopicsUsingRestApi();
        assertTrue(topics.stream().anyMatch((String.format("\"%s\"",TOPIC_1))::equals));
        producer.send(TOPIC_1,10);
        assertTrue(consumer.consume(10,TOPIC_1));

    }
    @Test
    public void kafkaRestApiSslTest() throws Exception {
        admin.createTopic(TOPIC_2,3, (short) 1);
        TimeUnit.MILLISECONDS.sleep(100);
        List<String> topics = admin.listTopicsUsingRestApi();
        assertTrue(topics.stream().anyMatch((String.format("\"%s\"",TOPIC_2))::equals));
        consumer.subscribeTopicRest(TOPIC_2);
        TimeUnit.MILLISECONDS.sleep(1000);
        producer.sendRest(TOPIC_2,10);
        TimeUnit.MILLISECONDS.sleep(10000);
        assertTrue(consumer.consumeWithRetry(10));

    }
}
