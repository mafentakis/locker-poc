package com.example.locker.client;

import com.example.locker.common.HealthFile;
import com.example.locker.common.tls.TlsContextFactory;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.logging.Logger;

public final class RestClientMain {

    private static final Logger LOG = Logger.getLogger(RestClientMain.class.getName());

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.logging.config.file",
                RestClientMain.class.getClassLoader().getResource("logging.properties").getPath());

        String serverUrl = System.getenv().getOrDefault("SERVER_URL", "https://rest-server:8443");
        String lockerId = System.getenv().getOrDefault("LOCKER_ID", "f47ac10b-58cc-4372-a567-0e02b2c3d479");
        String compartmentId = System.getenv().getOrDefault("COMPARTMENT_ID",
                "compartmentInfo@d4e5f6a7-b8c9-0123-defa-234567890123");
        String apiVersion = System.getenv().getOrDefault("API_VERSION", "v010");
        String clientVersion = System.getenv().getOrDefault("CLIENT_VERSION", "1.2.6");

        String url = serverUrl + "/api/" + apiVersion + "/locker/" + lockerId
                + "/compartment/" + compartmentId + "/open";

        SSLContext sslContext = TlsContextFactory.create();

        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Client-Version", clientVersion)
                .header("Lock-Token", "gy12p6N1UC4WJp4H1tFhVqtlzgm7qYp4qVnq5p/PLu4=")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .build();

        LOG.info("Sending openCompartment to " + url);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        LOG.info("Response status=" + response.statusCode() + " body=" + response.body());

        HealthFile.markDone("rest-client");
        LOG.info("Done.");
    }
}
