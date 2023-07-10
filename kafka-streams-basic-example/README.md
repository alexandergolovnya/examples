# Kafka Streams Basic Example

This project demonstrates the basic usage of Kafka Streams, including how to aggregate, join streams, and handle Avro data with specific Avro serializer/deserializer.

## Project Structure

1. `src/main/avro/Purchase.avsc` — Avro schema for Purchase class
2. `src/main/java/com/example/ApplicationProperties.java` — Utility class for application and Kafka related properties
3. `src/main/java/com/example/TestPurchaseProducer.java` — Test producer for generating Purchase events
4. `src/main/java/com/example/PurchasesKafkaStreamExample.java` — Main Kafka Streams class for processing Purchase events
4. `src/main/java/com/example/KTableInteractiveQueriesExample.java` — test Kafka Streams main class for playing around with interactive queries
5. `src/main/resources/docker-compose.yml` — Docker compose file for setting up the Kafka cluster
6. `src/main/resources/kafka/application.yml` — Configuration for Kafka UI (AKHQ)
7. `src/main/resources/logback.xml` — Logging configuration file
8. `src/main/resources/start-dev.sh` — Script for starting the Kafka Streams test application with Kafka cluster, Kafka UI, and Schema Registry
8. `src/main/resources/start-infra.sh` — Script for starting only Kafka cluster, Kafka UI, and Schema Registry
8. `src/main/resources/start-dev.sh` — Script for starting the Kafka cluster, Kafka UI, and Schema Registry

## Prerequisites

Make sure you have installed:

- Java 17 or higher
- Apache Maven
- Docker and Docker Compose (if you want to run Kafka locally using Docker)

## How to Run

1. Build the project

```sh
mvn clean package
```

2. Start the Kafka cluster using docker-compose:

```sh
sh src/main/resources/start-infra.sh
```

3. Start the application

You can run the application as JAR using the following command:

```sh
java -jar target/kafka-streams-basic-example-1.0-SNAPSHOT.jar
```

Alternatively, you can run the application using docker-compose.
For this you first need to build the Docker image:

```sh
mvn compile jib:dockerBuild
```

After that you can start the application using docker-compose via this command:

```sh
sh src/main/resources/start-app.sh
```

Your application will start with one test Purchase event in the 'events.purchases' topic.
Kafka UI (AKHQ) is available at http://localhost:8090/ui/my-cluster-plain-text

4. You can produce additional test data to the Kafka topic by running the `TestPurchaseProducer.java`:
```sh
mvn exec:java -Dexec.mainClass="com.example.TestPurchaseProducer"
```

## Kafka Streams Interactive Queries test example

You may find an example of Kafka Streams Interactive Queries implementation in the `src/main/java/com/example/KTableInteractiveQueriesExample.java` class.

**Build and start the application**
You can use the following commands to run this example:
    
```sh
mvn clean install && mvn exec:java -Dexec.mainClass="com.example.KTableInteractiveQueriesExample"
```
**Prerequisites**
1. Please make sure that you have started the Kafka cluster, Kafka UI, and Schema Registry using the `start-infra.sh` script.
2. You need to produce some test data to the `transactions-topic` topic and create this topic manually. You can do this via the Kafka UI or command line producer.

**High level description**
Let's break down this example step by step:

1. In the `main` method, we start by creating a `Properties` object to configure our Kafka Streams application. This includes the application identifier, Kafka broker address, and classes for serializing and deserializing keys and values. You need to change the Kafka broker address in the `bootstrap.servers` variable if it differs from `localhost:9092` in your case.
2. We then create a `StreamsBuilder`, which serves as the basis for defining our processing topology.
3. With `StreamsBuilder`, we create a `KTable` from the Kafka topic `transactions-topic`. `Consumed.with(Serdes.String(), Serdes.String())` specifies that the keys and values in this topic are serialized and deserialized as strings. `Materialized.as("transaction-store")` sets the name for the state store associated with this KTable. The creation of the Kafka topic `transactions-topic` and adding values to it also needs to be done on your side.
4. A `KafkaStreams` object is created using the topology we defined with `StreamsBuilder` and our configuration.
5. `CountDownLatch` is used to block the main thread until the Kafka Streams application is ready to process state queries.
6. `StateListener` is used to track changes in the state of the Kafka Streams application. When the state transitions to `RUNNING`, the countdown is returned, freeing up the main thread.
7. We start our Kafka Streams application, then wait for it to transition to the `RUNNING` state.
8. A `StoreQueryParameters` object is created for our state store query.
9. We get our state store using `streams.store(storeQueryParameters)` and save it to the `keyValueStore` variable.
10. Next, we can query our state store directly to get the transaction of the user with the id `1`.
11. The result is printed to the console and we close our Kafka Streams application.

This step-by-step breakdown should give a good understanding of how the example works and how to adapt it to your own use case.
Please note that this is a tutorial example and a real-world industrial solution for data stream processing will be significantly more complex.