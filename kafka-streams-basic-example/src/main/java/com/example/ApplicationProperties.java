package com.example;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;

import java.util.Objects;
import java.util.Properties;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;

/**
 * Utility class for retrieving Kafka properties and application-specific constants.
 */
@Slf4j
public class ApplicationProperties {
    public static final String KAFKA_STREAMS_APPLICATION_ID = "kafka-aggregator";
    public static final String BOOTSTRAP_SERVERS;
    public static final String SCHEMA_REGISTRY_URL;
    public static final String PURCHASE_TOPIC = "events.purchases";

    static {
        // Retrieve bootstrap servers and schema registry URL from environment variables
        var bootstrapServersFromEnvVariable = System.getenv("BOOTSTRAP_SERVERS");
        var schemaRegistryUrlFromEnvVariable = System.getenv("SCHEMA_REGISTRY_URL");

        // Use the environment variables if available, otherwise use default values
        BOOTSTRAP_SERVERS = Objects.requireNonNullElse(bootstrapServersFromEnvVariable, "localhost:9092");
        SCHEMA_REGISTRY_URL = Objects.requireNonNullElse(schemaRegistryUrlFromEnvVariable, "http://localhost:8081");

        log.info("Kafka broker: {}", BOOTSTRAP_SERVERS);
        log.info("Schema registry: {}", SCHEMA_REGISTRY_URL);
    }

    private ApplicationProperties() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Retrieves Kafka properties for configuring Kafka Streams.
     *
     * @return Kafka properties object
     */
    public static Properties getKafkaProperties() {
        var props = new Properties();
        props.put(APPLICATION_ID_CONFIG, KAFKA_STREAMS_APPLICATION_ID);
        props.put(BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        props.put(SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        return props;
    }
}
