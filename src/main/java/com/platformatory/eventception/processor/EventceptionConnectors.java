package com.platformatory.eventception.processor;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig.OutputConfig.Sink;

public class EventceptionConnectors {
    public static void createConnector(Map<String, Object> connectorConfig) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonConfig = objectMapper.writeValueAsString(connectorConfig);

        URL url = new URL("http://localhost:8083/connectors");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonConfig.getBytes());
            os.flush();
        }

        if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
            throw new RuntimeException("Failed to create connector. HTTP error code: "
                + connection.getResponseCode());
        }
    }

    public static void createWebHookConnector(Sink sink, String topics) {
        Map<String, Object> config = new HashMap<>();
        config.put("name", sink.getName());
        
        Map<String, String> connectorSettings = new HashMap<>();
        connectorSettings.put("connector.class", "io.aiven.kafka.connect.http.HttpSinkConnector");
        connectorSettings.put("tasks.max", "1");
        connectorSettings.put("topics", topics);
        connectorSettings.put("http.url", (String) sink.getConfig().get("url"));
        connectorSettings.put("http.headers.additional", (String) sink.getConfig().get("url"));
        connectorSettings.put("errors.tolerance", "all");
        connectorSettings.put("errors.log.enable", "true");
        connectorSettings.put("errors.log.include.messages", "true");
        connectorSettings.put("http.headers.additional", (String) sink.getConfig().get("headers"));
        // TODO: Authentication
        
        config.put("config", connectorSettings);

        try {
            createConnector(config);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
