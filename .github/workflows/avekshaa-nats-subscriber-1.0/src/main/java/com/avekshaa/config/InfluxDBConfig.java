package com.avekshaa.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class InfluxDBConfig {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = InfluxDBConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find app.properties file");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load app.properties", e);
        }
    }

    public static String getInfluxUrl() { return properties.getProperty("influx_url"); }

    public static String getInfluxToken() {
        return properties.getProperty("influx_token");
    }

    public static String getInfluxBucket() {
        return properties.getProperty("influx_bucket");
    }

    public static String getInfluxOrg() {
        return properties.getProperty("influx_org");
    }
}
