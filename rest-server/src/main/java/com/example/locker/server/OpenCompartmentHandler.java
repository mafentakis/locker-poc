package com.example.locker.server;

import com.example.locker.common.JsonMapper;
import com.example.locker.common.dto.ErrorResponseTo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OpenCompartmentHandler implements HttpHandler {

    private static final Logger LOG = Logger.getLogger(OpenCompartmentHandler.class.getName());

    private static final Pattern PATH_PATTERN = Pattern.compile(
            "/api/(v\\d{3})/locker/([^/]+)/compartment/([^/]+)/open");
    private static final Pattern COMPARTMENT_ID_REGEX = Pattern.compile(
            "^compartmentInfo@[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private final MqttEventEmitter emitter;

    public OpenCompartmentHandler(MqttEventEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath();
            Matcher m = PATH_PATTERN.matcher(path);
            if (!m.matches()) {
                sendError(exchange, 400, "INVALID_PATH", "Path does not match expected pattern: " + path);
                return;
            }

            String lockerId = m.group(2);
            String compartmentId = m.group(3);

            // Validate lockerId as UUID
            try {
                UUID.fromString(lockerId);
            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, "INVALID_LOCKER_ID",
                        "Path parameter 'lockerId' is not a valid UUID.");
                return;
            }

            // Validate compartmentId
            if (!COMPARTMENT_ID_REGEX.matcher(compartmentId).matches()) {
                sendError(exchange, 400, "INVALID_COMPARTMENT_ID",
                        "Path parameter 'compartmentId' is not a valid compartment ID.");
                return;
            }

            // Required headers
            String clientVersion = getRequiredHeader(exchange, "Client-Version");
            if (clientVersion == null) return;
            String lockToken = getRequiredHeader(exchange, "Lock-Token");
            if (lockToken == null) return;
            String idempotencyKey = getRequiredHeader(exchange, "Idempotency-Key");
            if (idempotencyKey == null) return;

            LOG.info("Received openCompartment lockerId=" + lockerId
                    + " compartmentId=" + compartmentId
                    + " idempotencyKey=" + idempotencyKey
                    + " lockToken=***"
                    + " clientVersion=" + clientVersion);

            // 200 OK (empty body)
            exchange.sendResponseHeaders(200, -1);

            // Emit MQTT event (best-effort)
            emitter.publishCompartmentOpened(lockerId, compartmentId);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unexpected error in openCompartment", e);
            sendError(exchange, 500, "INTERNAL_ERROR", e.getMessage());
        }
    }

    private String getRequiredHeader(HttpExchange exchange, String name) throws IOException {
        String value = exchange.getRequestHeaders().getFirst(name);
        if (value == null || value.isBlank()) {
            sendError(exchange, 400, "MISSING_HEADER",
                    "Required header '" + name + "' is missing.");
            return null;
        }
        return value;
    }

    private void sendError(HttpExchange exchange, int status, String code, String message) throws IOException {
        var error = new ErrorResponseTo(code, message, Instant.now());
        byte[] body = JsonMapper.instance().writeValueAsBytes(error);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
