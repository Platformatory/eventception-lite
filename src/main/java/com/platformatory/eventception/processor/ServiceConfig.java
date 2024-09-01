package com.platformatory.eventception.processor;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceConfig {
    private String name;
    private String description;
    private Kafka kafka;
    private List<TopologyConfig> topologies;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public List<TopologyConfig> getTopologies() {
        return topologies;
    }

    public void setTopologies(List<TopologyConfig> topologies) {
        this.topologies = topologies;
    }

    public static class Kafka {
        private StreamsConfig streamsConfig;
        

        public static class StreamsConfig {
            private String bootstrapServers;
            private Properties properties;
            private Map<String, String> authentication;

            public static class Properties {
                @JsonProperty("application.id")
                private String applicationId;
                @JsonProperty("default.key.serde")
                private String defaultKeySerde;
                @JsonProperty("default.value.serde")
                private String defaultValueSerde;
                @JsonProperty("state.dir")
                private String stateDir;
                private Map<String, String> consumer;
                private Map<String, String>  producer;
                private Map<String, String>  adminClient;

                public String getApplicationId() {
                    return applicationId;
                }

                public void setApplicationId(String applicationId) {
                    this.applicationId = applicationId;
                }

                public String getDefaultKeySerde() {
                    return defaultKeySerde;
                }

                public void setDefaultKeySerde(String defaultKeySerde) {
                    this.defaultKeySerde = defaultKeySerde;
                }

                public String getDefaultValueSerde() {
                    return defaultValueSerde;
                }

                public void setDefaultValueSerde(String defaultValueSerde) {
                    this.defaultValueSerde = defaultValueSerde;
                }

                public String getStateDir() {
                    return stateDir;
                }

                public void setStateDir(String stateDir) {
                    this.stateDir = stateDir;
                }

                public Map<String, String> getConsumer() {
                    return consumer;
                }

                public void setConsumer(Map<String, String>  consumer) {
                    this.consumer = consumer;
                }

                public Map<String, String>  getProducer() {
                    return producer;
                }

                public void setProducer(Map<String, String>  producer) {
                    this.producer = producer;
                }

                public Map<String, String> getAdminClient() {
                    return adminClient;
                }

                public void setAdminClient(Map<String, String> adminClient) {
                    this.adminClient = adminClient;
                }

            }

            public String getBootstrapServers() {
                return bootstrapServers;
            }

            public void setBootstrapServers(String bootstrapServers) {
                this.bootstrapServers = bootstrapServers;
            }

            public Properties getProperties() {
                return properties;
            }

            public void setProperties(Properties properties) {
                this.properties = properties;
            }

            public Map<String, String> getAuthentication() {
                return authentication;
            }

            public void setAuthentication(Map<String, String> authentication) {
                this.authentication = authentication;
            }

            public static class Authentication {
                @JsonProperty("sasl.mechanism")
                private String saslMechanism;
                @JsonProperty("security.protocol")
                private String securityProtocol;
                @JsonProperty("sasl.jaas.config")
                private String saslJaasConfig;
                public String getSaslMechanism() {
                    return saslMechanism;
                }
                public void setSaslMechanism(String saslMechanism) {
                    this.saslMechanism = saslMechanism;
                }
                public String getSecurityProtocol() {
                    return securityProtocol;
                }
                public void setSecurityProtocol(String securityProtocol) {
                    this.securityProtocol = securityProtocol;
                }
                public String getSaslJaasConfig() {
                    return saslJaasConfig;
                }
                public void setSaslJaasConfig(String saslJaasConfig) {
                    this.saslJaasConfig = saslJaasConfig;
                }

                
            }
        }

        public StreamsConfig getStreamsConfig() {
            return streamsConfig;
        }

        public void setStreamsConfig(StreamsConfig streamsConfig) {
            this.streamsConfig = streamsConfig;
        }
    }

    public static class TopologyConfig {
        private String name;
        private Input input;
        private List<ProcessorConfig> processors;
        private OutputConfig output;

        

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Input getInput() {
            return input;
        }

        public void setInput(Input input) {
            this.input = input;
        }

        public List<ProcessorConfig> getProcessors() {
            return processors;
        }

        public void setProcessors(List<ProcessorConfig> processors) {
            this.processors = processors;
        }

        public OutputConfig getOutput() {
            return output;
        }

        public void setOutput(OutputConfig output) {
            this.output = output;
        }

        public static class Input {
            private String topics;

            public String getTopics() {
                return topics;
            }

            public void setTopics(String topics) {
                this.topics = topics;
            }

            
        }

        public static class ProcessorConfig {
            private String type;
            private String celExpression;
            private Transform transform;
            private String keyLookupExpression;
            private String name;

            

            public String getType() {
                return type;
            }



            public void setType(String type) {
                this.type = type;
            }



            public String getCelExpression() {
                return celExpression;
            }



            public void setCelExpression(String celExpression) {
                this.celExpression = celExpression;
            }



            public Transform getTransform() {
                return transform;
            }



            public void setTransform(Transform transform) {
                this.transform = transform;
            }



            public String getKeyLookupExpression() {
                return keyLookupExpression;
            }



            public void setKeyLookupExpression(String keyLookupExpression) {
                this.keyLookupExpression = keyLookupExpression;
            }


            public String getName() {
                return name;
            }



            public void setName(String name) {
                this.name = name;
            }


            public static class Transform {
                private String key;
                private String value;
                public String getKey() {
                    return key;
                }
                public void setKey(String key) {
                    this.key = key;
                }
                public String getValue() {
                    return value;
                }
                public void setValue(String value) {
                    this.value = value;
                }

                
            }
            
        }

        public static class OutputConfig {
            private String topic;
            private String dlq;
            public String getTopic() {
                return topic;
            }
            public void setTopic(String topic) {
                this.topic = topic;
            }
            public String getDlq() {
                return dlq;
            }
            public void setDlq(String dlq) {
                this.dlq = dlq;
            }

            
        }
    }
}
