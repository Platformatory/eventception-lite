package com.platformatory.eventception.processor;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import com.platformatory.eventception.processor.EventceptionProcessors.DynamicTopicProcessor;
import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig;
import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig.ProcessorConfig;
import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig.OutputConfig;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class EventceptionTopologyBuilder {

    public static Topology buildTopology(TopologyConfig topologyConfig) throws Exception {
        Topology topology = new Topology();
        
        // Adding source processor based on input topics
        topology.addSource("source", Pattern.compile(topologyConfig.getInput().getTopics()));
        String parentProcessorName = "source";
        ArrayList<String> processorNames = new ArrayList<String>();

        try {
            for (ProcessorConfig processorConfig : topologyConfig.getProcessors()) {
                String className = "com.platformatory.eventception.processor.EventceptionProcessors$" + processorConfig.getType();
                String processorName = processorConfig.getName();
                
                if (processorConfig.getType().equals("DynamicTopicProcessor")) {
                    // Retrieve OutputConfig for DynamicTopicProcessor
                    OutputConfig outputConfig = topologyConfig.getOutput(); 
                    
                    // Special handling for DynamicTopicProcessor
                    topology.addProcessor(processorName, () -> new DynamicTopicProcessor(outputConfig), parentProcessorName);
                } else {
                    // Generic processor handling via reflection
                    Class<?> clazz = Class.forName(className);
                    Constructor<?> constructor = clazz.getConstructor(ProcessorConfig.class);
                    topology.addProcessor(processorName, () -> {
                        try {
                            processorNames.add(processorName);
                            return (Processor<String, String, String, String>) constructor.newInstance(processorConfig);
                        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                            throw new RuntimeException("Failed to initialize processors", e);
                        }
                    }, parentProcessorName);
                }

                if (processorConfig.getType().equals("ChangeDataCapture")) {
                    // Add state store for ChangeDataCapture processor
                    StoreBuilder<KeyValueStore<String, String>> storeBuilder = Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(processorConfig.getName() + "-state-store"),
                        Serdes.String(),
                        Serdes.String()
                    );
                    topology.addStateStore(storeBuilder, processorName);
                }
                
                parentProcessorName = processorName;  // The current processor becomes the parent for the next one
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize processors", e);
        }

        // Add a DLQ processor at the end to handle errors
        topology.addProcessor("dlq-processor", () -> new DlqProcessor(), processorNames.toArray(new String[0]));

        return topology;
    }
}
