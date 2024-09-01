package com.platformatory.eventception.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import java.io.File;
import java.util.Properties;

public class EventceptionApiProcessor {

    public static void main(String[] args) throws Exception {
        // Load YAML configuration
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        ServiceConfig config = mapper.readValue(new File("config.yaml"), ServiceConfig.class);

        // Setup Kafka Streams properties
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, config.getKafka().getStreamsConfig().getProperties().getApplicationId());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.getKafka().getStreamsConfig().getBootstrapServers());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.STATE_DIR_CONFIG, config.getKafka().getStreamsConfig().getProperties().getStateDir());

        // Add prefixed properties
        config.getKafka().getStreamsConfig().getProperties().getConsumer().forEach((key, value) -> 
            props.put("consumer." + key, value));
        config.getKafka().getStreamsConfig().getProperties().getProducer().forEach((key, value) -> 
            props.put("producer." + key, value));
        config.getKafka().getStreamsConfig().getProperties().getAdminClient().forEach((key, value) -> 
            props.put("admin." + key, value));
        config.getKafka().getStreamsConfig().getAuthentication().forEach((key, value) -> 
            props.put(key, value));

        

        // for (TopologyConfig topologyConfig : config.getTopologies()) {
            TopologyConfig topologyConfig = config.getTopologies().get(0);
            // Build the topology
            Topology topology = EventceptionTopologyBuilder.buildTopology(topologyConfig);

            topology.describe();

            // Create the Kafka Streams instance
            KafkaStreams streams = new KafkaStreams(topology, props);

            // Add custom JMX metrics
            // streams.setStateListener((newState, oldState) -> {
            //     StreamsMetricsImpl metrics = (StreamsMetricsImpl) streams.metrics();
            //     metrics.addLatencyRateTotalSensor("custom-latency", "latency", "description", RecordingLevel.INFO);
            //     metrics.addRateTotalSensor("custom-throughput", "throughput", "description", RecordingLevel.INFO);
            // });

            // Start the Kafka Streams application
            streams.start();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        // }
    }
}