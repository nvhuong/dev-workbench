package com.nvhuong.devworkbench.plugin.remote;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import java.io.IOException;

/** Connection and authentication rules shared by every SSH terminal session. */
final class SshAuthenticator {
    static final int CONNECT_TIMEOUT_MILLIS = 15_000;

    private SshAuthenticator() { }

    /** Applies timeouts so unreachable hosts fail quickly instead of hanging on the OS TCP timeout. */
    static void configure(SSHClient client) {
        client.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    }

    /**
     * Authenticates with the key file (the password field acts as its passphrase), otherwise with the password,
     * otherwise with the default keys in {@code ~/.ssh}.
     */
    static void authenticate(SSHClient client, String user, String password, String keyFile) throws IOException {
        String secret = password == null ? "" : password;
        if (keyFile != null && !keyFile.isBlank()) {
            KeyProvider provider = secret.isBlank() ? client.loadKeys(keyFile) : client.loadKeys(keyFile, secret);
            client.authPublickey(user, provider);
        } else if (!secret.isBlank()) {
            // sshj tries both the "password" and "keyboard-interactive" methods.
            client.authPassword(user, secret);
        } else {
            client.authPublickey(user);
        }
    }

    static String describe(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && (current.getMessage() == null || current.getMessage().isBlank())) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
