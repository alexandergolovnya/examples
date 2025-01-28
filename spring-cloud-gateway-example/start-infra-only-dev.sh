#!/bin/bash

echo 'Starting application containers..'
docker compose up -d spring-cloud-gateway-redis
echo 'Done'