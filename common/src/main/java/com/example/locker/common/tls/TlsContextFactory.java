package com.example.locker.common.tls;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.logging.Logger;

/**
 * Builds an SSLContext from PKCS#12 keystore + truststore.
 * Reads paths/passwords from environment variables.
 */
public final class TlsContextFactory {

    private static final Logger LOG = Logger.getLogger(TlsContextFactory.class.getName());

    private TlsContextFactory() {}

    public static SSLContext create() throws Exception {
        return create(
                env("TLS_KEYSTORE_PATH"),
                env("TLS_KEYSTORE_PASSWORD"),
                env("TLS_TRUSTSTORE_PATH"),
                env("TLS_TRUSTSTORE_PASSWORD")
        );
    }

    public static SSLContext create(String keystorePath, String keystorePassword,
                                    String truststorePath, String truststorePassword) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (var in = new FileInputStream(keystorePath)) {
            ks.load(in, keystorePassword.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keystorePassword.toCharArray());

        KeyStore ts = KeyStore.getInstance("PKCS12");
        try (var in = new FileInputStream(truststorePath)) {
            ts.load(in, truststorePassword.toCharArray());
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SSLContext ctx = SSLContext.getInstance("TLSv1.3");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        LOG.info("SSLContext created (TLSv1.3) keystore=" + keystorePath + " truststore=" + truststorePath);
        return ctx;
    }

    /**
     * Creates an SSLContext with truststore only (no client cert). For negative testing.
     */
    public static SSLContext createTrustOnly(String truststorePath, String truststorePassword) throws Exception {
        KeyStore ts = KeyStore.getInstance("PKCS12");
        try (var in = new FileInputStream(truststorePath)) {
            ts.load(in, truststorePassword.toCharArray());
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SSLContext ctx = SSLContext.getInstance("TLSv1.3");
        ctx.init(null, tmf.getTrustManagers(), null);
        return ctx;
    }

    private static String env(String name) {
        String val = System.getenv(name);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException("Required environment variable not set: " + name);
        }
        return val;
    }
}
