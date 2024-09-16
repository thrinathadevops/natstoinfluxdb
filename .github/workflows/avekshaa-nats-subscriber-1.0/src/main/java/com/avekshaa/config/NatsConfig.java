package com.avekshaa.config;

public class NatsConfig {

    private static final String DEFAULT_NATS_URL = "nats://localhost:4222";
    private static final String DEFAULT_SUBJECT = "nats.subject";

    private static final String natsUrl;
    private static final String subject;

    static {
        // Retrieve values from environment variables or use default values
        natsUrl = System.getenv().getOrDefault("NATS_URL", DEFAULT_NATS_URL);
        subject = System.getenv().getOrDefault("NATS_SUBJECT", DEFAULT_SUBJECT);
    }

    private NatsConfig() {
        // Private constructor to prevent instantiation
    }

    public static String getNatsUrl() {
        return natsUrl;
    }

    public static String getSubject() {
        return subject;
    }
}
