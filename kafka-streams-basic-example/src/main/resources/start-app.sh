#!/bin/bash

echo 'Starting application containers..'
docker-compose -f docker-compose.yml up -d test-kafka-streams-app
echo 'Done'