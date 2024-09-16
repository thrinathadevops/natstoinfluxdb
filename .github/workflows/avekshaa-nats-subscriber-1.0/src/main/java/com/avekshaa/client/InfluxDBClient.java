package com.avekshaa.client;

import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.write.Point;

import java.util.logging.Level;
import java.util.logging.Logger;

public class InfluxDBClient {

    private static final Logger logger = Logger.getLogger(InfluxDBClient.class.getName());

    private final com.influxdb.client.InfluxDBClient influxDBClient;
    private final String bucket;
    private final String org;
    private final WriteApi writeApi;  // Asynchronous Write API

    public InfluxDBClient(String url, String token, String bucket, String org) {
        try {
            this.influxDBClient = InfluxDBClientFactory.create(url, token.toCharArray());
            this.bucket = bucket;
            this.org = org;
            this.writeApi = influxDBClient.getWriteApi();  // Use non-blocking API for better performance
            logger.info("InfluxDB client and WriteApi initialized successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize InfluxDB client", e);
            throw e;  // Re-throw to ensure the caller is aware of the failure
        }
    }

    public void writeData(Point point) {
        try {
            // Non-blocking write
            writeApi.writePoint(bucket, org, point);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to write data to InfluxDB", e);
        }
    }

    public void close() {
        if (influxDBClient != null) {
            try {
                // Close the write API gracefully before closing the client
                writeApi.close();
                influxDBClient.close();
                logger.info("InfluxDB client closed successfully");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to close InfluxDB client", e);
            }
        }
    }
}
