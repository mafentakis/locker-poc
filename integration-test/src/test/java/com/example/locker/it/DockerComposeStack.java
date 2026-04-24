package com.example.locker.it;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * JUnit 5 extension that starts/stops the Docker Compose stack.
 */
public class DockerComposeStack implements BeforeAllCallback, AfterAllCallback {

    private static final Logger LOG = Logger.getLogger(DockerComposeStack.class.getName());
    private static final Duration TIMEOUT = Duration.ofSeconds(120);

    private File projectDir;

    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception {
        if ("true".equals(System.getProperty("it.skipStack"))) {
            LOG.info("Skipping stack start (it.skipStack=true)");
            return;
        }

        projectDir = findProjectRoot();
        LOG.info("Project root: " + projectDir);

        // Generate certs
        run(projectDir, "bash", "scripts/gen-certs.sh");

        // Start stack
        run(projectDir, "docker", "compose", "up", "-d", "--build");

        // Wait for healthy
        waitForHealthy();
    }

    @Override
    public void afterAll(ExtensionContext ctx) throws Exception {
        if ("true".equals(System.getProperty("it.keepStack"))) {
            LOG.info("Keeping stack alive (it.keepStack=true)");
            return;
        }
        if (projectDir == null) return;

        // Dump logs
        Path logsFile = projectDir.toPath().resolve("integration-test/target/compose-logs.txt");
        Files.createDirectories(logsFile.getParent());
        ProcessBuilder pb = new ProcessBuilder("docker", "compose", "logs", "--no-color")
                .directory(projectDir).redirectErrorStream(true);
        Process p = pb.start();
        String logs = new String(p.getInputStream().readAllBytes());
        Files.writeString(logsFile, logs);
        p.waitFor(30, TimeUnit.SECONDS);

        // Tear down
        run(projectDir, "docker", "compose", "down", "-v");
    }

    private void waitForHealthy() throws Exception {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            ProcessBuilder pb = new ProcessBuilder("docker", "compose", "ps", "--format", "{{.Service}} {{.State}} {{.Health}}")
                    .directory(projectDir).redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes());
            p.waitFor(10, TimeUnit.SECONDS);

            LOG.fine("docker compose ps:\n" + output);

            boolean allGood = true;
            for (String line : output.strip().split("\n")) {
                if (line.isBlank()) continue;
                String[] parts = line.trim().split("\\s+");
                String service = parts[0];
                String state = parts.length > 1 ? parts[1] : "";
                String health = parts.length > 2 ? parts[2] : "";

                if (service.equals("cert-init") || service.equals("rest-client") || service.equals("mqtt-publisher")) {
                    // One-shot: must be exited
                    if (!state.contains("exited")) {
                        allGood = false;
                    }
                } else {
                    // Long-running: must be healthy
                    if (!"healthy".equalsIgnoreCase(health)) {
                        allGood = false;
                    }
                }
            }

            if (allGood) {
                LOG.info("All services healthy/completed.");
                return;
            }
            Thread.sleep(3000);
        }
        throw new RuntimeException("Timed out waiting for Docker Compose stack to become healthy");
    }

    private static void run(File dir, String... cmd) throws Exception {
        LOG.info("Running: " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd).directory(dir)
                .redirectErrorStream(true).inheritIO();
        Process p = pb.start();
        if (!p.waitFor(300, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("Command timed out: " + String.join(" ", cmd));
        }
        if (p.exitValue() != 0) {
            throw new RuntimeException("Command failed (exit " + p.exitValue() + "): " + String.join(" ", cmd));
        }
    }

    static File findProjectRoot() {
        // Walk up from CWD looking for docker-compose.yml
        File dir = new File(System.getProperty("user.dir"));
        while (dir != null) {
            if (new File(dir, "docker-compose.yml").exists()) return dir;
            dir = dir.getParentFile();
        }
        throw new RuntimeException("Cannot find project root (docker-compose.yml not found)");
    }
}
