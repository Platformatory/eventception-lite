package com.platformatory.eventception.processor;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig;
import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig.SubTopologyConfig;
import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig.SubTopologyConfig.ProcessorConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class EventceptionTopologyBuilder {

    public static Topology buildTopology(TopologyConfig topologyConfig) throws Exception {
            Topology topology = new Topology();
            
            
            topology.addSource("source", Pattern.compile(topologyConfig.getInput().getTopics()));
            for(SubTopologyConfig subTopologyConfig : topologyConfig.getSubTopologies()) {
                String parentProcessorName = "source";
                ArrayList<String> processorNames = new ArrayList<String>();
                try {
                    for (ProcessorConfig processorConfig : subTopologyConfig.getProcessors()) {
                        String className = "com.platformatory.eventception.processor.EventceptionProcessors$"+processorConfig.getType();
                        String processorName = processorConfig.getName();
                        Class<?> clazz = Class.forName(className);
                        Constructor<?> constructor = clazz.getConstructor(ProcessorConfig.class, String.class);
                        topology.addProcessor(processorName, () -> {
                            try {
                                processorNames.add(processorName);
                                return (Processor<String, String, String, String>) constructor.newInstance(processorConfig, subTopologyConfig.getName());
                            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                                    | InvocationTargetException e) {
                                    throw new RuntimeException("Failed to initialize processors", e);
                            }
                        }, parentProcessorName);

                        if (processorConfig.getType().equals("ChangeDataCapture")) {
                            StoreBuilder<KeyValueStore<String, String>> storeBuilder = Stores.keyValueStoreBuilder(
                                Stores.persistentKeyValueStore(processorConfig.getName() + "-state-store"),
                                Serdes.String(),
                                Serdes.String()
                            );
                            topology.addStateStore(storeBuilder, processorName);
                        }
                        parentProcessorName = processorName;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize processors", e);
                }
                topology.addProcessor(subTopologyConfig.getName() + "-dlq-processor", () -> new DlqProcessor(), processorNames.toArray(new String[0]));

                

                topology.addSink(subTopologyConfig.getName() + "-sink", subTopologyConfig.getOutput().getTopic(), parentProcessorName);
                topology.addSink(subTopologyConfig.getName() + "-dlq", subTopologyConfig.getOutput().getDlq(), subTopologyConfig.getName() + "-dlq-processor");
            }

            return topology;
    }
}