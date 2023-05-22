package org.dockerImageTests.utils;

import okhttp3.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class Producer {

    private KafkaProducer producer;
    private OkHttpClient client;
    private String baseUrl;

    private Boolean isSsl;

    public Producer(String baseUrl, String bootstrapUrl,Properties props,Boolean isSsl) {
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("bootstrap.servers", bootstrapUrl);
        props.put("acks", "all");
        this.producer = new KafkaProducer(props);
        this.client = new OkHttpClient();
        this.baseUrl = baseUrl;
        this.isSsl = isSsl;
    }

    public void send(String topic, int value) throws IOException, ExecutionException, InterruptedException {
        for(int start=0;start<value;start++) {
            ProducerRecord record = new ProducerRecord(topic, Integer.toString(start));
            producer.send(record).get();
        }
    }

    public void sendRest(String topic, int value) throws Exception {
        // Configure request body

        // Configure request URL
        String endpoint = String.format("/topics/%s", topic);
        String url = baseUrl + endpoint;
        OkHttpClient client = new OkHttpClient.Builder().build();
        if (isSsl == true) {
            String truststoreFile = "/client-creds/client-truststore.jks";
            InputStream truststoreStream = getClass().getResourceAsStream(truststoreFile);
            KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
            truststore.load(truststoreStream, "confluent".toCharArray());

            // Build SSL context
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(truststore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());

            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Create HTTP client
            client = new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManagers[0])
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            // Allow all hostnames
                            return true;
                        }
                    })
                    .build();

        }
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