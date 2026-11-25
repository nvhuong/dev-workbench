package com.nvhuong.devworkbench.plugin.remote;

import com.nvhuong.devworkbench.plugin.api.McpToolContribution;
import com.nvhuong.devworkbench.plugin.api.PluginContext;
import com.nvhuong.devworkbench.plugin.api.PluginStorage;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * MCP tools of the Remote plugin. Servers are addressed by the SSH profiles saved in the Remote tool, so passwords
 * and key passphrases never pass through the AI client. Running a command is a write tool, and only hosts already
 * trusted in ~/.ssh/known_hosts are accepted: an unknown host key is rejected instead of being trusted silently.
 */
final class RemoteMcpTools {
    static final String PROFILE_FILE = "profiles.json";
    static final int DEFAULT_TIMEOUT_SECONDS = 60;
    static final int MAX_TIMEOUT_SECONDS = 300;
    static final int MAX_OUTPUT_BYTES = 100_000;

    record Profile(String name, String host, int port, String user, String keyFile, String password) {
        String auth() {
            if (keyFile != null && !keyFile.isBlank()) return "key";
            if (password != null && !password.isBlank()) return "password";
            return "agent-or-default-keys";
        }
    }

    private RemoteMcpTools() {
    }

    static void register(PluginContext context) {
        for (McpToolContribution tool : tools(context.storage(), RemoteMcpTools::defaultKnownHosts)) {
            context.registerMcpTool(tool);
        }
    }

    static List<McpToolContribution> tools(PluginStorage storage, Supplier<Path> knownHosts) {
        return List.of(
                new McpToolContribution("remote_list_profiles", "List SSH server profiles",
                        "Lists the SSH servers saved in the DevWorkbench Remote tool (host, port, user, auth type). "
                                + "Passwords are never returned.",
                        schema(Map.of(), List.of()), true, arguments -> listProfiles(storage)),
                new McpToolContribution("remote_exec", "Run a command over SSH",
                        "Runs one shell command on a saved SSH server and returns exit status, stdout and stderr "
                                + "(100 KB each at most). The host must already be trusted in ~/.ssh/known_hosts "
                                + "(connect once from the Remote tool). Only available with --allow-writes.",
                        schema(Map.of(
                                "profile", Map.of("type", "string", "description", "Profile name (see remote_list_profiles)."),
                                "command", Map.of("type", "string", "description", "Shell command to run."),
                                "timeoutSeconds", Map.of("type", "integer", "minimum", 1, "maximum", MAX_TIMEOUT_SECONDS,
                                        "description", "Stop waiting after this many seconds (default 60).")),
                                List.of("profile", "command")),
                        false, arguments -> exec(storage, knownHosts.get(), arguments)));
    }

    static Map<String, Object> listProfiles(PluginStorage storage) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Profile profile : readProfiles(storage)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", profile.name());
            item.put("host", profile.host());
            item.put("port", profile.port());
            item.put("user", profile.user());
            item.put("auth", profile.auth());
            items.add(item);
        }
        return Map.of("profiles", items, "count", items.size());
    }

    static Map<String, Object> exec(PluginStorage storage, Path knownHosts, Map<String, Object> arguments) throws Exception {
        String name = text(arguments, "profile");
        String command = text(arguments, "command");
        if (name == null) throw new IllegalArgumentException("profile is required");
        if (command == null) throw new IllegalArgumentException("command is required");
        Profile profile = readProfiles(storage).stream()
                .filter(candidate -> candidate.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown SSH profile: " + name + ". Use remote_list_profiles."));
        int timeout = timeoutSeconds(arguments);
        if (knownHosts == null || !Files.isRegularFile(knownHosts)) {
            throw new IllegalStateException("No known_hosts file. Connect once from the Remote tool to trust " + profile.host());
        }
        long started = System.nanoTime();
        try (SSHClient client = new SSHClient()) {
            SshAuthenticator.configure(client);
            client.addHostKeyVerifier(new OpenSSHKnownHosts(knownHosts.toFile()));
            try {
                client.connect(profile.host(), profile.port());
            } catch (net.schmizz.sshj.transport.TransportException hostKey) {
                throw new IllegalStateException("Could not connect to " + profile.host() + ": "
                        + SshAuthenticator.describe(hostKey) + ". If the host key is not trusted yet, connect once "
                        + "from the Remote tool and accept it.", hostKey);
            }
            SshAuthenticator.authenticate(client, profile.user(), profile.password(), profile.keyFile());
            try (Session session = client.startSession()) {
                Session.Command running = session.exec(command);
                Capture stdout = new Capture(running.getInputStream());
                Capture stderr = new Capture(running.getErrorStream());
                stdout.start();
                stderr.start();
                running.join(timeout, TimeUnit.SECONDS);
                boolean timedOut = running.isOpen();
                if (timedOut) running.close();
                stdout.join(2000);
                stderr.join(2000);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("profile", profile.name());
                result.put("host", profile.host());
                result.put("exitStatus", running.getExitStatus());
                result.put("timedOut", timedOut);
                result.put("elapsedMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
                result.put("stdout", stdout.text());
                result.put("stdoutTruncated", stdout.truncated);
                result.put("stderr", stderr.text());
                result.put("stderrTruncated", stderr.truncated);
                return result;
            }
        }
    }

    static List<Profile> readProfiles(PluginStorage storage) {
        List<Profile> result = new ArrayList<>();
        try {
            for (String line : storage.readString(PROFILE_FILE).orElse("").lines().toList()) {
                String[] fields = line.split("\\t", -1);
                if (fields.length != 5 && fields.length != 7) continue;
                result.add(new Profile(decode(fields[0]), decode(fields[1]), Integer.parseInt(fields[2]),
                        decode(fields[3]), decode(fields[4]), fields.length == 7 ? decode(fields[6]) : ""));
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not read SSH profiles: " + exception.getMessage(), exception);
        }
        return result;
    }

    static int timeoutSeconds(Map<String, Object> arguments) {
        Object value = arguments == null ? null : arguments.get("timeoutSeconds");
        int seconds = value == null ? DEFAULT_TIMEOUT_SECONDS
                : value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value).trim());
        return Math.max(1, Math.min(MAX_TIMEOUT_SECONDS, seconds));
    }

    private static Path defaultKnownHosts() {
        return Path.of(System.getProperty("user.home"), ".ssh", "known_hosts");
    }

    /** Reads a stream fully in the background but keeps only the first MAX_OUTPUT_BYTES. */
    private static final class Capture extends Thread {
        private final InputStream input;
        private final ByteArrayOutputStream kept = new ByteArrayOutputStream();
        private volatile boolean truncated;

        Capture(InputStream input) {
            super("mcp-ssh-output");
            setDaemon(true);
            this.input = input;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[8192];
            try {
                int read;
                while ((read = input.read(buffer)) != -1) {
                    int room = MAX_OUTPUT_BYTES - kept.size();
                    if (room > 0) kept.write(buffer, 0, Math.min(room, read));
                    if (read > room) truncated = true;
                }
            } catch (IOException ignored) {
                // channel closed (timeout or disconnect): keep what was read
            }
        }

        String text() {
            synchronized (kept) {
                return kept.toString(StandardCharsets.UTF_8);
            }
        }
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String text(Map<String, Object> arguments, String name) {
        Object value = arguments == null ? null : arguments.get(name);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }
}
