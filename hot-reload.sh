#!/bin/bash

docker-compose up build --force-recreate --no-deps &

BUILD_STATUS=$(docker-compose ps -q build | xargs docker inspect -f '{{.State.Status}}')

while [ "$BUILD_STATUS" != "exited" ]; do
  sleep 2
  BUILD_STATUS=$(docker-compose ps -q build | xargs docker inspect -f '{{.State.Status}}')
done

docker-compose up -d --force-recreate eventception-lite

