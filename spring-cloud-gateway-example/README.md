## About

This repo contains a Spring Cloud Gateway setup with a distributed dynamic capacity management and load shedding implementation allowing to prioritize requests from gateway's clients based on their priority (tier).

### Spring Cloud Gateway Architecture
![spring-cloud-gateway-architecture-diagram-1.png](docs/spring-cloud-gateway-architecture-diagram-1.png)

Key Components:
1. Dynamic Capacity Filter — custom GlobalFilter in Spring Cloud Gateway, which applies capacity limits dynamically based on client priority tiers.
2. Redis Backend — stores dynamic capacity maps (tier → capacity) using a hash structure. Lua scripts are used for atomic capacity initialization, acquisition, and release.
3. Capacity Manager — provides a capacity borrowing mechanism, in which high-priority tiers can borrow unused capacity from lower tiers, while requests from lower-priority tiers are shed during exhaustion.
4. Admin Control API — provides runtime control for capacity initialization and updates, which allows dynamic capacity changes without service interruption.
5. Spring Cloud Config Server — stores client’s configuration with tier mapping, and default capacity values per tier.
6. Scalability — Spring Cloud Gateway is stateless. Its instances share capacity state via Redis, supporting horizontal scaling without affecting resource management.

### Admin Control API

#### Get Current Capacity Values
```bash
curl --location 'localhost:8090/api/v1.0/admin/capacity'
```
Response example:
```json
{
  "capacities": {
    "1": 10,
    "2": 10,
    "3": 30
  },
  "success": true
}
```

#### Get Default Capacity Values
```bash
curl --location 'localhost:8090/api/v1.0/admin/capacity/default'
```
Response example:
```json
{
  "capacities": {
    "1": 10,
    "2": 10,
    "3": 30
  },
  "success": true
}
```

#### Dynamically Update Capacity Values

This method dynamically updates capacity values and adds new client tiers with their capacity to Redis.

```bash
curl --location --request PUT 'localhost:8090/api/v1.0/admin/capacity' \
--header 'Content-Type: application/json' \
--data '{
    "capacities": {
        "3": 30,
        "2": 10,
        "1": 10
    }
}'
```

Response example:
```json
{
  "capacities": {
    "1": 10,
    "2": 10,
    "3": 30
  },
  "success": true
}
```

### Priority Tier System
#### Tier Structure
* Tier 1: Highest priority (e.g., high-critical service)
* Tier 2: Medium priority services
* Tier 3: Lowest priority services

The lower tier number indicates higher priority. Tier numbers should be positive integers.
Adding new client tiers can be done via configs. 
No code changes are required to add a new client number and assign it to a client.

#### YML configuration
Three client tiers are provided by default. Tier configurations are defined as a pair:
```
client tier number → simultaneous in-flight request limit
```
In the example below, tier 1 has a capacity of 10 requests, tier 2 also has 10, and tier 3 has 30 concurrent requests set:
```yaml
spring-cloud-gateway:
  tiers-capacity:
    1: 10
    2: 10
    3: 30
  clients:
    - id: test_client_tier_1
      tier: 1
    - id: test_client_tier_2
      tier: 2
    - id: defaults
      tier: 3
```

## Building

### Pre Requisites

1. Docker to help to build, debug, and test Dockerized apps on a Mac.
2. Java 21 and Maven

### How to build

```bash
mvn clean package docker:build
```

### Start local demo

When build succeeds, run the following command:

```bash
start-dev.sh
```

If you want to start only infrastructure (Redis) containers, without the Gateway itself, run the following command:

```bash
start-infra-only-dev.sh
```

To stop all containers, run this command:

```bash
stop-dev.sh
```

### Run local demo load test

Demo load test is implemented using Gatling. To run it, execute the following command:
```bash
mvn -pl load-tests gatling:test -Dgatling.maxThreads=75
 ```

Load test class is located in `load-tests/src/main/java/com/example/LoadTestSimulation.java`

Generated Gatling reports after the test run will be located in `load-tests/target/gatling/`

### Run local demo API requests
Demo API requests can be sent to Spring Example API via Spring Coud Gateway. 

To send a request with a Tier 1 client "test_client_tier_1"", execute the following command:
```bash
curl --location 'localhost:8090/api/1.0/example' \
--header 'Authorization: Basic dGVzdF9jbGllbnRfdGllcl8xOnRlc3RwYXNzd29yZA=='
```


## Contributing

Currently, we use version control workflow with master-based releases (simple branch model).

### Commit Message Format

The commit message should be structured as follows:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Types are `fix:`, `feat:`, `build:`, `chore:`, `ci:`, `docs:`, `style:`, `refactor:`,
`perf:`, `test:`, and others.

Scope is a JIRA id or module name.

For the details, please, refer to: https://www.conventionalcommits.org/
