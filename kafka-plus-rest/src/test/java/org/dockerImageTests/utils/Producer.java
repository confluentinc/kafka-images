package org.dockerImageTests.utils;

import okhttp3.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class Producer {

    private KafkaProducer producer;
    private OkHttpClient client;
    private String baseUrl;

    public Producer(Properties props, String baseUrl) {
        this.producer = new KafkaProducer(props);
        this.client = new OkHttpClient();
        this.baseUrl = baseUrl;
    }

    public void send(String topic, int value) throws IOException, ExecutionException, InterruptedException {
        for(int start=0;start<value;start++) {
            ProducerRecord record = new ProducerRecord(topic, Integer.toString(start));
            producer.send(record).get();
        }
    }

    public void sendRest(String topic, int value) throws IOException {
        // Configure request body

        // Configure request URL
        String endpoint = String.format("/topics/%s", topic);
        String url = baseUrl + endpoint;

        // Configure request headers
        MediaType mediaType = MediaType.parse("application/vnd.kafka.json.v2+json");
        Headers headers = new Headers.Builder()
                .add("Content-Type", "application/vnd.kafka.binary.v2+json")
                .build();
        // Send message to topic
        for(int start=0;start<value;start++) {
            String requestBody = String.format("{\"records\":[{\"value\":{\"message\":\" %s \"}}]}",start);
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody, mediaType))
                    .headers(headers)
                    .build();
            Response response = client.newCall(request).execute();
            // Handle response
            if (!response.isSuccessful()) {
                throw new IOException("Failed to send message: " + response.body().string());
            }
        }
    }

    public void close() {
        producer.close();
        client.dispatcher().executorService().shutdown();
    }
}