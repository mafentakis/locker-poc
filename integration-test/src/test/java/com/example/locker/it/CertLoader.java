package com.example.locker.it;

import com.example.locker.common.tls.TlsContextFactory;

import javax.net.ssl.SSLContext;
import java.io.File;

/**
 * Loads TLS context from generated certs for integration tests.
 */
public final class CertLoader {

    private static final String PASSWORD = "changeit";

    private CertLoader() {}

    public static SSLContext clientSslContext() throws Exception {
        File certsDir = new File(DockerComposeStack.findProjectRoot(), "certs");
        return TlsContextFactory.create(
                new File(certsDir, "rest-client-keystore.p12").getAbsolutePath(), PASSWORD,
                new File(certsDir, "truststore.p12").getAbsolutePath(), PASSWORD
        );
    }

    public static SSLContext trustOnlySslContext() throws Exception {
        File certsDir = new File(DockerComposeStack.findProjectRoot(), "certs");
        return TlsContextFactory.createTrustOnly(
                new File(certsDir, "truststore.p12").getAbsolutePath(), PASSWORD
        );
    }
}
