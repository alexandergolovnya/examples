# Spring Boot Docker Basic Example

This project demonstrates the basic usage of Maven, Google JIB, and docker-compose.yml to dockerize a Spring Boot application.

## Project Structure

1. `src/main/java/com/example/SpringBootDockerExampleApp.java` — Main Spring application class
2. `src/main/java/com/example/SpringBootDockerExampleApp.WebController.java` — Sample REST API controller
3. `src/main/resources/docker-compose.yml` — Docker compose file for setting up the Spring application
4. `src/main/resources/application.yml` — Spring configuration file

## Prerequisites

Make sure you have installed:

- Java 17 or higher
- Apache Maven
- Docker and Docker Compose (if you want to run Kafka locally using Docker)

## How to Run

1. Build the project and Docker image

```sh
mvn clean package jib:dockerBuild
```

2. Start the application using docker-compose:

```sh
docker-compose -f docker-compose.yml up -d spring-boot-docker-example
```

3. Start the application as JAR

Alternatively, you can run the application as JAR using the following command:

```sh
java -jar target/spring-boot-docker-example-1.0-SNAPSHOT.jar
```
