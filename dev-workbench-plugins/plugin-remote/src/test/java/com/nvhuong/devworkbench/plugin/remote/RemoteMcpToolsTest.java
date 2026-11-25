package com.nvhuong.devworkbench.plugin.remote;

import com.nvhuong.devworkbench.plugin.api.PluginStorage;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.common.KeyType;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class RemoteMcpToolsTest {
    private SshServer server;

    @AfterMethod(alwaysRun = true)
    public void stopServer() throws IOException {
        if (server != null) server.stop(true);
        server = null;
    }

    @Test
    public void listsProfilesWithoutPasswords() {
        PluginStorage storage = storage(profileLine("Prod", "10.0.0.5", 2222, "deploy", "", "pw-secret"));

        Map<String, Object> result = RemoteMcpTools.listProfiles(storage);

        assertEquals(result.get("count"), 1);
        assertTrue(result.toString().contains("10.0.0.5"));
        assertTrue(result.toString().contains("auth=password"));
        assertFalse(result.toString().contains("pw-secret"));
    }

    @Test
    public void runsACommandOnATrustedHostOnly() throws Exception {
        startServer();
        int port = server.getPort();
        PluginStorage storage = storage(profileLine("Local", "127.0.0.1", port, "dev", "", "secret"));
        Path knownHosts = Files.createTempFile("known_hosts", "");

        IllegalStateException unknown = expectThrows(IllegalStateException.class, () -> RemoteMcpTools.exec(storage, knownHosts,
                Map.of("profile", "local", "command", "hello")));
        assertTrue(unknown.getMessage().contains("Remote tool"), unknown.getMessage());

        PublicKey hostKey = server.getKeyPairProvider().loadKeys(null).iterator().next().getPublic();
        String keyData = Base64.getEncoder().encodeToString(new Buffer.PlainBuffer().putPublicKey(hostKey).getCompactData());
        Files.writeString(knownHosts, "[127.0.0.1]:" + port + " " + KeyType.fromKey(hostKey) + " " + keyData + "\n");

        Map<String, Object> result = RemoteMcpTools.exec(storage, knownHosts, Map.of("profile", "Local", "command", "hello"));

        assertEquals(result.get("stdout"), "out:hello");
        assertEquals(result.get("stderr"), "err");
        assertEquals(result.get("exitStatus"), 3);
        assertEquals(result.get("timedOut"), false);
    }

    @Test
    public void clampsTimeouts() {
        assertEquals(RemoteMcpTools.timeoutSeconds(Map.of()), RemoteMcpTools.DEFAULT_TIMEOUT_SECONDS);
        assertEquals(RemoteMcpTools.timeoutSeconds(Map.of("timeoutSeconds", 9999)), RemoteMcpTools.MAX_TIMEOUT_SECONDS);
        assertEquals(RemoteMcpTools.timeoutSeconds(Map.of("timeoutSeconds", 0)), 1);
        assertEquals(RemoteMcpTools.tools(storage(), () -> null).stream()
                .filter(tool -> !tool.readOnly()).map(tool -> tool.name()).toList(), List.of("remote_exec"));
    }

    private void startServer() throws IOException {
        server = SshServer.setUpDefaultServer();
        server.setHost("127.0.0.1");
        server.setPort(0);
        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Files.createTempFile("hostkey", ".ser")));
        server.setPasswordAuthenticator((user, pass, session) -> "dev".equals(user) && "secret".equals(pass));
        server.setCommandFactory((channel, command) -> new FakeCommand(command));
        server.start();
    }

    /** Writes "out:<command>" to stdout, "err" to stderr and exits with status 3. */
    private static final class FakeCommand implements Command {
        private final String command;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback exit;

        FakeCommand(String command) {
            this.command = command;
        }

        @Override public void setInputStream(InputStream in) { }
        @Override public void setOutputStream(OutputStream out) { this.out = out; }
        @Override public void setErrorStream(OutputStream err) { this.err = err; }
        @Override public void setExitCallback(ExitCallback callback) { this.exit = callback; }

        @Override
        public void start(ChannelSession channel, Environment env) throws IOException {
            out.write(("out:" + command).getBytes(StandardCharsets.UTF_8));
            out.flush();
            err.write("err".getBytes(StandardCharsets.UTF_8));
            err.flush();
            exit.onExit(3);
        }

        @Override public void destroy(ChannelSession channel) { }
    }

    private static String profileLine(String name, String host, int port, String user, String keyFile, String password) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        java.util.function.Function<String, String> e = value -> encoder.encodeToString(value.getBytes(StandardCharsets.UTF_8));
        return e.apply(name) + "\t" + e.apply(host) + "\t" + port + "\t" + e.apply(user) + "\t" + e.apply(keyFile)
                + "\ttrue\t" + e.apply(password) + "\n";
    }

    private static PluginStorage storage(String... lines) {
        String content = String.join("", lines);
        return new PluginStorage() {
            @Override public Optional<String> readString(String path) {
                return RemoteMcpTools.PROFILE_FILE.equals(path) ? Optional.of(content) : Optional.empty();
            }
            @Override public void writeString(String path, String value) { }
            @Override public void delete(String path) { }
            @Override public Path dataDirectory() { return Path.of(System.getProperty("java.io.tmpdir")); }
        };
    }
}
