package com.laker.postman.plugin.remote;

import com.laker.postman.plugin.api.PluginStorage;
import com.laker.postman.util.FontsUtil;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

final class RemotePanel extends JPanel {
    private static final String PROFILE_FILE = "profiles.json";
    private final PluginStorage storage;
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(RemotePanel.class.getName());
    private final JComboBox<Profile> profiles = new JComboBox<>();
    private final JTextField name = new JTextField("Server"), host = new JTextField(), user = new JTextField(), keyFile = new JTextField();
    private final JSpinner port = new JSpinner(new SpinnerNumberModel(22, 1, 65535, 1));
    private final JPasswordField password = new JPasswordField();
    private final JCheckBox savePassword = new JCheckBox(RemoteI18n.t("save_password"));
    private final JTabbedPane sessions = new JTabbedPane();
    private final Map<Component, SSHClient> clients = new ConcurrentHashMap<>();

    RemotePanel(PluginStorage storage) {
        this.storage = storage;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(createConnectionForm(), BorderLayout.NORTH);
        add(sessions, BorderLayout.CENTER);
        loadProfiles();
        sessions.addChangeListener(event -> {});
    }

    private JPanel createConnectionForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(3, 4, 3, 4); c.fill = GridBagConstraints.HORIZONTAL;
        JComboBox<Profile> saved = profiles;
        saved.addActionListener(e -> { Profile p=(Profile)saved.getSelectedItem(); if(p!=null) apply(p); });
        JButton save = new JButton(RemoteI18n.t("save")); save.addActionListener(e -> saveProfile());
        addField(form,c,0,RemoteI18n.t("profiles"),saved,save);
        addField(form,c,1,RemoteI18n.t("name"),name,null);
        addField(form,c,2,RemoteI18n.t("host"),host,new JLabel(RemoteI18n.t("port")));
        c.gridx=3; c.weightx=0; form.add(port,c); c.weightx=0;
        addField(form,c,3,RemoteI18n.t("user"),user,new JLabel(RemoteI18n.t("password")));
        c.gridx=3; form.add(password,c);
        c.gridx=1; c.gridy=4; c.gridwidth=2; form.add(savePassword,c);
        addField(form,c,5,RemoteI18n.t("key"),keyFile,null);
        JButton browse=new JButton(RemoteI18n.t("browse")); browse.addActionListener(e->chooseKey()); c.gridx=3; c.gridy=5; form.add(browse,c);
        JButton connect=new JButton(RemoteI18n.t("connect")); connect.addActionListener(e->connect()); c.gridx=3; c.gridy=6; form.add(connect,c);
        JLabel note=new JLabel(RemoteI18n.t("host_key")); note.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN,-2)); c.gridx=0;c.gridy=7;c.gridwidth=4;form.add(note,c);
        return form;
    }

    private void addField(JPanel form, GridBagConstraints c, int row, String label, Component field, Component trailing) {
        c.gridy=row;c.gridwidth=1;c.weightx=0;c.gridx=0;form.add(new JLabel(label),c);
        c.gridx=1;c.weightx=1;form.add(field,c);
        if(trailing!=null){c.gridx=2;c.weightx=0;form.add(trailing,c);}
    }

    private void chooseKey() {
        JFileChooser chooser=new JFileChooser();
        if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) keyFile.setText(chooser.getSelectedFile().getAbsolutePath());
    }

    private void connect() {
        Profile profile=currentProfile();
        if(profile.host().isBlank()||profile.user().isBlank()){ JOptionPane.showMessageDialog(this,RemoteI18n.t("required"));return; }
        JPasswordField secret=password;
        JPanel terminal=new JPanel(new BorderLayout(4,4));
        JTextArea output=new JTextArea(); output.setEditable(false); output.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));
        JTextField command=new JTextField(); terminal.add(new JScrollPane(output),BorderLayout.CENTER); terminal.add(command,BorderLayout.SOUTH);
        String title=profile.name().isBlank()?profile.host():profile.name(); sessions.addTab(title,terminal); sessions.setSelectedComponent(terminal);
        addClosableTab(title, terminal);
        command.setEnabled(false);
        Thread worker=new Thread(()->{
            SSHClient client=new SSHClient();
            try {
                clients.put(terminal, client);
                client.addHostKeyVerifier(new OpenSSHKnownHosts(Path.of(System.getProperty("user.home"),".ssh","known_hosts").toFile()));
                client.connect(profile.host(),profile.port());
                String pass=new String(secret.getPassword());
                if(!profile.keyFile().isBlank()) {
                    KeyProvider kp=pass.isBlank()?client.loadKeys(profile.keyFile()):client.loadKeys(profile.keyFile(),pass);
                    client.authPublickey(profile.user(),kp);
                } else if(!pass.isBlank()) client.authPassword(profile.user(),pass);
                else client.authPublickey(profile.user());
                Session session=client.startSession(); session.allocateDefaultPTY();
                Session.Shell shell=session.startShell();
                SwingUtilities.invokeLater(()->command.setEnabled(true));
                Thread reader=new Thread(()->readOutput(shell.getInputStream(),output),"ssh-reader-"+profile.host()); reader.setDaemon(true);reader.start();
                command.addActionListener(e->{String line=command.getText();command.setText("");try{shell.getOutputStream().write((line+"\n").getBytes(StandardCharsets.UTF_8));shell.getOutputStream().flush();}catch(Exception ex){SwingUtilities.invokeLater(()->append(output,"\n"+ex.getMessage()+"\n"));}});
                shell.join();
            } catch(Exception ex) { LOG.log(java.util.logging.Level.WARNING,"SSH session failed for "+profile.host(),ex); SwingUtilities.invokeLater(()->append(output,RemoteI18n.t("connection_error",ex.getMessage()))); }
            finally { try{client.disconnect();}catch(Exception ignored){} clients.remove(terminal); }
        },"ssh-session-"+profile.host()); worker.setDaemon(true);worker.start();
    }

    private void readOutput(InputStream input,JTextArea output) {
        byte[] data=new byte[2048];
        try { int count;while((count=input.read(data))>=0){String text=new String(data,0,count,StandardCharsets.UTF_8);SwingUtilities.invokeLater(()->append(output,text));} }
        catch(Exception ex){SwingUtilities.invokeLater(()->append(output,"\n"+ex.getMessage()));}
    }
    private void append(JTextArea area,String text){area.append(text);area.setCaretPosition(area.getDocument().getLength());}

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
            SSHClient client = clients.remove(session);
            if (client != null) {
                try { client.disconnect(); } catch (Exception ex) { LOG.log(java.util.logging.Level.FINE, "SSH session close failed", ex); }
            }
            sessions.remove(session);
        });
        tab.add(close);
        sessions.setTabComponentAt(sessions.indexOfComponent(session), tab);
    }

    private Profile currentProfile(){return new Profile(name.getText().trim(),host.getText().trim(),(Integer)port.getValue(),user.getText().trim(),keyFile.getText().trim(),savePassword.isSelected(),savePassword.isSelected()?new String(password.getPassword()):"");}
    private void apply(Profile p){name.setText(p.name());host.setText(p.host());port.setValue(p.port());user.setText(p.user());keyFile.setText(p.keyFile());savePassword.setSelected(p.savePassword());password.setText(p.password());}
    private void saveProfile(){Profile p=currentProfile();if(p.name().isBlank()||p.host().isBlank())return;List<Profile> all=readProfiles();all.removeIf(old->old.name().equals(p.name()));all.add(p);writeProfiles(all);loadProfiles();profiles.setSelectedItem(p);}
    private void loadProfiles(){profiles.removeAllItems();for(Profile p:readProfiles())profiles.addItem(p);}
    private List<Profile> readProfiles(){
        List<Profile> result=new ArrayList<>();
        try {
            for(String line:storage.readString(PROFILE_FILE).orElse("").lines().toList()) {
                String[] f=line.split("\\t",-1); if(f.length!=5 && f.length!=7) continue;
                result.add(new Profile(dec(f[0]),dec(f[1]),Integer.parseInt(f[2]),dec(f[3]),dec(f[4]),f.length==7&&Boolean.parseBoolean(f[5]),f.length==7?dec(f[6]):""));
            }
        } catch(Exception ex){LOG.log(java.util.logging.Level.WARNING,"Could not read SSH profiles",ex);}
        return result;
    }
    private void writeProfiles(List<Profile> data){
        try {
            StringBuilder text=new StringBuilder();
            for(Profile p:data) text.append(enc(p.name())).append('\t').append(enc(p.host())).append('\t').append(p.port()).append('\t').append(enc(p.user())).append('\t').append(enc(p.keyFile())).append('\t').append(p.savePassword()).append('\t').append(enc(p.password())).append('\n');
            storage.writeString(PROFILE_FILE,text.toString());
            Path dataDirectory = storage.dataDirectory();
            if (dataDirectory != null && !dataDirectory.toString().isBlank()) {
                try {
                    Files.setPosixFilePermissions(dataDirectory.resolve(PROFILE_FILE),
                            Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                } catch (UnsupportedOperationException ignored) {
                    // Hệ thống tệp hiện tại không hỗ trợ quyền POSIX.
                }
            }
        } catch(Exception ex){LOG.log(java.util.logging.Level.WARNING,"Could not save SSH profiles",ex);}
    }
    private static String enc(String value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));}
    private static String dec(String value){return new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8);}
    private record Profile(String name,String host,int port,String user,String keyFile,boolean savePassword,String password){@Override public String toString(){return name+" — "+user+"@"+host+":"+port;}}
}
