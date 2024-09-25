#!/bin/bash

kafka-console-consumer --bootstrap-server localhost:9092 --topic orders-cdc --from-beginning | jq