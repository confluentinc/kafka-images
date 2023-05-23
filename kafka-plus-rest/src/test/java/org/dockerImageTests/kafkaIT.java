package org.dockerImageTests;

//import org.junit.Test;
import io.confluent.common.utils.IntegrationTest;
import org.dockerImageTests.utils.Admin;
import org.dockerImageTests.utils.Consumer;
import org.dockerImageTests.utils.CustomKafkaContainer;
import org.dockerImageTests.utils.Producer;
import org.jetbrains.annotations.NotNull;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.*;

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


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@Category(IntegrationTest.class)
@Tag("IntegrationTest")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class kafkaIT {
   private static final int KAFKA_PORT = 19092;
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
   public void setup(){
       Yaml yaml = new Yaml();
       InputStream inputStream = getClass().getResourceAsStream("/sslconfigs.yml");
       Map<String,String> env = yaml.load(inputStream);
       env.put("KAFKA_REST_LISTENERS","http://0.0.0.0:8082");
       env.put("KAFKA_REST_BOOTSTRAP_SERVERS","BROKER://kafka-1:9092");
       env.remove("KAFKA_REST_CLIENT_SECURITY_PROTOCOL");
       String imageName = String.format("%s%s:%s",DOCKER_REGISTRY,IMAGE_NAME,DOCKER_TAG);
       //String imageName = String.format("placeholder/confluentinc/kafka-local:7.4.0-80-ubi8");
       container1=new CustomKafkaContainer(imageName,env)
               .withClasspathResourceMapping("/kafka-1-creds","/etc/kafka/secrets", BindMode.READ_ONLY)
               .withClasspathResourceMapping("/restproxy-creds","/etc/restproxy/secrets",BindMode.READ_ONLY);;
       try {
           container1.start();
       }
       catch(Exception  e) {
           System.out.println(container1.getLogs());
       }
       String baseUrl = String.format("http://%s:%s",container1.getHost(),container1.getMappedPort(KAFKA_REST_PORT));
       String bootstrapUrl = String.format("%s:%s",container1.getHost(),container1.getMappedPort(KAFKA_PORT));
       Properties props = new Properties();
       admin =  new Admin(bootstrapUrl,baseUrl,props,false);
       consumer = new Consumer(bootstrapUrl,"test-1",baseUrl,props,false);
       producer = new Producer(baseUrl,bootstrapUrl,props,false);
   }
   @AfterAll
   public void teardown(){
       System.out.println("tearing down");
       System.out.println(container1.getLogs());
       container1.stop();
       System.out.println(container1.isRunning());
       producer.close();
   }
    @Test
    public void kafkaApiTest() throws Exception {
    admin.createTopic(TOPIC_1,3, (short) 1);
   // TimeUnit.MILLISECONDS.sleep(100);
    List<String> topics = admin.listTopicsUsingRestApi();
    assertTrue(topics.stream().anyMatch((String.format("\"%s\"",TOPIC_1))::equals));
    producer.send(TOPIC_1,10);
    assertTrue(consumer.consume(10,TOPIC_1));

    }
    @Test
    public void kafkaRestApiTest() throws Exception {
       admin.createTopic(TOPIC_2,3, (short) 1);
    //   TimeUnit.MILLISECONDS.sleep(100);
       List<String> topics = admin.listTopicsUsingRestApi();
       assertTrue(topics.stream().anyMatch((String.format("\"%s\"",TOPIC_2))::equals));
       consumer.subscribeTopicRest(TOPIC_2);
   //    TimeUnit.MILLISECONDS.sleep(1000);
       producer.sendRest(TOPIC_2,10);
        assertTrue(consumer.consumeWithRetry(10));

    }

}
