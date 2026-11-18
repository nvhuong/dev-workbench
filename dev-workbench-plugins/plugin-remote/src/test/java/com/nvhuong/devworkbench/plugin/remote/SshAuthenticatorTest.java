package com.nvhuong.devworkbench.plugin.remote;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.keyboard.DefaultKeyboardInteractiveAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class SshAuthenticatorTest {
    private SshServer server;

    @AfterMethod(alwaysRun = true)
    public void stopServer() throws IOException {
        if (server != null) server.stop(true);
        server = null;
    }

    @Test public void authenticatesWithPasswordMethod() throws Exception {
        startServer(true, false);
        assertTrue(authenticate("secret"));
    }

    @Test public void fallsBackToKeyboardInteractiveWhenPasswordMethodIsDisabled() throws Exception {
        startServer(false, true);
        assertTrue(authenticate("secret"));
    }

    @Test public void rejectsWrongPassword() throws Exception {
        startServer(true, true);
        expectThrows(Exception.class, () -> authenticate("wrong"));
    }

    @Test public void appliesConnectTimeout() {
        SSHClient client = new SSHClient();
        SshAuthenticator.configure(client);
        assertEquals(client.getConnectTimeout(), SshAuthenticator.CONNECT_TIMEOUT_MILLIS);
    }

    @Test public void describesExceptionsWithoutMessage() {
        assertEquals(SshAuthenticator.describe(new IOException((String) null)), "IOException");
        assertEquals(SshAuthenticator.describe(new RuntimeException(new IOException("refused"))),
                "java.io.IOException: refused");
        assertEquals(SshAuthenticator.describe(new IllegalStateException("", new IOException("refused"))), "refused");
    }

    private void startServer(boolean password, boolean keyboardInteractive) throws IOException {
        server = SshServer.setUpDefaultServer();
        server.setHost("127.0.0.1");
        server.setPort(0);
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Files.createTempFile("hostkey", ".ser")));
        if (password) {
            server.setPasswordAuthenticator((user, pass, session) -> "dev".equals(user) && "secret".equals(pass));
        } else {
            server.setPasswordAuthenticator(null);
        }
        if (keyboardInteractive) {
            server.setPasswordAuthenticator(server.getPasswordAuthenticator() != null
                    ? server.getPasswordAuthenticator()
                    : (user, pass, session) -> "dev".equals(user) && "secret".equals(pass));
            server.setKeyboardInteractiveAuthenticator(DefaultKeyboardInteractiveAuthenticator.INSTANCE);
            if (!password) {
                server.setUserAuthFactories(java.util.List.of(
                        org.apache.sshd.server.auth.keyboard.UserAuthKeyboardInteractiveFactory.INSTANCE));
            }
        } else {
            server.setKeyboardInteractiveAuthenticator(null);
        }
        server.start();
    }

    private boolean authenticate(String password) throws Exception {
        try (SSHClient client = new SSHClient()) {
            SshAuthenticator.configure(client);
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.connect("127.0.0.1", server.getPort());
            SshAuthenticator.authenticate(client, "dev", password, "");
            return client.isAuthenticated();
        }
    }
}
