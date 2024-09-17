package com.platformatory.eventception.processor;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.platformatory.eventception.processor.ServiceConfig.Kafka.Config;

public class EventceptionConnectWorkerBuilder {

    public static void startConnectWorker(Config kafkaConfig) {
        Map<String, String> connectDistributedProperties = new HashMap<>();
        connectDistributedProperties.put("offset.storage.topic", "eventception-connect-offsets");
        connectDistributedProperties.put("offset.storage.replication.factor", "-1");
        connectDistributedProperties.put("config.storage.topic", "eventception-connect-config");
        connectDistributedProperties.put("config.storage.replication.factor", "-1");
        connectDistributedProperties.put("status.storage.topic", "eventception-connect-status");
        connectDistributedProperties.put("status.storage.replication.factor", "-1");
        connectDistributedProperties.put("bootstrap.servers", kafkaConfig.getBootstrapServers());
        kafkaConfig.getAuthentication().forEach((key, value) -> 
            connectDistributedProperties.put(key, value));
        kafkaConfig.getConnectProperties().forEach((key, value) -> 
            connectDistributedProperties.put(key, value));
        connectDistributedProperties.put("plugin.path", "/usr/share/java");
        try (FileWriter writer = new FileWriter("connect-distributed.properties")) {
            for (Map.Entry<String, String> entry : connectDistributedProperties.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ProcessBuilder processBuilder = new ProcessBuilder("connect-distributed", "connect-distributed.properties");

        try {
            // Start the Kafka Connect distributed process in the background
            Process process = processBuilder.start();
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // System.out.println(line);  // Print stdout
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Thread errorThread = new Thread(() -> {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        // System.err.println(errorLine);  // Print stderr
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            outputThread.start();
            errorThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
