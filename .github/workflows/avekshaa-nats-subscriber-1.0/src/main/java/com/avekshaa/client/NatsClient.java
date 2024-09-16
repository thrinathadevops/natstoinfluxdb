package com.avekshaa.client;

import com.avekshaa.config.NatsConfig;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Subscription;

import java.io.IOException;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NatsClient {

    private static final Logger logger = Logger.getLogger(NatsClient.class.getName());
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(5);

    private Connection connection;
    private Subscription subscription;

    public NatsClient() {
        try {
            connectWithRetries();
            subscribeToSubject();
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to initialize NATS client", e);
            // Ensure resources are cleaned up properly in case of initialization failure
            close();
            throw new RuntimeException("Failed to initialize NATS client", e);
        }
    }

    // Retry mechanism for establishing NATS connection
    private void connectWithRetries() throws IOException, InterruptedException {
        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                //connection = Nats.connect(NATS_URL);
                connection = Nats.connect(NatsConfig.getNatsUrl());
                logger.info("Connected to NATS server");
                return;  // Exit the loop once connection is successful
            } catch (IOException | InterruptedException e) {
                attempts++;
                logger.log(Level.WARNING, "Failed to connect to NATS (attempt " + attempts + ")", e);
                if (attempts >= MAX_RETRY_ATTEMPTS) {
                    logger.log(Level.SEVERE, "Max retry attempts reached. Unable to connect to NATS");
                    throw e;  // Re-throw after max retries
                }
                Thread.sleep(RETRY_DELAY.toMillis());  // Wait before retrying
            }
        }
    }

    // Subscription logic moved to a separate method for better structure
    private void subscribeToSubject() {
        if (connection != null) {
            try {
                subscription = connection.subscribe(NatsConfig.getSubject());
                logger.info("Subscribed to NATS subject: " + NatsConfig.getSubject());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to subscribe to NATS subject", e);
                throw new RuntimeException(e);  // Throw unchecked exception to prevent proceeding with a bad state
            }
        } else {
            throw new IllegalStateException("Connection is not established, cannot subscribe.");
        }
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("NATS connection closed successfully");
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Failed to close NATS connection", e);
                Thread.currentThread().interrupt();  // Restore interrupted status
            }
        }
    }
}
