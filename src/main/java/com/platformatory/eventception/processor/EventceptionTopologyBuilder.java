package com.platformatory.eventception.processor;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.processor.api.Processor;
// import org.apache.kafka.streams.processor.api.ProcessorContext;
// import org.apache.kafka.streams.processor.api.ProcessorSupplier;
// import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig;
// import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig.OutputConfig;
import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig.ProcessorConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;

public class EventceptionTopologyBuilder {

    public static Topology buildTopology(TopologyConfig topologyConfig) throws Exception {
            Topology topology = new Topology();
            
            
            topology.addSource("source", Pattern.compile(topologyConfig.getInput().getTopics()));
            String parentProcessorName = "source";
            try {
                for (ProcessorConfig processorConfig : topologyConfig.getProcessors()) {
                    String className = "com.platformatory.eventception.processor.EventceptionProcessors$"+processorConfig.getType();
                    String processorName = processorConfig.getName();
                    Class<?> clazz = Class.forName(className);
                    Constructor<?> constructor = clazz.getConstructor(ProcessorConfig.class);
                    topology.addProcessor(processorName, () -> {
                        try {
                            return (Processor<String, String, String, String>) constructor.newInstance(processorConfig);
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
                        // topology.connectProcessorAndStateStores(processorName, processorConfig.getName() + "-state-store");
                        
                    }
                    parentProcessorName = processorName;
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize processors", e);
            }
            

            

            // TODO: Handle DLQ
            topology.addSink("sink", topologyConfig.getOutput().getTopic(), parentProcessorName);

            return topology;

            // // Create persistent state stores if needed
            // builder.addStateStore(
            //     Stores.keyValueStoreBuilder(
            //         Stores.persistentKeyValueStore(topologyConfig.getName() + "-state-store"),
            //         Serdes.String(),
            //         Serdes.String()
            //     )
            // );
            

            // // Create the processors based on the topology config
            // builder.stream(Pattern.compile(topologyConfig.getInput().getTopics()))
            // .
            //     // .process(new DynamicProcessorSupplier(topologyConfig), topologyConfig.getName() + "-state-store")
            //     .process(new ProcessorSupplier<String, String, String, String>() {
            //         @Override
            //         public Processor<String, String, String, String> get() {
            //             return new DynamicProcessor(topologyConfig);
            //         }
            //     }, topologyConfig.getName() + "-state-store")
            //     .process(new ProcessorSupplier<String, String, String, String>() {
            //         @Override
            //         public Processor<String, String, String, String> get() {
            //             return new EventceptionProcessors.SinkProcessor(topologyConfig.getOutputConfig());
            //         }
            //     }, "");
    }

    // public static class DynamicProcessorSupplier implements ProcessorSupplier<String, String, String, String> {
    //     private final TopologyConfig config;

    //     DynamicProcessorSupplier(TopologyConfig config) {
    //         this.config = config;
    //     }

    //     @Override
    //     public Processor<String, String, String, String> get() {
    //         return new DynamicProcessor(config);
    //     }
    // }

    // static class DynamicProcessor implements Processor<String, String, String, String> {
    //     private final TopologyConfig config;
    //     private ProcessorContext<String, String> context;
    //     private Map<String, Processor<String, String, String, String>> processors = new HashMap<>();

    //     DynamicProcessor(TopologyConfig config) {
    //         this.config = config;
    //     }

    //     @Override
    //     public void init(ProcessorContext<String, String> context) {
    //         this.context = context;
    //         try {
    //             for (ProcessorConfig processorConfig : config.getProcessors()) {
    //                 String className = processorConfig.getType();
    //                 System.out.println(className);
    //                 Class<?> clazz = Class.forName("EventceptionProcessors$" + className);
    //                 Constructor<?> constructor = clazz.getConstructor(ProcessorConfig.class, OutputConfig.class);
    //                 Processor<String, String, String, String> processor = (Processor<String, String, String, String>) constructor.newInstance(processorConfig, config.getOutput());
    //                 processor.init(context);
    //                 processors.put(className, processor);
    //             }
    //         } catch (Exception e) {
    //             throw new RuntimeException("Failed to initialize processors", e);
    //         }
    //     }


    //     @Override
    //     public void process(Record<String, String> record) {
    //         try {
    //             for (Processor<String, String, String, String> processor : processors.values()) {
    //                 processor.process(record);
    //             }
    //             context.forward(record);  // Forward to the next processor, in this case, the SinkProcessor
    //         } catch (Exception e) {
    //             System.err.println(e);
    //             // context.forward(record, To.all().withTopic(config.getOutputConfig().getDlq()));
    //             // TODO: Handle DLQ
    //             context.forward(record);
    //         }
    //     }
    // }
}