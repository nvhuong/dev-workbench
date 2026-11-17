package com.laker.postman.plugin.minio;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.util.FontsUtil;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import io.minio.messages.Bucket;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public final class MinioPanel extends JPanel {
    private static final int MAX_ROWS = 2000;
    private final JTextField endpoint = new JTextField("http://localhost:9000");
    private final JTextField accessKey = new JTextField();
    private final JPasswordField secretKey = new JPasswordField();
    private final JLabel status = new JLabel(" ");
    private final JTree buckets = new JTree(new DefaultMutableTreeNode(MinioI18n.t("buckets")));
    private final DefaultTableModel objectModel = new DefaultTableModel(
            new Object[]{MinioI18n.t("object"), MinioI18n.t("size"), MinioI18n.t("modified")}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable objects = new JTable(objectModel);
    private MinioClient client;
    private JButton connectButton;
    private JButton disconnectButton;
    private JButton refreshButton;
    private JButton uploadButton;
    private JButton downloadButton;
    private JButton deleteButton;
    private JButton createBucketButton;
    private String selectedBucket;

    public MinioPanel() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(createConnectionBar(), BorderLayout.NORTH);
        add(createBrowser(), BorderLayout.CENTER);
        status.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        JPanel footer = new JPanel(new BorderLayout()); footer.setOpaque(false); footer.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0)); footer.add(status, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        updateConnectedState(false);
    }

    private JPanel createConnectionBar() {
        JPanel panel = new JPanel(new MigLayout("insets 0, novisualpadding, fillx, wrap 8", "[][grow,fill][][grow,fill][][grow,fill][]", "[]6[]"));
        panel.setOpaque(false);
        endpoint.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, MinioI18n.t("endpoint_hint"));
        accessKey.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, MinioI18n.t("access_hint"));
        secretKey.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, MinioI18n.t("secret_hint"));
        connectButton = new JButton(MinioI18n.t("connect"));
        disconnectButton = new JButton(MinioI18n.t("disconnect"));
        connectButton.addActionListener(event -> connect());
        disconnectButton.addActionListener(event -> disconnect());
        panel.add(new JLabel(MinioI18n.t("endpoint"))); panel.add(endpoint, "span 3,growx,wmin 160");
        panel.add(new JLabel(MinioI18n.t("access_key"))); panel.add(accessKey, "span 3,growx,wmin 100,wrap");
        panel.add(new JLabel(MinioI18n.t("secret_key"))); panel.add(secretKey, "span 3,growx,wmin 100");
        panel.add(connectButton, "split 2"); panel.add(disconnectButton, "wrap");
        return panel;
    }

    private JPanel createBrowser() {
        JPanel left = new JPanel(new BorderLayout(0, 6)); left.setOpaque(false);
        JPanel bucketHeader = new JPanel(new BorderLayout(4, 0)); bucketHeader.setOpaque(false);
        bucketHeader.add(new JLabel(MinioI18n.t("buckets")), BorderLayout.CENTER);
        refreshButton = new JButton(MinioI18n.t("refresh")); refreshButton.addActionListener(event -> loadBuckets());
        createBucketButton = new JButton(MinioI18n.t("create_bucket")); createBucketButton.addActionListener(event -> createBucket());
        JPanel bucketActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)); bucketActions.setOpaque(false); bucketActions.add(createBucketButton); bucketActions.add(refreshButton);
        bucketHeader.add(bucketActions, BorderLayout.EAST); left.add(bucketHeader, BorderLayout.NORTH);
        buckets.setRootVisible(false); buckets.setShowsRootHandles(true);
        buckets.addTreeSelectionListener(event -> {
            Object node = buckets.getLastSelectedPathComponent();
            if (node instanceof DefaultMutableTreeNode selected && selected.getLevel() == 1) {
                selectedBucket = String.valueOf(selected.getUserObject()); loadObjects(selectedBucket);
            }
        });
        JScrollPane bucketScroll = new JScrollPane(buckets); ToolWindowSurfaceStyle.applyFramedScrollPaneCard(bucketScroll); left.add(bucketScroll, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(0, 6)); right.setOpaque(false);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); toolbar.setOpaque(false);
        uploadButton = new JButton(MinioI18n.t("upload")); uploadButton.addActionListener(event -> uploadObject());
        downloadButton = new JButton(MinioI18n.t("download")); downloadButton.addActionListener(event -> downloadObject());
        deleteButton = new JButton(MinioI18n.t("delete")); deleteButton.addActionListener(event -> deleteObject());
        JButton urlButton = new JButton(MinioI18n.t("presign")); urlButton.addActionListener(event -> createPresignedUrl());
        toolbar.add(uploadButton); toolbar.add(downloadButton); toolbar.add(deleteButton); toolbar.add(urlButton);
        right.add(toolbar, BorderLayout.NORTH);
        objects.setAutoCreateRowSorter(true); objects.setFillsViewportHeight(true); objects.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane objectScroll = new JScrollPane(objects); ToolWindowSurfaceStyle.applyFramedScrollPaneCard(objectScroll); right.add(objectScroll, BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right); split.setBorder(null); split.setDividerSize(6); split.setResizeWeight(0.25); split.setDividerLocation(240);
        JPanel host = new JPanel(new BorderLayout()); host.setOpaque(false); host.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0)); host.add(split, BorderLayout.CENTER);
        return host;
    }

    private void connect() {
        String url = endpoint.getText().trim();
        if (!(url.startsWith("http://") || url.startsWith("https://"))) { setStatus(MinioI18n.t("invalid_endpoint")); return; }
        connectButton.setEnabled(false); setStatus(MinioI18n.t("connecting"));
        String access = accessKey.getText().trim(); String secret = new String(secretKey.getPassword());
        new SwingWorker<MinioClient, Void>() {
            @Override protected MinioClient doInBackground() {
                return MinioClient.builder().endpoint(url).credentials(access, secret).build();
            }
            @Override protected void done() {
                try { client = get(); updateConnectedState(true); setStatus(MinioI18n.t("connected")); loadBuckets(); }
                catch (Exception ex) { updateConnectedState(false); setStatus(MinioI18n.t("error", safeMessage(ex))); log.warn("MinIO connection failed", ex); }
            }
        }.execute();
    }

    private void loadBuckets() {
        MinioClient active = client; if (active == null) return;
        setStatus(MinioI18n.t("loading_buckets")); refreshButton.setEnabled(false);
        new SwingWorker<List<Bucket>, Void>() {
            @Override protected List<Bucket> doInBackground() throws Exception { return active.listBuckets(); }
            @Override protected void done() {
                if (active != client) return;
                try {
                    List<Bucket> result = get(); DefaultMutableTreeNode root = new DefaultMutableTreeNode(MinioI18n.t("buckets"));
                    for (Bucket bucket : result) root.add(new DefaultMutableTreeNode(bucket.name()));
                    buckets.setModel(new DefaultTreeModel(root));
                    for (int i=0;i<buckets.getRowCount();i++) buckets.expandRow(i);
                    refreshButton.setEnabled(true); setStatus(MinioI18n.t("buckets_loaded", result.size()));
                } catch (Exception ex) { refreshButton.setEnabled(true); setStatus(MinioI18n.t("error", safeMessage(ex))); }
            }
        }.execute();
    }

    private void loadObjects(String bucket) {
        MinioClient active = client; if (active == null) return;
        setStatus(MinioI18n.t("loading_objects", bucket));
        new SwingWorker<List<ObjectRow>, Void>() {
            @Override protected List<ObjectRow> doInBackground() throws Exception {
                List<ObjectRow> rows = new ArrayList<>();
                Iterable<Result<Item>> listing = active.listObjects(ListObjectsArgs.builder().bucket(bucket).recursive(true).build());
                for (Result<Item> result : listing) {
                    Item item = result.get();
                    if (rows.size() >= MAX_ROWS) break;
                    String modified = item.lastModified() == null ? "" : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(item.lastModified());
                    rows.add(new ObjectRow(item.objectName(), item.size(), modified));
                }
                return rows;
            }
            @Override protected void done() {
                if (active != client || !bucket.equals(selectedBucket)) return;
                try {
                    List<ObjectRow> result = get(); objectModel.setRowCount(0);
                    for (ObjectRow row : result) objectModel.addRow(new Object[]{row.name(), formatSize(row.size()), row.modified()});
                    setStatus(MinioI18n.t("objects_loaded", result.size(), MAX_ROWS));
                } catch (Exception ex) { setStatus(MinioI18n.t("error", safeMessage(ex))); }
            }
        }.execute();
    }

    private void createBucket() {
        MinioClient active=client; if(active==null)return;
        String name=JOptionPane.showInputDialog(this,MinioI18n.t("bucket_name")); if(name==null||name.isBlank())return;
        runAction(MinioI18n.t("creating_bucket"), ()->{active.makeBucket(MakeBucketArgs.builder().bucket(name.trim()).build());return null;}, ignored->loadBuckets());
    }

    private void uploadObject() {
        MinioClient active=client; String bucket=selectedBucket; if(active==null||bucket==null)return;
        JFileChooser chooser=new JFileChooser(); if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path file=chooser.getSelectedFile().toPath(); String object=JOptionPane.showInputDialog(this,MinioI18n.t("object_name"),file.getFileName().toString());
        if(object==null||object.isBlank())return;
        runAction(MinioI18n.t("uploading"),()->{long size=Files.size(file);String type=Files.probeContentType(file);try(InputStream input=Files.newInputStream(file)){active.putObject(PutObjectArgs.builder().bucket(bucket).object(object.trim()).stream(input,size,-1).contentType(type==null?"application/octet-stream":type).build());}return null;},ignored->loadObjects(bucket));
    }

    private void downloadObject() {
        String object=selectedObject(); MinioClient active=client; String bucket=selectedBucket; if(object==null||active==null||bucket==null)return;
        JFileChooser chooser=new JFileChooser(); chooser.setSelectedFile(Path.of(object).getFileName().toFile()); if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        Path target=chooser.getSelectedFile().toPath();
        runAction(MinioI18n.t("downloading"),()->{try(InputStream input=active.getObject(GetObjectArgs.builder().bucket(bucket).object(object).build())){Files.copy(input,target,StandardCopyOption.REPLACE_EXISTING);}return null;},ignored->setStatus(MinioI18n.t("downloaded",target.getFileName())));
    }

    private void deleteObject() {
        String object=selectedObject();MinioClient active=client;String bucket=selectedBucket;if(object==null||active==null||bucket==null)return;
        if(JOptionPane.showConfirmDialog(this,MinioI18n.t("confirm_delete",object),MinioI18n.t("delete"),JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        runAction(MinioI18n.t("deleting"),()->{active.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(object).build());return null;},ignored->loadObjects(bucket));
    }

    private void createPresignedUrl() {
        String object=selectedObject();MinioClient active=client;String bucket=selectedBucket;if(object==null||active==null||bucket==null)return;
        runAction(MinioI18n.t("creating_url"),()->active.getPresignedObjectUrl(io.minio.GetPresignedObjectUrlArgs.builder().method(io.minio.http.Method.GET).bucket(bucket).object(object).expiry(3600).build()),url->{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(url),null);setStatus(MinioI18n.t("url_copied"));});
    }

    private <T> void runAction(String working, ThrowingSupplier<T> operation, java.util.function.Consumer<T> success) {
        setStatus(working);
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return operation.get(); }
            @Override protected void done() { try { success.accept(get()); } catch(Exception ex) { setStatus(MinioI18n.t("error",safeMessage(ex)));log.warn("MinIO operation failed",ex); } }
        }.execute();
    }

    private String selectedObject() { int row=objects.getSelectedRow(); if(row<0)return null; return String.valueOf(objectModel.getValueAt(objects.convertRowIndexToModel(row),0)); }
    private String safeMessage(Throwable ex){Throwable cause=ex.getCause()==null?ex:ex.getCause();String message=cause.getMessage();return message==null||message.isBlank()?cause.getClass().getSimpleName():message;}
    private String formatSize(long bytes){if(bytes<1024)return bytes+" B";if(bytes<1024*1024)return String.format("%.1f KB",bytes/1024.0);if(bytes<1024L*1024*1024)return String.format("%.1f MB",bytes/(1024.0*1024));return String.format("%.1f GB",bytes/(1024.0*1024*1024));}
    private void disconnect(){client=null;selectedBucket=null;objectModel.setRowCount(0);buckets.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(MinioI18n.t("buckets"))));updateConnectedState(false);setStatus(MinioI18n.t("disconnected"));}
    private void updateConnectedState(boolean connected){connectButton.setEnabled(!connected);disconnectButton.setEnabled(connected);endpoint.setEnabled(!connected);accessKey.setEnabled(!connected);secretKey.setEnabled(!connected);refreshButton.setEnabled(connected);createBucketButton.setEnabled(connected);uploadButton.setEnabled(connected);downloadButton.setEnabled(connected);deleteButton.setEnabled(connected);}
    private void setStatus(String text){status.setText(text);status.setToolTipText(text);}
    private record ObjectRow(String name,long size,String modified){}
    @FunctionalInterface private interface ThrowingSupplier<T>{T get()throws Exception;}
}
