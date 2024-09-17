# Eventception Lite

Light weight version of Eventception built with Kafka Streams with a declarative config.

## Setup

```bash
docker-compose up -d
# UI is accessible in 9021

# Generate data
./scripts/01_create_orders.sh
./scripts/02_update_orders.sh

# Consume CDC
./scripts/03_consume_cdc_messages.sh
```

### Troubleshooting

- If there are no messages in the cdc topic after generating data, check the logs with
```bash
docker compose logs eventception-lite
```
- If the last line of the log is `State transition from REBALANCING to RUNNING`, try restarting the eventception-lite container with
```bash
docker compose restart eventception-lite
```

### TODO

- [x] CEL Filter
- [x] Transform processor
- [x] Logging in Kafka Streams
- [x] Support DLQ
- [ ] keyLookupExpression in CDC
- [ ] Output connectors
- [ ] CEL expression for output topic
- [ ] Separate YAML files for config, k8s style