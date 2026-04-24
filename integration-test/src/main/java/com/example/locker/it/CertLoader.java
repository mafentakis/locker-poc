package com.example.locker.it;

import com.example.locker.common.tls.TlsContextFactory;

import javax.net.ssl.SSLContext;

/**
 * Loads TLS context from certs mounted at /certs (or CERTS_DIR env var).
 */
public final class CertLoader {

    private static final String PASSWORD = "changeit";
    private static final String CERTS_DIR = System.getenv().getOrDefault("CERTS_DIR", "/certs");

    private CertLoader() {}

    public static SSLContext clientSslContext() throws Exception {
        return TlsContextFactory.create(
                CERTS_DIR + "/rest-client-keystore.p12", PASSWORD,
                CERTS_DIR + "/truststore.p12", PASSWORD
        );
    }

    public static SSLContext trustOnlySslContext() throws Exception {
        return TlsContextFactory.createTrustOnly(
                CERTS_DIR + "/truststore.p12", PASSWORD
        );
    }
}

