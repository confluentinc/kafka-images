package org.dockerImageTests.utils;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.TopicConfig;

import javax.net.ssl.SSLContext;

public class Admin {
    private final String bootstrapServers;
    private final String restEndpoint;
    AdminClient adminClient;

    private final Boolean isSsl;
    public Admin(String bootstrapServers, String restEndpoint,Properties props,Boolean isSsl) {
        this.bootstrapServers = bootstrapServers;
        this.restEndpoint = restEndpoint;
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        adminClient = AdminClient.create(props);
        this.isSsl = isSsl;

    }

    public List<String> listTopicsUsingKafkaApi() throws Exception {
        ListTopicsResult topics = adminClient.listTopics();
        return new ArrayList<>(topics.names().get());
    }

    public void createTopic(String topicName, int numPartitions, short replicationFactor) throws Exception {
        Map<String, String> configs = new HashMap<>();
        configs.put(TopicConfig.RETENTION_MS_CONFIG, "86400000");

        NewTopic newTopic = new NewTopic(topicName, numPartitions, replicationFactor).configs(configs);

        CreateTopicsResult result = adminClient.createTopics(Collections.singleton(newTopic));

        result.values().get(topicName).get();
    }

    public void createTopicRest(String topicName, int numPartitions, short replicationFactor) throws Exception {
        String url = restEndpoint + "/topics/";

        String requestBody = String.format(
                "{\"topic_name\" : \"%s\",\"partitions_count\": %d,\"replication-factor\": %d}",
                topicName,
                numPartitions,
                replicationFactor
        );


        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost postRequest = new HttpPost(url);
        postRequest.addHeader("Content-Type", "application/vnd.kafka.binary.v2+json");
        StringEntity requestEntity = new StringEntity(requestBody);

        postRequest.setEntity(requestEntity);
        HttpResponse response = httpClient.execute(postRequest);
        HttpEntity responseBody = response.getEntity();
        String responseString = "";
        if (responseBody != null) {
            InputStream instream = responseBody.getContent();
            responseString = EntityUtils.toString(responseBody);
        }
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("Failed to create topic: " + response.toString());
        }
    }



    public List<String> listTopicsUsingRestApi() throws Exception {
        String endpoint = restEndpoint + "/topics";
        HttpClient httpClient = HttpClientBuilder.create().build();;
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
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);

            //   Create HTTP client
            httpClient = HttpClients.custom()
                    .setSSLSocketFactory(sslSocketFactory)
                    .build();
        }

        HttpGet getRequest = new HttpGet(endpoint);
        HttpResponse response = httpClient.execute(getRequest);
        HttpEntity responseBody = response.getEntity();
        String responseString = "";
        if (responseBody != null) {
            InputStream instream = responseBody.getContent();
            responseString = EntityUtils.toString(responseBody);
        }

        return Arrays.asList(responseString.replaceAll("\\[|\\]", "").split(","));
    }


    public void deleteTopicUsingKafkaApi(String topicName) throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        AdminClient adminClient = AdminClient.create(props);
        DeleteTopicsResult result = adminClient.deleteTopics(Arrays.asList(topicName));
        result.all().get();
    }

    public void deleteTopicUsingRestApi(String topicName) throws Exception {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpDelete deleteRequest = new HttpDelete(restEndpoint + "/" + topicName);
        HttpResponse response = httpClient.execute(deleteRequest);
        response.getEntity().getContent().close();
    }

    public boolean topicExistsUsingKafkaApi(String topicName) throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        AdminClient adminClient = AdminClient.create(props);
        ListTopicsResult topics = adminClient.listTopics();
        for (TopicListing topic : topics.listings().get()) {
            if (topic.name().equals(topicName)) {
                return true;
            }
        }
        return false;
    }

    public boolean topicExistsUsingRestApi(String topicName) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet getRequest = new HttpGet(restEndpoint);
        HttpResponse response = httpClient.execute(getRequest);
        String responseBody = response.getEntity().getContent().toString();
        return Arrays.asList(responseBody.split(",")).contains(topicName);
    }
}
