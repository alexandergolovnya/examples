#!/bin/bash

echo 'Starting infra containers..'
docker-compose -f docker-compose.yml up -d test-kafka test-schema-registry test-kafka-ui
echo 'Done'