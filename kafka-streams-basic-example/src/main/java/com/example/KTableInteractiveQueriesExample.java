package com.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class KTableInteractiveQueriesExample {
    public static void main(String[] args) {
        // Kafka Streams Configuration
        Properties config = new Properties();
        config.put("application.id", "ktable-interactive-queries-example");
        config.put("bootstrap.servers", "localhost:9092");
        config.put("default.key.serde", Serdes.String().getClass());
        config.put("default.value.serde", Serdes.String().getClass());

        // Define the processing topology
        StreamsBuilder builder = new StreamsBuilder();

        // Create a KTable from the 'transactions-topic'
        // Record key is a user id and the record value is a transaction amount
        builder.table("transactions-topic", Consumed.with(Serdes.String(), Serdes.String()), Materialized.as("transaction-store"));

        // Build the Kafka Streams application
        KafkaStreams streams = new KafkaStreams(builder.build(), config);

        // Latch to wait for streams to be in RUNNING state
        final CountDownLatch latch = new CountDownLatch(1);

        // State listener to listen for a transition to RUNNING state
        streams.setStateListener((newState, oldState) -> {
            if (newState == KafkaStreams.State.RUNNING && oldState != KafkaStreams.State.RUNNING) {
                latch.countDown();
            }
        });

        // Start the Kafka Streams application
        streams.start();

        // Wait for the streams to be in the RUNNING state
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        // Define the store query parameters
        StoreQueryParameters<ReadOnlyKeyValueStore<String, String>> storeQueryParameters = StoreQueryParameters.fromNameAndType(
                "transaction-store", QueryableStoreTypes.keyValueStore());

        // Fetch our store
        ReadOnlyKeyValueStore<String, String> keyValueStore = streams.store(storeQueryParameters);

        // We can now query the store directly for a user's transaction
        String userId = "1";

        // Get the transaction amount for the user with id '1'
        String transaction = keyValueStore.get(userId);

        // Print the result
        System.out.println("Transaction amount for user with id " + userId + " is: " + transaction);

        // Always close the Kafka Streams instance when you are done
        streams.close();
    }
}
