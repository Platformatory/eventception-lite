package com.platformatory.eventception.processor;

import com.dashjoin.jsonata.Jsonata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.google.common.collect.ImmutableMap;
import com.platformatory.eventception.processor.ServiceConfig.TopologyConfig.ProcessorConfig;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.common.CelValidationResult;
import dev.cel.common.types.CelTypes;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructTypeReference;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerBuilder;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Collection;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;

public class EventceptionProcessors {

    public static class CelFilter implements Processor<String, String, String, String> {

        private static final Logger log = LoggerFactory.getLogger(CelFilter.class);

        private ProcessorContext<String, String> context;
        private ProcessorConfig config;

        public CelFilter(ProcessorConfig config) {
            this.config = config;
        }

        public static CelCompilerBuilder addVariablesToCompiler(CelCompilerBuilder celCompilerBuilder, Map<String, Object> jsonData, String parentKey) throws InvalidProtocolBufferException, JsonProcessingException {
            if (celCompilerBuilder == null) {
                celCompilerBuilder = CelCompilerFactory.standardCelCompilerBuilder();
            }
            for (Map.Entry<String, Object> entry : jsonData.entrySet()) {
                String key = parentKey.isEmpty() ? entry.getKey() : parentKey + "." + entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String) {
                    celCompilerBuilder.addVar(key, CelTypes.STRING);
                } else if (value instanceof Integer) {
                    celCompilerBuilder.addVar(key, CelTypes.INT64);
                } else if (value instanceof Long) {
                    celCompilerBuilder.addVar(key, CelTypes.INT64);
                } else if (value instanceof Double) {
                    celCompilerBuilder.addVar(key, CelTypes.DOUBLE);
                } else if (value instanceof Boolean) {
                    celCompilerBuilder.addVar(key, CelTypes.BOOL);
                } else if (value instanceof Map || value instanceof List) {
                    Struct.Builder structBuilder = Struct.newBuilder();
                    Map<String, Object> val = (Map<String, Object>) value;
                    log.info("Value String "+new ObjectMapper().writeValueAsString(value));
                    JsonFormat.parser().merge(new ObjectMapper().writeValueAsString(value), structBuilder);
                    Struct struct = structBuilder.build();
                    celCompilerBuilder.addVar(parentKey, StructTypeReference.create(struct.getDescriptor().getFullName()));
                    celCompilerBuilder = addVariablesToCompiler(celCompilerBuilder, (Map<String, Object>) value, key);
                } else {
                    celCompilerBuilder.addVar(key, CelTypes.DYN);
                }
            }
            return celCompilerBuilder;
        }

        public static CelCompilerBuilder addListToCompiler(CelCompilerBuilder celCompilerBuilder, String key, List<?> list) throws InvalidProtocolBufferException, JsonProcessingException {
            if (!list.isEmpty()) {
                Object firstElement = list.get(0);

                if (firstElement instanceof String) {
                    celCompilerBuilder.addVar(key, CelTypes.createList(CelTypes.STRING));
                } else if (firstElement instanceof Integer) {
                    celCompilerBuilder.addVar(key, CelTypes.createList(CelTypes.INT64));
                } else if (firstElement instanceof Double) {
                    celCompilerBuilder.addVar(key, CelTypes.createList(CelTypes.DOUBLE));
                } else if (firstElement instanceof Boolean) {
                    celCompilerBuilder.addVar(key, CelTypes.createList(CelTypes.BOOL));
                } else if (firstElement instanceof Map) {
                    addVariablesToCompiler(celCompilerBuilder, (Map<String, Object>) firstElement, key);
                } else {
                    celCompilerBuilder.addVar(key, CelTypes.createList(CelTypes.DYN));
                }
            } else {
                celCompilerBuilder.addVar(key, CelTypes.createList(CelTypes.DYN));
            }
            return celCompilerBuilder;
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
                log.info("Processing record for CEL Filter "+value);
                Map<String, Object> flattenedJson = JsonFlattener.flattenAsMap(value);
                CelCompilerBuilder celCompilerBuilder = addVariablesToCompiler(null, flattenedJson, "");
                CelCompiler celCompiler = celCompilerBuilder.setResultType(SimpleType.BOOL).build();
                CelValidationResult parseResult = celCompiler.parse(this.config.getCelExpression());
                CelValidationResult checkResult = celCompiler.check(parseResult.getAst());
                CelAbstractSyntaxTree ast = checkResult.getAst();
                
                CelRuntime celRuntime = CelRuntimeFactory.standardCelRuntimeBuilder().build();
                CelRuntime.Program program = celRuntime.createProgram(ast);
                boolean result = (boolean) program.eval(ImmutableMap.copyOf(flattenedJson));

                log.info("CEL Result - "+ result);
                if (result) {
                    context.forward(new Record<>(key, value, record.timestamp()));
                }
            } catch (CelEvaluationException e) {
                log.error("Evaluation error has occurred. Reason: " + e.getMessage(), e);
                Record<String, String> dlqRecord = record.withHeaders(record.headers().add("error-message", e.getMessage().getBytes()));
                context.forward(dlqRecord, "dlq-processor");
            } catch (CelValidationException e) {
                log.error("Evaluation error has occurred. Reason: " + e.getMessage(), e);
                Record<String, String> dlqRecord = record.withHeaders(record.headers().add("error-message", e.getMessage().getBytes()));
                context.forward(dlqRecord, "dlq-processor");
            } catch (JsonProcessingException e) {
                log.error("Evaluation error has occurred. Reason: " + e.getMessage(), e);
                Record<String, String> dlqRecord = record.withHeaders(record.headers().add("error-message", e.getMessage().getBytes()));
                context.forward(dlqRecord, "dlq-processor");
            } catch (InvalidProtocolBufferException e) {
                log.error("Evaluation error has occurred. Reason: " + e.getMessage(), e);
                Record<String, String> dlqRecord = record.withHeaders(record.headers().add("error-message", e.getMessage().getBytes()));
                context.forward(dlqRecord, "dlq-processor");
            } catch (Exception e) {
                log.error("Error evaluating CEL expression: " + e);
                Record<String, String> dlqRecord = record.withHeaders(record.headers().add("error-message", e.getMessage().getBytes()));
                context.forward(dlqRecord, "dlq-processor");
            }
        }

        @Override
        public void close() {
            
        }

    }

    public static class JSONTransform implements Processor<String, String, String, String> {
        private static final Logger log = LoggerFactory.getLogger(JSONTransform.class);
        private final ProcessorConfig config;
        private Jsonata jsonataKey;
        private Jsonata jsonataValue;
        private ProcessorContext<String, String> context;

        public JSONTransform(ProcessorConfig config) {
            this.config = config;
        }

        @Override
        public void init(ProcessorContext<String, String> context) {
            this.context = context;
            try {
                jsonataKey = Jsonata.jsonata(config.getTransform().getKey());
                jsonataValue = Jsonata.jsonata(config.getTransform().getValue());
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize JSONata expressions", e);
            }
        }

        public static Map<String, Object> parseJson(String jsonString) throws JsonMappingException, JsonProcessingException {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonString, Map.class);
        }

        public static String convertToString(Object object) {
            if (object == null) {
                return "null";
            }
    
            if (object.getClass().isArray()) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                int length = Array.getLength(object);
                for (int i = 0; i < length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(convertToString(Array.get(object, i)));
                }
                sb.append("]");
                return sb.toString();
            }
    
            if (object instanceof Collection) {
                Collection<?> collection = (Collection<?>) object;
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                Iterator<?> iterator = collection.iterator();
                while (iterator.hasNext()) {
                    sb.append(convertToString(iterator.next()));
                    if (iterator.hasNext()) sb.append(", ");
                }
                sb.append("]");
                return sb.toString();
            }
    
            if (object instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) object;
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<?, ?> entry = iterator.next();
                    sb.append("\"").append(escapeJsonString(entry.getKey().toString())).append("\": ")
                      .append(convertToString(entry.getValue()));
                    if (iterator.hasNext()) sb.append(", ");
                }
                sb.append("}");
                return sb.toString();
            }
    
            if (object instanceof String) {
                return "\"" + escapeJsonString(object.toString()) + "\"";
            }
    
            if (object instanceof Number || object instanceof Boolean) {
                return object.toString();
            }
    
            return "\"" + escapeJsonString(object.toString()) + "\"";
        }
    
        private static String escapeJsonString(String input) {
            return input.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
        }
        @Override
        public void process(Record<String, String> record) {
            try {
                String key = record.key();
                String value = record.value();
                log.info("Processing record for JSON Transform "+value);
                Map<String, Object> jsonData = parseJson(value);
                String transformedKey = convertToString(jsonataKey.evaluate(jsonData));
                String transformedValue = convertToString(jsonataValue.evaluate(jsonData));
                context.forward(new Record<>(transformedKey, transformedValue, record.timestamp()));
            } catch (JsonProcessingException e) {
                log.error("Error transforming JSON: " + e);
                String errorMessage = "Error while parsing record value as JSON - "+e.getMessage();
                Record<String, String> dlqRecord = record.withHeaders(record.headers().add("error-message", errorMessage.getBytes()));
                context.forward(dlqRecord, "dlq-processor");
            } catch (Exception e) {
                log.error("Error transforming JSON: " + e);
                Record<String, String> dlqRecord = record.withHeaders(record.headers().add("error-message", e.getMessage().getBytes()));
                context.forward(dlqRecord, "dlq-processor");
            }
        }
    }


    public static class ChangeDataCapture implements Processor<String, String, String, String> {
        private static final Logger log = LoggerFactory.getLogger(ChangeDataCapture.class);
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
            log.info("Processing record for CDC " + afterImage);
            if (!afterImage.equals(beforeImage)) {
                // try{
                //     Map<String, Object> diff = calculateDiff(beforeImage, afterImage);
                // } catch(Exception e) {
                //     Record<String, String> dlqRecord = record.withHeaders(record.headers().add("error-message", e.getMessage().getBytes()));
                //     context.forward(dlqRecord, "dlq-processor");
                // }

                context.forward(new Record<>(record.key(), String.format("{\"before\": %s, \"after\": %s, \"timestamp\": %s}", beforeImage, afterImage, record.timestamp()), record.timestamp()));
                stateStore.put(record.key(), afterImage);
            }
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
}