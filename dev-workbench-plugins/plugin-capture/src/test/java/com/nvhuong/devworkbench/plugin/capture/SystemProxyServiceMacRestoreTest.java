package com.nvhuong.devworkbench.plugin.capture;

import com.nvhuong.devworkbench.plugin.api.PluginStorage;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class SystemProxyServiceMacRestoreTest {

    /**
     * A network service can disappear while capturing (USB/Thunderbolt adapter unplugged, VPN removed).
     * Restoring must still put every remaining service back, otherwise e.g. Wi-Fi keeps pointing at the
     * stopped capture proxy and the machine loses connectivity.
     */
    @Test
    public void restoresRemainingServicesWhenOneServiceDisappeared() throws Exception {
        List<String> survivors = List.of("Wi-Fi", "Ethernet", "Thunderbolt Bridge", "iPhone USB",
                "Bluetooth PAN", "VPN A", "VPN B", "Ethernet 2");
        List<String> all = new ArrayList<>();
        all.add("USB 10/100/1000 LAN");
        all.addAll(survivors);
        FakeNetworkSetup networkSetup = new FakeNetworkSetup(all);
        SystemProxyService service = new SystemProxyService(networkSetup, "Mac OS X", false);
        service.configureStorage(new MemoryStorage());

        service.enable("127.0.0.1", 8888);
        assertTrue(networkSetup.state("Wi-Fi").webEnabled, "capture proxy should be applied to Wi-Fi");

        networkSetup.remove("USB 10/100/1000 LAN");

        expectThrows(Exception.class, service::disable);

        for (String name : survivors) {
            FakeNetworkSetup.ServiceState state = networkSetup.state(name);
            assertFalse(state.webEnabled, name + " HTTP proxy must be restored to off");
            assertFalse(state.secureEnabled, name + " HTTPS proxy must be restored to off");
            assertEquals(state.bypass, List.of("*.local"), name + " bypass domains must be restored");
        }
    }

    @Test
    public void restoresAllServicesWhenNothingFails() throws Exception {
        FakeNetworkSetup networkSetup = new FakeNetworkSetup(List.of("Ethernet", "Wi-Fi"));
        SystemProxyService service = new SystemProxyService(networkSetup, "Mac OS X", false);
        service.configureStorage(new MemoryStorage());

        service.enable("127.0.0.1", 8888);
        service.disable();

        for (String name : List.of("Ethernet", "Wi-Fi")) {
            assertFalse(networkSetup.state(name).webEnabled, name);
            assertFalse(networkSetup.state(name).secureEnabled, name);
        }
    }

    /** Minimal in-memory model of macOS {@code networksetup} for the commands SystemProxyService uses. */
    private static final class FakeNetworkSetup implements SystemProxyService.CommandRunner {
        static final class ServiceState {
            boolean webEnabled;
            String webServer = "";
            int webPort;
            boolean secureEnabled;
            String secureServer = "";
            int securePort;
            boolean autoProxyEnabled;
            boolean autoDiscovery;
            List<String> bypass = new ArrayList<>(List.of("*.local"));
        }

        private final Map<String, ServiceState> services = new LinkedHashMap<>();
        private final Set<String> removed = ConcurrentHashMap.newKeySet();

        FakeNetworkSetup(List<String> names) {
            names.forEach(name -> services.put(name, new ServiceState()));
        }

        ServiceState state(String name) {
            return services.get(name);
        }

        void remove(String name) {
            removed.add(name);
        }

        @Override
        public SystemProxyService.CommandResult run(List<String> command) {
            String operation = command.size() > 1 ? command.get(1) : "";
            if ("-listallnetworkservices".equals(operation)) {
                List<String> lines = new ArrayList<>();
                lines.add("An asterisk (*) denotes that a network service is disabled.");
                services.keySet().stream().filter(name -> !removed.contains(name)).forEach(lines::add);
                return ok(lines);
            }
            String name = command.size() > 2 ? command.get(2) : "";
            if (removed.contains(name) || !services.containsKey(name)) {
                return new SystemProxyService.CommandResult(4,
                        List.of("** Error: The parameters were not valid. " + name + " is not a recognized network service."));
            }
            ServiceState state = services.get(name);
            switch (operation) {
                case "-getwebproxy" -> {
                    return ok(proxyLines(state.webEnabled, state.webServer, state.webPort));
                }
                case "-getsecurewebproxy" -> {
                    return ok(proxyLines(state.secureEnabled, state.secureServer, state.securePort));
                }
                case "-getproxyautodiscovery" -> {
                    return ok(List.of("Auto Proxy Discovery: " + (state.autoDiscovery ? "On" : "Off")));
                }
                case "-getautoproxyurl" -> {
                    return ok(List.of("URL: (null)", "Enabled: " + (state.autoProxyEnabled ? "Yes" : "No")));
                }
                case "-getproxybypassdomains" -> {
                    return ok(state.bypass.isEmpty()
                            ? List.of("There aren't any bypass domains set on " + name + ".")
                            : List.copyOf(state.bypass));
                }
                case "-setwebproxy" -> {
                    state.webServer = command.get(3);
                    state.webPort = Integer.parseInt(command.get(4));
                    state.webEnabled = true;
                }
                case "-setsecurewebproxy" -> {
                    state.secureServer = command.get(3);
                    state.securePort = Integer.parseInt(command.get(4));
                    state.secureEnabled = true;
                }
                case "-setwebproxystate" -> state.webEnabled = "on".equals(command.get(3));
                case "-setsecurewebproxystate" -> state.secureEnabled = "on".equals(command.get(3));
                case "-setautoproxystate" -> state.autoProxyEnabled = "on".equals(command.get(3));
                case "-setproxyautodiscovery" -> state.autoDiscovery = "on".equals(command.get(3));
                case "-setautoproxyurl" -> { }
                case "-setproxybypassdomains" -> {
                    List<String> domains = command.subList(3, command.size());
                    state.bypass = domains.equals(List.of("Empty")) ? new ArrayList<>() : new ArrayList<>(domains);
                }
                default -> {
                    return new SystemProxyService.CommandResult(1, List.of("unsupported " + operation));
                }
            }
            return ok(List.of());
        }

        private static List<String> proxyLines(boolean enabled, String server, int port) {
            return List.of("Enabled: " + (enabled ? "Yes" : "No"), "Server: " + server, "Port: " + port,
                    "Authenticated Proxy Enabled: 0");
        }

        private static SystemProxyService.CommandResult ok(List<String> lines) {
            return new SystemProxyService.CommandResult(0, lines);
        }
    }

    private static final class MemoryStorage implements PluginStorage {
        private final Map<String, String> files = new HashMap<>();

        @Override
        public Optional<String> readString(String relativePath) {
            return Optional.ofNullable(files.get(relativePath));
        }

        @Override
        public void writeString(String relativePath, String content) {
            files.put(relativePath, content);
        }

        @Override
        public void delete(String relativePath) {
            files.remove(relativePath);
        }

        @Override
        public Path dataDirectory() {
            return Path.of("");
        }
    }
}
