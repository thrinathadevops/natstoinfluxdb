package com.avekshaa.nats.processor;

import com.avekshaa.client.InfluxDBClient;
import io.nats.client.Message;
import io.nats.client.Subscription;
import org.json.JSONObject;
import com.influxdb.client.write.Point;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageProcessor {

    private static final Logger logger = Logger.getLogger(MessageProcessor.class.getName());
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MessageProcessor.class);

    private Subscription subscription;
    private InfluxDBClient influxDBClient;

    public MessageProcessor(Subscription subscription, InfluxDBClient influxDBClient) {
        this.subscription = subscription;
        this.influxDBClient = influxDBClient;
    }

    public void startProcessing() {
        // This loop will run indefinitely to listen for messages.
        while (true) {
            try {
                // Wait for the next message from the NATS subscription.
                Message message = subscription.nextMessage(Duration.ofSeconds(10));

                if (message != null) {
                    //logger.info("Received a message");
                    String messageData = new String(message.getData());

                    // Assuming message data is a JSON string
                    JSONObject jsonObject = new JSONObject(messageData);

                  // logger.info(" Received message :"+ jsonObject);

                    if (isVirtualUserMessage(jsonObject)) {
//                        logger.info("processing VirtualUser Metrics");
                        processVirtualUserMetrics(jsonObject);
                    } else {
                        //logger.info("processing Sample Results");
                        processSampleResults(jsonObject);
                    }
                } else {
                    logger.log(Level.ALL, "No message received within the timeout period");
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "An error occurred while processing messages", e);
            }
        }
    }

    // Check if the message contains virtual user metrics
    private boolean isVirtualUserMessage(JSONObject jsonMessage) {
        return jsonMessage.has("MIN_ACTIVE_THREADS") &&
                jsonMessage.has("MEAN_ACTIVE_THREADS" ) &&
                jsonMessage.has("MAX_ACTIVE_THREADS") &&
                jsonMessage.has("STARTED_THREADS") &&
                jsonMessage.has("FINISHED_THREADS");
    }

    // Process and write virtual user metrics to InfluxDB under "virtualUsers" measurement
    private void processVirtualUserMetrics(JSONObject jsonObject) {
        try {
            String runId = jsonObject.optString("RUN_ID");
            String testName = jsonObject.optString("TEST_NAME");
            int minActiveThreads = jsonObject.optInt("MIN_ACTIVE_THREADS");
            int meanActiveThreads = jsonObject.optInt("MEAN_ACTIVE_THREADS");
            int maxActiveThreads = jsonObject.optInt("MAX_ACTIVE_THREADS");
            int startedThreads = jsonObject.optInt("STARTED_THREADS");
            int finishedThreads = jsonObject.optInt("FINISHED_THREADS");

            // Create InfluxDB point for virtualUsers
            Point point = Point.measurement("virtualUsers")
                    .addTag("runId", runId)
                    .addTag("testName", testName)
                    .addField("minActiveThreads", minActiveThreads)
                    .addField("meanActiveThreads", meanActiveThreads)
                    .addField("maxActiveThreads", maxActiveThreads)
                    .addField("startedThreads", startedThreads)
                    .addField("finishedThreads", finishedThreads)
                    .time(Instant.now(), com.influxdb.client.domain.WritePrecision.NS);

            influxDBClient.writeData(point);
//            logger.info("Processed virtual user metrics: " + jsonObject.toString());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing virtual user metrics", e);
        }
    }

    // Process and write sample result data to InfluxDB under "requestsRaw" measurement
    private void processSampleResults(JSONObject jsonObject) {
        try {
            String runId = jsonObject.optString("RUN_ID");
            String testName = jsonObject.optString("TEST_NAME");
            String nodeName = jsonObject.optString("NODE_NAME");
            String requestName = jsonObject.optString("REQUEST_NAME");
            String responseCode = jsonObject.optString("RESPONSE_CODE");
            String errorMessage = jsonObject.optString("ERROR_MESSAGE");
            String sampleType = jsonObject.optString("SAMPLE_TYPE");
            String errorResponseBody = jsonObject.optString("ERROR_RESPONSE_BODY");
            long errorCount = jsonObject.optLong("ERROR_COUNT", 0L);
            long count = jsonObject.optLong("COUNT", 0L);
            long receivedBytes = jsonObject.optLong("RECEIVED_BYTES", 0L);
            long sentBytes = jsonObject.optLong("SENT_BYTES", 0L);
            long responseTime = jsonObject.optLong("RESPONSE_TIME", 0L);
            long latency = jsonObject.optLong("LATENCY", 0L);
            long connectTime = jsonObject.optLong("CONNECT_TIME", 0L);
            long processingTime = jsonObject.optLong("PROCESSING_TIME", 0L);

            // Create InfluxDB point for requestsRaw
            Point point = Point.measurement("requestsRaw")
                    .addTag("runId", runId)
                    .addTag("testName", testName)
                    .addTag("nodeName", nodeName)
                    .addTag("requestName", requestName)
                    .addTag("responseCode", responseCode)
                    .addTag("errorMessage", errorMessage)
                    .addTag("samplerType", sampleType)
                    .addTag("errorResponseBody", errorResponseBody)
                    .addField("errorCount", errorCount)
                    .addField("count", count)
                    .addField("receivedBytes", receivedBytes)
                    .addField("sentBytes", sentBytes)
                    .addField("responseTime", responseTime)
                    .addField("latency", latency)
                    .addField("connectTime", connectTime)
                    .addField("processingTime", processingTime)
                    .time(Instant.now(), com.influxdb.client.domain.WritePrecision.NS);

            influxDBClient.writeData(point);
            //logger.info("Processed sample results: " + jsonObject.toString());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing sample results", e);
        }
    }
}
