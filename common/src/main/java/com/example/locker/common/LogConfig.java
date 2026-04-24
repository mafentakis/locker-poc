package com.example.locker.common;

import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public final class LogConfig {

    private LogConfig() {}

    public static void init() {
        try (InputStream is = LogConfig.class.getClassLoader().getResourceAsStream("logging.properties")) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
            }
        } catch (Exception e) {
            Logger.getLogger(LogConfig.class.getName()).warning("Failed to load logging.properties: " + e.getMessage());
        }
    }
}
