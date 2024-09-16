package com.avekshaa;

import com.avekshaa.client.InfluxDBClient;
import com.avekshaa.config.InfluxDBConfig;
import com.avekshaa.config.NatsConfig;
import com.avekshaa.nats.processor.MessageProcessor;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Subscription;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10); // Fixed thread pool with 10 threads

    public static void main(String[] args) {
        Connection natsConnection = null;
        InfluxDBClient influxDBClient = null;
        MessageProcessor messageProcessor = null;

        // Set default values from the config file
        String influxDBUrl = InfluxDBConfig.getInfluxUrl();
        String influxToken = InfluxDBConfig.getInfluxToken();
        String influxBucket = InfluxDBConfig.getInfluxBucket();
        String influxOrg = InfluxDBConfig.getInfluxOrg();

        // Override with command line arguments if provided
        if (args.length > 0 && args[0] != null && !args[0].isEmpty()) {
            influxDBUrl = args[0];
        }
        if (args.length > 1 && args[1] != null && !args[1].isEmpty()) {
            influxToken = args[1];
        }
        if (args.length > 2 && args[2] != null && !args[2].isEmpty()) {
            influxBucket = args[2];
        }
        if (args.length > 3 && args[3] != null && !args[3].isEmpty()) {
            influxOrg = args[3];
        }

        try {
            // Initialize InfluxDB client with either arguments or config file values
            influxDBClient = new InfluxDBClient(influxDBUrl,influxToken, influxBucket, influxOrg);

            // Initialize NATS connection
            natsConnection = Nats.connect(NatsConfig.getNatsUrl());
            logger.info("Connected to NATS server");

            // Subscribe to the NATS subject
            Subscription subscription = natsConnection.subscribe(NatsConfig.getSubject());
            logger.info("Subscribed to NATS subject: " + NatsConfig.getSubject());

            // Create message processor
            messageProcessor = new MessageProcessor(subscription, influxDBClient);

            // Start processing messages asynchronously
            MessageProcessor finalMessageProcessor = messageProcessor;
            threadPool.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        // Fetch and process the next message
                        finalMessageProcessor.startProcessing();
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error processing message", e);
                }
            });

        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred", e);
            // Ensure resources are cleaned up in case of an exception
            if (influxDBClient != null) {
                influxDBClient.close();
            }
            if (natsConnection != null) {
                try {
                    natsConnection.close();
                } catch (InterruptedException ex) {
                    logger.log(Level.SEVERE, "Error closing NATS connection", ex);
                    Thread.currentThread().interrupt();  // Restore interrupted status
                }
            }
        } finally {
            // Add shutdown hook for resource cleanup
            Connection finalNatsConnection = natsConnection;
            InfluxDBClient finalInfluxDBClient = influxDBClient;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (finalNatsConnection != null) {
                        finalNatsConnection.close();
                    }
                    if (finalInfluxDBClient != null) {
                        finalInfluxDBClient.close();
                    }

                    threadPool.shutdown();
                    if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                        threadPool.shutdownNow();
                    }

                    logger.info("Application stopped and resources cleaned up.");
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during shutdown", e);
                }
            }));
        }
    }
}
