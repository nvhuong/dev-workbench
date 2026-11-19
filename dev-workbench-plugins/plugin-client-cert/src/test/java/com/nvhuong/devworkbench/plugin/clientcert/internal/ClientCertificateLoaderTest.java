package com.nvhuong.devworkbench.plugin.clientcert.internal;

import com.nvhuong.devworkbench.model.ClientCertificate;
import org.testng.annotations.Test;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ClientCertificateLoaderTest {

    /**
     * The settings dialog has a single "Password" field that is stored as {@code certPassword} for both PFX and
     * PEM entries, so an encrypted PEM private key must be decrypted with it.
     */
    @Test
    public void loadsEncryptedPemKeyWithPasswordEnteredInSettingsDialog() throws Exception {
        ClientCertificate certificate = new ClientCertificate();
        certificate.setCertType(ClientCertificate.CERT_TYPE_PEM);
        certificate.setCertPath(resource("certs/client.crt"));
        certificate.setKeyPath(resource("certs/client-encrypted.key"));
        certificate.setCertPassword("changeit");

        assertHasClientIdentity(ClientCertificateLoader.createKeyManagers(certificate));
    }

    @Test
    public void prefersExplicitKeyPasswordForPem() throws Exception {
        ClientCertificate certificate = new ClientCertificate();
        certificate.setCertType(ClientCertificate.CERT_TYPE_PEM);
        certificate.setCertPath(resource("certs/client.crt"));
        certificate.setKeyPath(resource("certs/client-encrypted.key"));
        certificate.setCertPassword("unused");
        certificate.setKeyPassword("changeit");

        assertHasClientIdentity(ClientCertificateLoader.createKeyManagers(certificate));
    }

    private static void assertHasClientIdentity(KeyManager[] keyManagers) {
        assertEquals(keyManagers.length, 1);
        X509KeyManager keyManager = (X509KeyManager) keyManagers[0];
        String[] aliases = keyManager.getClientAliases("RSA", null);
        assertNotNull(aliases, "an RSA client alias should be available");
        assertTrue(aliases.length > 0);
        assertNotNull(keyManager.getPrivateKey(aliases[0]));
    }

    private static String resource(String name) throws URISyntaxException {
        return Path.of(Objects.requireNonNull(ClientCertificateLoaderTest.class.getClassLoader().getResource(name))
                .toURI()).toString();
    }
}
