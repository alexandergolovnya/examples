#!/bin/bash

echo 'Starting application containers..'
docker-compose -f docker-compose.yml up -d spring-cloud-gateway-example spring-cloud-gateway-redis
echo 'Done'