package org.dockerImageTests.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.JSONArray;

public class Consumer {
    private final String bootstrapServers;
    private final String groupId;
    private final String topic;
    private final String restEndpoint;

    private static final String CONSUMER_GROUP_ID = "dockerTests";
    private static final String CONSUMER_INSTANCE_ID = "instance1";

    public Consumer(String bootstrapServers, String groupId, String topic, String restEndpoint) {
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.topic = topic;
        this.restEndpoint = restEndpoint;
    }

    public boolean consume(int numMessages,String topicName) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topicName));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        int retry=0;
        while(records.count()==0){
            System.out.println("Polling again");
            records = consumer.poll(Duration.ofMillis(100));
        }
        System.out.println("subcscribed to topic" + consumer.subscription());
        for (ConsumerRecord<String, String> record : records) {
            handleMessage(record.value());
            numMessages--;
        }
        System.out.println("records === " + records.count());
        consumer.commitSync();
        if (numMessages>0){
            return false;
        }
        return true;

    }
    private  void createConsumerInstance() throws IOException {
        String url = restEndpoint + "/consumers/" + CONSUMER_GROUP_ID;
        String requestBody = "{\"name\":\"" + CONSUMER_INSTANCE_ID + "\",\"format\":\"binary\",\"auto.offset.reset\":\"earliest\"}";

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", "application/vnd.kafka.v2+json");
        request.setEntity(new StringEntity(requestBody));

        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new RuntimeException("Failed to create Kafka consumer instance. Response code: " + statusCode);
        }
        System.out.println("successfully created the consumer instance");
    }

    public boolean subscribeTopicRest(String topicName){
        try {
            createConsumerInstance();
            CloseableHttpClient httpClient = HttpClients.createDefault();
            String subscriptionUrl = restEndpoint + "/consumers/" + CONSUMER_GROUP_ID + "/instances/" + CONSUMER_INSTANCE_ID + "/subscription";
            HttpPost subscriptionRequest = new HttpPost(subscriptionUrl);
            String subscriptionJson = "{ \"topics\": [ \"" + topicName + "\" ] }";
            StringEntity subscriptionEntity = new StringEntity(subscriptionJson, ContentType.APPLICATION_JSON);
            subscriptionRequest.setEntity(subscriptionEntity);
            subscriptionRequest.addHeader("Content-Type", "application/vnd.kafka.json.v2+json");
            HttpResponse subscriptionResponse = httpClient.execute(subscriptionRequest);
            if (subscriptionResponse.getStatusLine().getStatusCode() == 204) {
                System.out.println("Subscribed to Kafka topic: " + topicName);
                return true;
            } else {
                System.out.println("Failed to subscribe to Kafka topic. Response code: " + subscriptionResponse.getStatusLine().getStatusCode());
               return false;}
        } catch (IOException e) {
            System.out.println("Error subscribing to Kafka topic: " + e.getMessage());
            return false;
        }
    }
    public boolean consumeRest(int numMessages) throws Exception {

        // Continuously fetch messages from the Kafka topic
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            String fetchUrl = restEndpoint + "/consumers/" + CONSUMER_GROUP_ID + "/instances/" + CONSUMER_INSTANCE_ID + "/records?timeout=10000";
            HttpGet fetchRequest = new HttpGet(fetchUrl);
            HttpResponse fetchResponse = httpClient.execute(fetchRequest);
            HttpEntity fetchEntity = fetchResponse.getEntity();
            while(fetchEntity==null){
                fetchResponse = httpClient.execute(fetchRequest);
                fetchEntity = fetchResponse.getEntity();
            }
            if (fetchEntity != null) {
                String messageJson = EntityUtils.toString(fetchEntity, StandardCharsets.UTF_8);
                System.out.println(messageJson);
                JSONArray jsonArray = new JSONArray(messageJson);

                for (int i = 0; i < jsonArray.length(); i++) {
                    numMessages--;
                }
                if (numMessages>0){
                    System.out.println("numMessages = " + numMessages);
                    return false;
                }
            }
        } catch (IOException e) {
            System.out.println("Error fetching messages from Kafka topic: " + e.getMessage());
            return false;
        }
        return true;
    }

    private void handleMessage(String message) {
        System.out.println("Received message: " + message);
    }
}
