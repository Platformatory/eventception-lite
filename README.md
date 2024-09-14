# Eventception Lite

Light weight version of Eventception built with Kafka Streams with a declarative config.

## Setup

```bash
cp config.yaml.sample config.yaml
# Make necessary changes such as bootstrap servers, authentication, etc.
```

```bash
docker-compose up -d build
```


### TODO

- [ ] CEL Filter
- [ ] Transform processor
- [ ] Logging in Kafka Streams
- [ ] Support DLQ
- [ ] keyLookupExpression in CDC