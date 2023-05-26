package org.dockerImageTests.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Consumer {
    private final String bootstrapServers;
    private final String groupId;
    private final String restEndpoint;

    private final Boolean isSsl;
    Properties props;

    private static final String CONSUMER_GROUP_ID = "dockerTests";
    private static final String CONSUMER_INSTANCE_ID = "instance1";

    public Consumer(String bootstrapServers, String groupId, String restEndpoint, Properties props, Boolean isSsl) {
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.restEndpoint = restEndpoint;
        this.props = props;
        this.isSsl = isSsl;
    }

    public boolean consume(int numMessages, String topicName) throws Exception {
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer < String, String > consumer = new KafkaConsumer < > (props);
        consumer.subscribe(Collections.singletonList(topicName));
        ConsumerRecords < String, String > records = consumer.poll(Duration.ofMillis(100));
        int tries = 0;
        while (records.isEmpty() && tries < 3) {
            TimeUnit.SECONDS.sleep(1);
            records = consumer.poll(Duration.ofMillis(100));
            tries += 1;
        }
        System.out.println("subcscribed to topic" + consumer.subscription());
        for (ConsumerRecord < String, String > record: records) {
            handleMessage(record.value());
            numMessages--;
        }
        System.out.println("records === " + records.count());
        consumer.commitSync();
        if (numMessages > 0) {
            return false;
        }
        return true;

    }
    private void createConsumerInstance() throws Exception {
        String url = restEndpoint + "/consumers/" + CONSUMER_GROUP_ID;
        String requestBody = "{\"name\":\"" + CONSUMER_INSTANCE_ID + "\",\"format\":\"binary\",\"auto.offset.reset\":\"earliest\"}";
        // Kafka REST API URL
        HttpPost request = new HttpPost(url);

        // Create HTTP POST request
        HttpClient httpClient = HttpClientBuilder.create().build();
        if (isSsl == true) {
            String truststoreFile = "/client-creds/client-truststore.jks";
            InputStream truststoreStream = getClass().getResourceAsStream(truststoreFile);
            KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
            truststore.load(truststoreStream, "confluent".toCharArray());

            // Build SSL context
            SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(truststore, new TrustSelfSignedStrategy())
                .build();

            // Create SSL connection socket factory
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            // Create HTTP client
            httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslSocketFactory)
                .build();

        }
        request.addHeader("Content-Type", "application/vnd.kafka.v2+json");
        request.setEntity(new StringEntity(requestBody));

        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new RuntimeException("Failed to create Kafka consumer instance. Response code: " + statusCode);
        }
        System.out.println("successfully created the consumer instance");
    }
    public boolean subscribeTopicRest(String topicName) throws Exception {
        try {
            createConsumerInstance();
            HttpClient httpClient = HttpClientBuilder.create().build();
            if (isSsl == true) {
                String truststoreFile = "/client-creds/client-truststore.jks";
                InputStream truststoreStream = getClass().getResourceAsStream(truststoreFile);
                KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
                truststore.load(truststoreStream, "confluent".toCharArray());

                // Build SSL context
                SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(truststore, new TrustSelfSignedStrategy())
                    .build();

                // Create SSL connection socket factory
                SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

                // Create HTTP client
                httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();

            }

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
                return false;
            }
        } catch (IOException e) {
            System.out.println("Error subscribing to Kafka topic: " + e.getMessage());
            return false;
        }
    }
    public boolean consumeRest(int numMessages) throws Exception {

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            if (isSsl == true) {
                String truststoreFile = "/client-creds/client-truststore.jks";
                InputStream truststoreStream = getClass().getResourceAsStream(truststoreFile);
                KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
                truststore.load(truststoreStream, "confluent".toCharArray());

                // Build SSL context
                SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(truststore, new TrustSelfSignedStrategy())
                    .build();

                // Create SSL connection socket factory
                SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

                // Create HTTP client
                httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();

            }
            String fetchUrl = restEndpoint + "/consumers/" + CONSUMER_GROUP_ID + "/instances/" + CONSUMER_INSTANCE_ID + "/records?timeout=10000";
            HttpGet fetchRequest = new HttpGet(fetchUrl);
            HttpResponse fetchResponse = httpClient.execute(fetchRequest);
            HttpEntity fetchEntity = fetchResponse.getEntity();
            while (fetchEntity == null) {
                fetchResponse = httpClient.execute(fetchRequest);
                fetchEntity = fetchResponse.getEntity();
            }
            if (fetchEntity != null) {
                JSONArray jsonArray = parseJson(fetchEntity);

                for (int i = 0; i < jsonArray.length(); i++) {
                    numMessages--;
                }
                if (numMessages > 0) {
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

    @NotNull
    public Boolean consumeWithRetry(int numMessages) throws Exception {
        Boolean isConsumeSuccessful = false;
        int tries = 0;
        isConsumeSuccessful = consumeRest(numMessages);
        while (isConsumeSuccessful == false && tries < 3) {
            TimeUnit.SECONDS.sleep(1);
            isConsumeSuccessful = consumeRest(numMessages);
            tries += 1;
        }
        return isConsumeSuccessful;
    }

    @NotNull
    private static JSONArray parseJson(HttpEntity fetchEntity) throws IOException {
        String messageJson = EntityUtils.toString(fetchEntity, StandardCharsets.UTF_8);
        System.out.println(messageJson);
        JSONArray jsonArray = new JSONArray(messageJson);
        return jsonArray;
    }

    private void handleMessage(String message) {
        System.out.println("Received message: " + message);
    }
}
