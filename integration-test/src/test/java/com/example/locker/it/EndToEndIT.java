package com.example.locker.it;

import com.example.locker.common.JsonMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DockerComposeStack.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EndToEndIT {

    private static final String BASE = "https://localhost:8443";
    private static final String LOCKER_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
    private static final String COMPARTMENT_ID = "compartmentInfo@d4e5f6a7-b8c9-0123-defa-234567890123";
    private static final String TOPIC = "psfusion/business-event/v010/compartment/opened";

    private static HttpClient httpClient;

    @BeforeAll
    static void setup() throws Exception {
        SSLContext ssl = CertLoader.clientSslContext();
        httpClient = HttpClient.newBuilder().sslContext(ssl).build();
    }

    @Test
    @Order(1)
    void restServerHealthEndpointReturns200() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/health"))
                .GET().build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("\"status\":\"UP\""));
    }

    @Test
    @Order(2)
    void openCompartmentReturns200AndEmitsEvent() throws Exception {
        try (MqttTestClient mqtt = new MqttTestClient(CertLoader.clientSslContext(), TOPIC)) {
            String url = BASE + "/api/v010/locker/" + LOCKER_ID + "/compartment/" + COMPARTMENT_ID + "/open";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .header("Client-Version", "1.2.6")
                    .header("Lock-Token", "gy12p6N1UC4WJp4H1tFhVqtlzgm7qYp4qVnq5p/PLu4=")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, resp.statusCode());

            // Wait for MQTT event
            String payload = mqtt.poll(10);
            assertNotNull(payload, "Expected MQTT event within 10s");

            JsonNode node = JsonMapper.instance().readTree(payload);
            assertEquals("CompartmentOpenedEvent", node.path("headers").path("eventType").asText());
            assertEquals(COMPARTMENT_ID, node.path("payload").path("compartmentId").asText());
        }
    }

    @Test
    @Order(3)
    void invalidCompartmentIdReturns400() throws Exception {
        String url = BASE + "/api/v010/locker/" + LOCKER_ID + "/compartment/bogus/open";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Client-Version", "1.2.6")
                .header("Lock-Token", "dummy")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());

        JsonNode node = JsonMapper.instance().readTree(resp.body());
        assertEquals("INVALID_COMPARTMENT_ID", node.path("errorCode").asText());
    }

    @Test
    @Order(4)
    void missingLockTokenHeaderReturns400() throws Exception {
        String url = BASE + "/api/v010/locker/" + LOCKER_ID + "/compartment/" + COMPARTMENT_ID + "/open";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Client-Version", "1.2.6")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, resp.statusCode());

        JsonNode node = JsonMapper.instance().readTree(resp.body());
        assertEquals("MISSING_HEADER", node.path("errorCode").asText());
        assertTrue(node.path("message").asText().contains("Lock-Token"));
    }

    @Test
    @Order(5)
    void httpsWithoutClientCertIsRejected() throws Exception {
        SSLContext trustOnly = CertLoader.trustOnlySslContext();
        HttpClient noClientCert = HttpClient.newBuilder().sslContext(trustOnly).build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/health"))
                .GET().build();

        assertThrows(Exception.class, () -> {
            noClientCert.send(req, HttpResponse.BodyHandlers.ofString());
        }, "Connection without client cert should be rejected");
    }

    @Test
    @Order(6)
    void mqttWithoutClientCertIsRejected() throws Exception {
        SSLContext trustOnly = CertLoader.trustOnlySslContext();
        assertThrows(Exception.class, () -> {
            new MqttTestClient(trustOnly, TOPIC);
        }, "MQTT connection without client cert should be rejected");
    }

    @Test
    @Order(7)
    void standaloneMqttPublisherEventWasObservedBySubscriber() throws Exception {
        // Check docker logs for mqtt-subscriber to have at least 2 "Received event" lines
        File projectRoot = DockerComposeStack.findProjectRoot();
        ProcessBuilder pb = new ProcessBuilder("docker", "compose", "logs", "mqtt-subscriber")
                .directory(projectRoot).redirectErrorStream(true);
        Process p = pb.start();
        String logs = new String(p.getInputStream().readAllBytes());
        p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);

        long count = logs.lines().filter(l -> l.contains("Received event")).count();
        assertTrue(count >= 2,
                "Expected at least 2 'Received event' lines in mqtt-subscriber logs, found " + count);
    }
}
