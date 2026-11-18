package com.nvhuong.devworkbench.plugin.remote;

import com.nvhuong.devworkbench.plugin.api.PluginStorage;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

final class RemotePanel extends JPanel {
    private static final String PROFILE_FILE = "profiles.json";
    private static final String ACTIVE_PROFILE_FILE = "active-profile.txt";
    private static final Logger LOG = Logger.getLogger(RemotePanel.class.getName());

    private final PluginStorage storage;
    private final JComboBox<Profile> profiles = new JComboBox<>();
    private final JTextField name = new JTextField("Server");
    private final JTextField host = new JTextField();
    private final JTextField user = new JTextField();
    private final JTextField keyFile = new JTextField();
    private final JSpinner port = new JSpinner(new SpinnerNumberModel(22, 1, 65535, 1));
    private final JPasswordField password = new JPasswordField();
    private final javax.swing.JCheckBox savePassword = new javax.swing.JCheckBox(RemoteI18n.t("save_password"));
    private final JTabbedPane sessions = new JTabbedPane();
    private final java.util.Map<Component, SSHClient> clients = new ConcurrentHashMap<>();
    private boolean loadingProfiles;

    RemotePanel(PluginStorage storage) {
        this.storage = storage == null ? PluginStorage.noop() : storage;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel sidebar = createConnectionForm();
        sidebar.setPreferredSize(new java.awt.Dimension(310, 520));
        add(sidebar, BorderLayout.WEST);
        add(sessions, BorderLayout.CENTER);
        loadProfiles(null);
    }

    private JPanel createConnectionForm() {
        JPanel form = new JPanel(new MigLayout("insets 12, fillx, wrap 2", "[pref!][grow,fill]", "[]8[]8[]8[]8[]8[]8[]8[]8[]8[]"));
        form.setBorder(BorderFactory.createTitledBorder(RemoteI18n.t("hosts")));

        profiles.setRenderer((list, value, index, selected, focus) -> new javax.swing.DefaultListCellRenderer()
                .getListCellRendererComponent(list, value == null ? "" : value.name(), index, selected, focus));
        profiles.addActionListener(event -> {
            if (!loadingProfiles && profiles.getSelectedItem() instanceof Profile profile) apply(profile);
        });
        JButton save = new JButton(RemoteI18n.t("save"));
        JButton copy = new JButton(RemoteI18n.t("copy"));
        JButton delete = new JButton(RemoteI18n.t("delete"));
        JButton fresh = new JButton(RemoteI18n.t("new"));
        save.addActionListener(event -> saveProfile());
        copy.addActionListener(event -> copyProfile());
        delete.addActionListener(event -> deleteProfile());
        fresh.addActionListener(event -> clearForm());

        form.add(new JLabel(RemoteI18n.t("profiles"))); form.add(profiles, "growx,wrap");
        form.add(fresh, "span 2,split 4"); form.add(save); form.add(copy); form.add(delete, "wrap");
        addField(form, RemoteI18n.t("name"), name);
        addField(form, RemoteI18n.t("host"), host);
        addField(form, RemoteI18n.t("port"), port);
        addField(form, RemoteI18n.t("user"), user);
        addField(form, RemoteI18n.t("password"), password);
        form.add(new JLabel(RemoteI18n.t("key"))); form.add(keyFile, "growx,wmin 100,wrap");
        JButton browse = new JButton(RemoteI18n.t("browse"));
        browse.addActionListener(event -> chooseKey());
        form.add(browse, "span 2,align right,wrap");
        form.add(savePassword, "span 2,wrap");
        JButton connect = new JButton(RemoteI18n.t("connect"));
        connect.addActionListener(event -> connect());
        form.add(connect, "span 2,growx,h 34!,wrap");
        JLabel note = new JLabel(RemoteI18n.t("host_key"));
        note.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        form.add(note, "span 2,growx");
        return form;
    }

    private void addField(JPanel panel, String label, Component field) {
        panel.add(new JLabel(label));
        panel.add(field, "growx,wmin 120,wrap");
    }

    private void chooseKey() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            keyFile.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void connect() {
        Profile profile = connectionProfile();
        if (profile.host().isBlank() || profile.user().isBlank()) {
            JOptionPane.showMessageDialog(this, RemoteI18n.t("required"));
            return;
        }

        JLabel connectionState = new JLabel(RemoteI18n.t("connecting", profile.user(), profile.host()));
        connectionState.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JediTermWidget terminalWidget = new JediTermWidget(new DefaultSettingsProvider() {
            @Override public Font getTerminalFont() { return new Font(Font.MONOSPACED, Font.PLAIN, 14); }
            @Override public float getTerminalFontSize() { return 14f; }
            @Override public int getBufferMaxLinesCount() { return 20_000; }
        });
        JPanel terminal = new JPanel(new BorderLayout());
        terminal.add(connectionState, BorderLayout.NORTH);
        terminal.add(terminalWidget, BorderLayout.CENTER);
        String title = profile.name().isBlank() ? profile.host() : profile.name();
        sessions.addTab(title, terminal);
        sessions.setSelectedComponent(terminal);
        addClosableTab(title, terminal);

        SSHClient client = new SSHClient();
        SshAuthenticator.configure(client);
        clients.put(terminal, client);
        Thread worker = new Thread(() -> {
            try {
                OpenSSHKnownHosts knownHosts = createKnownHostsVerifier(profile);
                client.addHostKeyVerifier(knownHosts);
                client.connect(profile.host(), profile.port());
                SshAuthenticator.authenticate(client, profile.user(), profile.password(), profile.keyFile());

                Session session = client.startSession();
                session.allocatePTY("xterm-256color", 120, 40, 0, 0, java.util.Map.of());
                Session.Shell shell = session.startShell();
                SwingUtilities.invokeLater(() -> {
                    if (clients.get(terminal) != client) {
                        // The tab was closed while the connection was being established.
                        disconnectQuietly(client);
                        return;
                    }
                    connectionState.setText(RemoteI18n.t("connected", profile.user(), profile.host()));
                    terminalWidget.setTtyConnector(new SshTtyConnector(shell, title));
                    terminalWidget.start();
                    terminalWidget.requestFocusInWindow();
                });
                watchSessionEnd(terminal, client, shell, connectionState, profile);
            } catch (Exception exception) {
                boolean closedByUser = clients.get(terminal) != client;
                disconnectQuietly(client);
                clients.remove(terminal, client);
                if (closedByUser) return;
                LOG.log(Level.WARNING, "SSH session failed for " + profile.host(), exception);
                SwingUtilities.invokeLater(() -> connectionState.setText(
                        RemoteI18n.t("connection_error", SshAuthenticator.describe(exception))));
            }
        }, "ssh-session-" + profile.host());
        worker.setDaemon(true);
        worker.start();
    }

    /** Releases the SSH connection and tells the user when the remote shell exits (e.g. after {@code exit}). */
    private void watchSessionEnd(Component terminal, SSHClient client, Session.Shell shell, JLabel connectionState,
                                 Profile profile) {
        try {
            shell.join();
        } catch (Exception exception) {
            LOG.log(Level.FINE, "SSH shell wait ended", exception);
        }
        if (!clients.remove(terminal, client)) return;
        disconnectQuietly(client);
        SwingUtilities.invokeLater(() -> connectionState.setText(
                RemoteI18n.t("disconnected", profile.user(), profile.host())));
    }

    private static void disconnectQuietly(SSHClient client) {
        try { client.disconnect(); } catch (Exception exception) {
            LOG.log(Level.FINE, "SSH session close failed", exception);
        }
    }

    private OpenSSHKnownHosts createKnownHostsVerifier(Profile profile) throws Exception {
        Path sshDirectory = Path.of(System.getProperty("user.home"), ".ssh");
        Files.createDirectories(sshDirectory);
        Path knownHostsPath = sshDirectory.resolve("known_hosts");
        if (Files.notExists(knownHostsPath)) Files.createFile(knownHostsPath);
        try {
            Files.setPosixFilePermissions(sshDirectory,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
            Files.setPosixFilePermissions(knownHostsPath,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) { }

        return new OpenSSHKnownHosts(knownHostsPath.toFile()) {
            @Override
            protected boolean hostKeyUnverifiableAction(String hostKeyHost, java.security.PublicKey publicKey) {
                OpenSSHKnownHosts.HostEntry entry;
                try {
                    entry = new OpenSSHKnownHosts.HostEntry(null, hostKeyHost, KeyType.fromKey(publicKey), publicKey,
                            "Dev Workbench");
                } catch (Exception exception) {
                    LOG.log(Level.WARNING, "Could not prepare SSH host-key entry", exception);
                    return false;
                }
                java.util.concurrent.atomic.AtomicBoolean trusted = new java.util.concurrent.atomic.AtomicBoolean();
                Runnable confirm = () -> trusted.set(JOptionPane.showConfirmDialog(
                        RemotePanel.this,
                        RemoteI18n.t("host_key.trust_message", profile.host(), entry.getFingerprint()),
                        RemoteI18n.t("host_key.trust_title"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                ) == JOptionPane.YES_OPTION);
                try {
                    if (SwingUtilities.isEventDispatchThread()) confirm.run();
                    else SwingUtilities.invokeAndWait(confirm);
                    if (!trusted.get()) return false;
                    write(entry);
                    return true;
                } catch (Exception exception) {
                    LOG.log(Level.WARNING, "Could not save trusted SSH host key", exception);
                    return false;
                }
            }
        };
    }

    private void addClosableTab(String title, Component session) {
        JPanel tab = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        tab.setOpaque(false);
        tab.add(new JLabel(title));
        JButton close = new JButton("×");
        close.setFocusable(false);
        close.setBorderPainted(false);
        close.setContentAreaFilled(false);
        close.setMargin(new Insets(0, 2, 0, 2));
        close.addActionListener(event -> {
            Component terminalView = ((JPanel) session).getComponent(1);
            if (terminalView instanceof JediTermWidget widget) widget.close();
            SSHClient client = clients.remove(session);
            if (client != null) disconnectQuietly(client);
            sessions.remove(session);
        });
        tab.add(close);
        sessions.setTabComponentAt(sessions.indexOfComponent(session), tab);
    }

    private Profile connectionProfile() {
        return new Profile(name.getText().trim(), host.getText().trim(), (Integer) port.getValue(),
                user.getText().trim(), keyFile.getText().trim(), savePassword.isSelected(),
                new String(password.getPassword()));
    }

    private Profile savedProfile(Profile profile) {
        return new Profile(profile.name(), profile.host(), profile.port(), profile.user(), profile.keyFile(),
                savePassword.isSelected(), savePassword.isSelected() ? profile.password() : "");
    }

    private void apply(Profile profile) {
        name.setText(profile.name());
        host.setText(profile.host());
        port.setValue(profile.port());
        user.setText(profile.user());
        keyFile.setText(profile.keyFile());
        savePassword.setSelected(profile.savePassword());
        password.setText(profile.password());
        try { storage.writeString(ACTIVE_PROFILE_FILE, encode(profile.name())); }
        catch (Exception exception) { LOG.log(Level.WARNING, "Could not persist active SSH profile", exception); }
    }

    private void saveProfile() {
        Profile profile = savedProfile(connectionProfile());
        if (profile.name().isBlank() || profile.host().isBlank()) {
            JOptionPane.showMessageDialog(this, RemoteI18n.t("required"));
            return;
        }
        List<Profile> all = readProfiles();
        int existing = indexOfName(all, profile.name());
        if (existing >= 0) all.set(existing, profile); else all.add(profile);
        writeProfiles(all);
        loadProfiles(profile.name());
    }

    private void copyProfile() {
        Profile current = savedProfile(connectionProfile());
        String defaultName = current.name().isBlank() ? RemoteI18n.t("profile_copy_default") : current.name() + " copy";
        String copyName = JOptionPane.showInputDialog(this, RemoteI18n.t("profile_copy_prompt"), defaultName);
        if (copyName == null || copyName.isBlank()) return;
        String normalizedName = copyName.trim();
        List<Profile> all = readProfiles();
        if (indexOfName(all, normalizedName) >= 0) {
            JOptionPane.showMessageDialog(this, RemoteI18n.t("profile_exists", normalizedName));
            return;
        }
        Profile copy = new Profile(normalizedName, current.host(), current.port(), current.user(),
                current.keyFile(), current.savePassword(), current.password());
        all.add(copy);
        writeProfiles(all);
        loadProfiles(copy.name());
    }

    private void deleteProfile() {
        Profile selected = (Profile) profiles.getSelectedItem();
        if (selected == null) return;
        if (JOptionPane.showConfirmDialog(this, RemoteI18n.t("profile_delete_confirm", selected.name()),
                RemoteI18n.t("profile_delete_title"), JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        List<Profile> all = readProfiles();
        all.removeIf(profile -> profile.name().equals(selected.name()));
        writeProfiles(all);
        loadProfiles(all.isEmpty() ? null : all.get(0).name());
    }

    private void clearForm() {
        profiles.setSelectedItem(null);
        name.setText("Server");
        host.setText("");
        port.setValue(22);
        user.setText("");
        keyFile.setText("");
        password.setText("");
        savePassword.setSelected(false);
    }

    private void loadProfiles(String selectedName) {
        loadingProfiles = true;
        profiles.removeAllItems();
        List<Profile> all = readProfiles();
        for (Profile profile : all) profiles.addItem(profile);
        String activeName = selectedName == null ? readActiveProfileName() : selectedName;
        Profile selected = all.stream().filter(profile -> profile.name().equals(activeName)).findFirst()
                .orElse(all.isEmpty() ? null : all.get(0));
        profiles.setSelectedItem(selected);
        loadingProfiles = false;
        if (selected != null) apply(selected);
        else clearForm();
    }

    private String readActiveProfileName() {
        try { return storage.readString(ACTIVE_PROFILE_FILE).map(RemotePanel::decode).orElse(""); }
        catch (Exception exception) { LOG.log(Level.WARNING, "Could not read active SSH profile", exception); return ""; }
    }

    private List<Profile> readProfiles() {
        List<Profile> result = new ArrayList<>();
        try {
            for (String line : storage.readString(PROFILE_FILE).orElse("").lines().toList()) {
                String[] fields = line.split("\\t", -1);
                if (fields.length != 5 && fields.length != 7) continue;
                result.add(new Profile(decode(fields[0]), decode(fields[1]), Integer.parseInt(fields[2]),
                        decode(fields[3]), decode(fields[4]), fields.length == 7 && Boolean.parseBoolean(fields[5]),
                        fields.length == 7 ? decode(fields[6]) : ""));
            }
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "Could not read SSH profiles", exception);
        }
        return result;
    }

    private void writeProfiles(List<Profile> data) {
        try {
            StringBuilder content = new StringBuilder();
            for (Profile profile : data) {
                content.append(encode(profile.name())).append('\t').append(encode(profile.host())).append('\t')
                        .append(profile.port()).append('\t').append(encode(profile.user())).append('\t')
                        .append(encode(profile.keyFile())).append('\t').append(profile.savePassword()).append('\t')
                        .append(encode(profile.password())).append('\n');
            }
            storage.writeString(PROFILE_FILE, content.toString());
            Path dataDirectory = storage.dataDirectory();
            if (dataDirectory != null && !dataDirectory.toString().isBlank()) {
                try {
                    Files.setPosixFilePermissions(dataDirectory.resolve(PROFILE_FILE),
                            Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                } catch (UnsupportedOperationException ignored) { }
            }
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "Could not save SSH profiles", exception);
        }
    }

    private int indexOfName(List<Profile> profiles, String profileName) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).name().equalsIgnoreCase(profileName)) return i;
        }
        return -1;
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private record Profile(String name, String host, int port, String user, String keyFile,
                           boolean savePassword, String password) {
        @Override public String toString() { return name + " — " + user + "@" + host + ":" + port; }
    }
}
