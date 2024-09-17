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
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        ServiceConfig config = mapper.readValue(new File("config.yaml"), ServiceConfig.class);

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, config.getKafka().getConfig().getStreamsProperties().getApplicationId());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.getKafka().getConfig().getBootstrapServers());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.STATE_DIR_CONFIG, config.getKafka().getConfig().getStreamsProperties().getStateDir());

        config.getKafka().getConfig().getStreamsProperties().getConsumer().forEach((key, value) -> 
            props.put("consumer." + key, value));
        config.getKafka().getConfig().getStreamsProperties().getProducer().forEach((key, value) -> 
            props.put("producer." + key, value));
        config.getKafka().getConfig().getStreamsProperties().getAdminClient().forEach((key, value) -> 
            props.put("admin." + key, value));
        config.getKafka().getConfig().getAuthentication().forEach((key, value) -> 
            props.put(key, value));

        

        // for (TopologyConfig topologyConfig : config.getTopologies()) {
            TopologyConfig topologyConfig = config.getTopologies().get(0);
            EventceptionConnectWorkerBuilder.startConnectWorker(config.getKafka().getConfig());
            // for (Sink sink : topologyConfig.getOutput().getSinks()) {
            //     // TODO: Wait for connect to start
            //     if (sink.getType() == "Webhook") {
            //         EventceptionConnectors.createWebHookConnector(sink, topologyConfig.getOutput().getTopic());
            //     }
            // }
            Topology topology = EventceptionTopologyBuilder.buildTopology(topologyConfig);

            topology.describe();
            KafkaStreams streams = new KafkaStreams(topology, props);

            // Add custom JMX metrics
            // streams.setStateListener((newState, oldState) -> {
            //     StreamsMetricsImpl metrics = (StreamsMetricsImpl) streams.metrics();
            //     metrics.addLatencyRateTotalSensor("custom-latency", "latency", "description", RecordingLevel.INFO);
            //     metrics.addRateTotalSensor("custom-throughput", "throughput", "description", RecordingLevel.INFO);
            // });

            streams.start();

            Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        // }
    }
}