package org.dockerImageTests;

//import org.junit.Test;
import io.confluent.common.utils.IntegrationTest;
import org.dockerImageTests.utils.Admin;
import org.dockerImageTests.utils.Consumer;
import org.dockerImageTests.utils.CustomKafkaContainer;
import org.dockerImageTests.utils.Producer;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@Category(IntegrationTest.class)
@Tag("IntegrationTest")
@Testcontainers
public class kafkaIT {
   private static final int KAFKA_PORT = 9093;
    private static final int KAFKA_REST_PORT = 8082;

    private static final String TOPIC_1 = "test-topic1";
    private static final String TOPIC_2 = "test-topic2";
   @Container
   public static GenericContainer container1=new CustomKafkaContainer();;
   @BeforeAll
   public static void setup(){
       try {
           container1.start();
       }
       catch(Exception  e) {
           System.out.println(container1.getLogs());
           System.out.println(container1.isRunning());
       }
   }
   @AfterAll
   public static void teardown(){
       System.out.println(container1.isRunning());
       System.out.println(container1.isRunning());
       container1.stop();
       System.out.println(container1.isRunning());
       System.out.println("tearing down");
   }
    @Test
    public void kafkaApiTest() {
	Map<String, String> env = System.getenv();
        Properties props = new Properties();
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        String baseUrl = String.format("http://localhost:%s",container1.getMappedPort(KAFKA_REST_PORT));
        System.out.println(baseUrl);
        System.out.println(KAFKA_PORT);
        System.out.println(container1.getMappedPort(KAFKA_PORT));
        String bootstrapUrl = String.format("localhost:%s",container1.getMappedPort(KAFKA_PORT));
        String bootstrapUrl1 = String.format("localhost:%s","9092");
        System.out.println(bootstrapUrl);
        System.out.println(container1.getHost());
        Admin admin =  new Admin(bootstrapUrl,baseUrl);
        Consumer consumer = new Consumer(bootstrapUrl,"test-1","abc",baseUrl);
        props.put("bootstrap.servers", bootstrapUrl);
        props.put("acks", "all");
        Producer producer = new Producer(props,baseUrl);
        try {
            admin.createTopic(TOPIC_1,3, (short) 1);
            System.out.println(admin.listTopicsUsingKafkaApi());
            TimeUnit.MILLISECONDS.sleep(100);
            System.out.println(admin.listTopicsUsingRestApi());
            List<String> topics = admin.listTopicsUsingRestApi();
            assertTrue(topics.stream().anyMatch((String.format("\"%s\"",TOPIC_1))::equals));
            producer.send(TOPIC_1,10);
            assertTrue(consumer.consume(10,TOPIC_1));
        }
        catch (Exception e){
            System.out.println(e);
            System.out.println(container1.getLogs());
            fail();
        }

        producer.close();
        System.out.println(container1.isRunning());

    }
    @Test
    public void kafkaRestApiTest() {
        Properties props = new Properties();
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        String baseUrl = String.format("http://localhost:%s",container1.getMappedPort(KAFKA_REST_PORT));
        System.out.println(baseUrl);
        System.out.println(KAFKA_PORT);
        System.out.println(container1.getMappedPort(KAFKA_PORT));
        String bootstrapUrl = String.format("localhost:%s",container1.getMappedPort(KAFKA_PORT));
        String bootstrapUrl1 = String.format("localhost:%s","9092");
        System.out.println(bootstrapUrl);
        System.out.println(container1.getHost());
        Admin admin =  new Admin(bootstrapUrl,baseUrl);
        Consumer consumer = new Consumer(bootstrapUrl,"test-1","abc",baseUrl);
        props.put("bootstrap.servers", bootstrapUrl);
        Producer producer = new Producer(props,baseUrl);
        try {
            admin.createTopic(TOPIC_2,3, (short) 1);
            TimeUnit.MILLISECONDS.sleep(100);
            System.out.println(admin.listTopicsUsingRestApi());
            List<String> topics = admin.listTopicsUsingRestApi();
            assertTrue(topics.stream().anyMatch((String.format("\"%s\"",TOPIC_2))::equals));
            producer.sendRest(TOPIC_2,10);
            TimeUnit.MILLISECONDS.sleep(100);
            consumer.subscribeTopicRest(TOPIC_2);
            assertTrue(consumer.consumeRest(10));
        }
        catch (Exception e){
            System.out.println(e);
            fail();
        }

        producer.close();
        System.out.println(container1.isRunning());

    }
}
