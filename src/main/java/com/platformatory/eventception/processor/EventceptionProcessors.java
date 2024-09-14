package com.platformatory.eventception.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
// import com.dashjoin.jsonata.JSONata;
// import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig.OutputConfig;
import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig.ProcessorConfig;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.CelValidationResult;
import dev.cel.common.ast.Expression;
import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.expr.ExprValue;
import dev.cel.expr.Value;
import dev.cel.parser.CelParser;
import dev.cel.parser.CelParserFactory;
import dev.cel.runtime.Activation;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;

import org.apache.kafka.streams.processor.To;
// import dev.cel.expr.CEL;
// import dev.cel.expr.CELBuilder;
// import dev.cel.expr.ExprValue;
// import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
// import org.apache.kafka.streams.processor.To;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.HashMap;
import java.util.Map;

public class EventceptionProcessors {

    public static class CelFilter implements Processor<String, String, String, String> {

    private ProcessorContext<String, String> context;
    private ProcessorConfig config;

    public CelFilter(ProcessorConfig config) {
        this.config = config;
    }

    public static Map<String, Object> parseJson(String jsonString) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonString, Map.class);
    }

    @Override
    public void init(ProcessorContext<String, String> context) {
        this.context = context;
    }

    @Override
    public void process(Record<String, String> record) {
        try {
            String key = record.key();
            String value = record.value();
            Map<String, Object> jsonData = parseJson(value);
            // Prepare the data to evaluate
            // for (Map.Entry<String, Object> entry : jsonData.entrySet()) {
            //     String key = entry.getKey();
            //     Object value = entry.getValue();

            //     // Create a CEL variable
            //     ExprValue celVar = ExprValue.of(value); // auto infers CEL type

            //     // Add the variable to CEL environment
            //     celRuntime.setVariable(key, celVar);
            // }
            // Activation activation = Activation.of(null, ImmutableMap.copyOf(jsonData));
            // Map<String, Value> variables = new HashMap<>();
            // variables.put("key", Value.newBuilder().setStringValue(key).build());
            // variables.put("value", Value.newBuilder().setStringValue(value).build());
            CelCompiler celCompiler = CelCompilerFactory.standardCelCompilerBuilder().setResultType(SimpleType.BOOL).build();
            // Initialize the CEL runtime and parse the CEL expression
            CelValidationResult parseResult = celCompiler.parse(this.config.getCelExpression());
            CelValidationResult checkResult = celCompiler.check(parseResult.getAst());
            CelAbstractSyntaxTree ast = checkResult.getAst();
            CelRuntime celRuntime = CelRuntimeFactory.standardCelRuntimeBuilder().build();
            // Evaluate the CEL expression
            CelRuntime.Program program = celRuntime.createProgram(ast);
            // boolean result = celRuntime.eval(this.celExpression, variables)
            //                   .getBoolValue();
            boolean result = (boolean) program.eval(ImmutableMap.copyOf(jsonData));

            // Forward the record if the CEL expression evaluates to true
            if (result) {
                context.forward(new Record<>(key, value, record.timestamp()));
            }
        } catch (CelEvaluationException e) {
            // Report any evaluation errors, if present
            throw new IllegalArgumentException(
                "Evaluation error has occurred. Reason: " + e.getMessage(), e);
        } catch (Exception e) {
            // Log or handle evaluation errors
            System.err.println("Error evaluating CEL expression: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        // Cleanup resources if necessary
    }

}

    // public static class CELFilter extends AbstractProcessor<String, String> {
    //     private final ProcessorConfig config;
    //     private final OutputConfig outputConfig;
    //     private CEL cel;
    //     private String celExpression;

    //     public CELFilter(ProcessorConfig config, OutputConfig outputConfig) {
    //         this.config = config;
    //         this.outputConfig = outputConfig;
    //     }

    //     @Override
    //     public void init(ProcessorContext context) {
    //         CELBuilder celBuilder = CEL.builder();
    //         cel = celBuilder.build();
    //         celExpression = config.getCelExpression();
    //     }

    //     @Override
    //     public void process(String key, String value) {
    //         try {
    //             // Evaluate the CEL expression
    //             Map<String, Object> variables = new HashMap<>();
    //             variables.put("request", key);
    //             variables.put("response", value);
    //             ExprValue exprValue = cel.eval(celExpression, variables);
    //             if (!exprValue.booleanValue()) {
    //                 context.forward(key, value, To.all().withTopic(outputConfig.getDlq()));
    //                 return;
    //             }
    //             context.forward(key, value);
    //         } catch (Exception e) {
    //             context.forward(key, value, To.all().withTopic(outputConfig.getDlq()));
    //         }
    //     }
    // }

    // public static class JSONataTransform extends AbstractProcessor<String, String> {
    //     private final ProcessorConfig config;
    //     private final OutputConfig outputConfig;
    //     private JSONata jsonataKey;
    //     private JSONata jsonataValue;

    //     public JSONataTransform(ProcessorConfig config, OutputConfig outputConfig) {
    //         this.config = config;
    //         this.outputConfig = outputConfig;
    //     }

    //     @Override
    //     public void init(ProcessorContext context) {
    //         try {
    //             jsonataKey = JSONata.newInstance().evaluate(config.getTransform().getKey());
    //             jsonataValue = JSONata.newInstance().evaluate(config.getTransform().getValue());
    //         } catch (Exception e) {
    //             throw new RuntimeException("Failed to initialize JSONata expressions", e);
    //         }
    //     }

    //     @Override
    //     public void process(String key, String value) {
    //         try {
    //             String transformedKey = jsonataKey.evaluate(key).stringValue();
    //             String transformedValue = jsonataValue.evaluate(value).stringValue();
    //             context.forward(transformedKey, transformedValue);
    //         } catch (Exception e) {
    //             context.forward(key, value, To.all().withTopic(outputConfig.getDlq()));
    //         }
    //     }
    // }

    public static class ChangeDataCapture implements Processor<String, String, String, String> {
        private final ProcessorConfig config;
        private ProcessorContext<String, String> context;
        private KeyValueStore<String, String> stateStore;

        public ChangeDataCapture(ProcessorConfig config) {
            this.config = config;
        }

        @Override
        public void init(ProcessorContext<String, String> context) {
            this.context = context;
            stateStore = context.getStateStore(config.getName()+"-state-store");
        }

        @Override
        public void process(Record<String, String> record) {
            String beforeImage = stateStore.get(record.key());
            String afterImage = record.value();
            Map<String, Object> diff = calculateDiff(beforeImage, afterImage);

            context.forward(new Record<>(record.key(), String.format("Before: %s, After: %s, Diff: %s", beforeImage, afterImage, diff), record.timestamp()));
            stateStore.put(record.key(), afterImage);
        }

        private Map<String, Object> calculateDiff(String beforeImage, String afterImage) {
            Map<String, Object> diff = new HashMap<>();
            if (beforeImage == null || afterImage == null) {
                return diff;
            }

            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> beforeMap = mapper.readValue(beforeImage, Map.class);
                Map<String, Object> afterMap = mapper.readValue(afterImage, Map.class);

                for (String key : beforeMap.keySet()) {
                    if (!afterMap.containsKey(key) || !beforeMap.get(key).equals(afterMap.get(key))) {
                        diff.put(key, Map.of("before", beforeMap.get(key), "after", afterMap.get(key)));
                    }
                }

                for (String key : afterMap.keySet()) {
                    if (!beforeMap.containsKey(key)) {
                        diff.put(key, Map.of("before", null, "after", afterMap.get(key)));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to calculate diff", e);
            }

            return diff;
        }
    }

    // public static class SinkProcessor implements Processor<String, String, String, String> {
    //     private final OutputConfig outputConfig;
    //     private ProcessorContext<String, String> context;

    //     public SinkProcessor(OutputConfig outputConfig) {
    //         this.outputConfig = outputConfig;
    //     }

    //     @Override
    //     public void init(ProcessorContext<String, String>  context) {
    //         this.context = context;
    //     }

    //     @Override
    //     public void process(Record<String, String> record) {
    //         try {
    //             // context.forward(record, To.all().withTopic(outputConfig.getTopic()));
    //         } catch (Exception e) {
    //             // context.forward(record, To.all().withTopic(outputConfig.getDlq()));
    //         }
    //     }
    // }
}