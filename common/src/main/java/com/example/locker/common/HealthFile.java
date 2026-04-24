package com.example.locker.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Writes sentinel files for Docker healthchecks.
 */
public final class HealthFile {

    private static final Logger LOG = Logger.getLogger(HealthFile.class.getName());

    private HealthFile() {}

    public static void markReady(String serviceName) {
        write("/tmp/" + serviceName + ".ready");
    }

    public static void markDone(String serviceName) {
        write("/tmp/" + serviceName + ".done");
    }

    private static void write(String path) {
        try {
            Files.writeString(Path.of(path), "ok");
            LOG.info("Health file written: " + path);
        } catch (IOException e) {
            LOG.warning("Failed to write health file " + path + ": " + e.getMessage());
        }
    }
}
