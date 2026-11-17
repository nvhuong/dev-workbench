package com.laker.postman.panel.collections.tree;

import com.laker.postman.model.Environment;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.HttpRequestItem;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.collection.CollectionIdNormalizer;
import com.laker.postman.collection.model.CollectionDocument;
import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.ToolWindowSidebarToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.PlusButton;
import com.laker.postman.common.component.dialog.CurlImportDialog;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.panel.collections.tree.coordinator.RequestTreeCoordinator;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.topmenu.TopMenuBar;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.apipost.ApiPostCollectionParser;
import com.laker.postman.collection.model.CollectionParseResult;
import com.laker.postman.service.common.TreeNodeBuilder;
import com.laker.postman.service.collections.CollectionDocumentJsonCodec;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.panel.collections.tree.adapter.SwingCollectionTreeDocumentMapper;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.har.HarParser;
import com.laker.postman.service.ideahttp.IntelliJHttpParser;
import com.laker.postman.collection.importer.postman.PostmanCollectionParser;
import com.laker.postman.service.swagger.SwaggerParser;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.AsyncClipboardUtil;
import com.laker.postman.util.ClipboardUtil;
import com.laker.postman.util.FileChooserUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.common.component.notification.NotificationCenter;
import com.laker.postman.service.curl.CurlImportUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.laker.postman.panel.collections.tree.CollectionTreePanel.*;


@Slf4j
public class CollectionTreeToolbar extends UiSingletonPanel {
    private static final String FILE_CHOOSER_STATE_COLLECTION_IMPORT = "collections.import";
    private static final String FILE_CHOOSER_STATE_POSTMAN_IMPORT = "collections.import.postman";
    private static final String FILE_CHOOSER_STATE_HAR_IMPORT = "collections.import.har";
    private static final String FILE_CHOOSER_STATE_SWAGGER_IMPORT = "collections.import.swagger";
    private static final String FILE_CHOOSER_STATE_HTTP_IMPORT = "collections.import.http";
    private static final String FILE_CHOOSER_STATE_APIPOST_IMPORT = "collections.import.apipost";

    private SearchTextField searchField;
    /**
     * + Nút (Bộ sưu tập mới / nhập khẩu khác nhau)
     */
    private PlusButton plusBtn;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setOpaque(false);

        plusBtn = new PlusButton();
        plusBtn.setToolTipText("New / Import");
        plusBtn.addActionListener(e -> showPlusMenu());

        searchField = new SearchTextField();

        add(new ToolWindowSidebarToolbar(plusBtn, searchField), BorderLayout.CENTER);
    }

    /**
     * Bấm vào + Menu bật lên nút: Tạo bộ sưu tập mới + Bộ chia + Hàng nhập khẩu đa dạng
     */
    private void showPlusMenu() {
        readClipboardCurlTextAsync()
                .thenAccept(curlText -> SwingUtilities.invokeLater(() -> showPlusMenu(curlText)));
    }

    CompletableFuture<String> readClipboardCurlTextAsync() {
        return ClipboardUtil.getClipboardCurlTextAsync();
    }

    private void showPlusMenu(String clipboardText) {
        if (clipboardText != null) {
            int result = JOptionPane.showConfirmDialog(
                    UiSingletonFactory.getInstance(MainFrame.class),
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DETECTED),
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_TITLE),
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                importCurlToCollection(clipboardText);
                return;
            }
        }

        JPopupMenu menu = buildPlusMenu();
        menu.show(plusBtn, 0, plusBtn.getHeight());
    }

    private JPopupMenu buildPlusMenu() {
        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);

        // ── Tạo một bộ sưu tập mới
        JMenuItem newCollection = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_NEW_COLLECTION),
                IconUtil.create("icons/collection.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        newCollection.addActionListener(e -> {
            CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
            RequestTreeCoordinator coordinator = new RequestTreeCoordinator(leftPanel.getRequestTree(), leftPanel);
            coordinator.showAddGroupDialog(leftPanel.getRootTreeNode());
        });
        menu.add(newCollection);

        menu.addSeparator();

        // ── Hàng nhập khẩu đa dạng
        JMenuItem importEasy = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_EASY),
                new FlatSVGIcon("icons/easy.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importEasy.addActionListener(e -> importRequestCollection());
        menu.add(importEasy);

        JMenuItem importPostman = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_POSTMAN),
                new FlatSVGIcon("icons/postman.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importPostman.addActionListener(e -> importPostmanCollection());
        menu.add(importPostman);

        JMenuItem importSwagger2 = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SWAGGER2),
                new FlatSVGIcon("icons/swagger.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importSwagger2.addActionListener(e -> importSwaggerCollection());
        menu.add(importSwagger2);

        JMenuItem importOpenApi3 = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_OPENAPI3),
                new FlatSVGIcon("icons/openapi.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importOpenApi3.addActionListener(e -> importSwaggerCollection());
        menu.add(importOpenApi3);

        JMenuItem importHar = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HAR),
                new FlatSVGIcon("icons/insomnia.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importHar.addActionListener(e -> importHarCollection());
        menu.add(importHar);

        JMenuItem importHttp = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HTTP),
                new FlatSVGIcon("icons/idea-http.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importHttp.addActionListener(e -> importHttpFile());
        menu.add(importHttp);

        JMenuItem importApiPost = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_APIPOST),
                new FlatSVGIcon("icons/apipost.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importApiPost.addActionListener(e -> importApiPostCollection());
        menu.add(importApiPost);

        JMenuItem importCurl = new JMenuItem(
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL),
                new FlatSVGIcon("icons/curl.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importCurl.addActionListener(e -> importCurlToCollection(null));
        menu.add(importCurl);

        return menu;
    }

    @Override
    protected void registerListeners() {
        // Logic lọc tìm kiếm
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void filterTree() {
                CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
                String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    searchField.setNoResult(false);
                    // Mở rộng tất cả các nhóm cấp một để hiển thị tất cả
                    expandAll(leftPanel.getRequestTree(), false);
                    leftPanel.getTreeModel().setRoot(leftPanel.getRootTreeNode());
                    leftPanel.getTreeModel().reload();
                    return;
                }

                // Nhận các tùy chọn tìm kiếm
                boolean caseSensitive = searchField.isCaseSensitive();
                boolean wholeWord = searchField.isWholeWord();

                DefaultMutableTreeNode filteredRoot = new DefaultMutableTreeNode(ROOT);
                boolean hasResult = filterNodes(leftPanel.getRootTreeNode(), filteredRoot, text, caseSensitive, wholeWord);
                // Ô tìm kiếm chuyển sang màu đỏ khi không có kết quả và trở lại bình thường khi có kết quả
                searchField.setNoResult(!hasResult);
                leftPanel.getTreeModel().setRoot(filteredRoot);
                leftPanel.getTreeModel().reload();
                expandAll(leftPanel.getRequestTree(), true);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTree();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTree();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTree();
            }
        });

        // Theo dõi các thay đổi trong tùy chọn tìm kiếm và kích hoạt quá trình lọc lại
        searchField.addPropertyChangeListener("caseSensitive", evt -> {
            if (!searchField.getText().isEmpty()) {
                CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
                String text = searchField.getText().trim();
                boolean caseSensitive = searchField.isCaseSensitive();
                boolean wholeWord = searchField.isWholeWord();
                DefaultMutableTreeNode filteredRoot = new DefaultMutableTreeNode(ROOT);
                boolean hasResult = filterNodes(leftPanel.getRootTreeNode(), filteredRoot, text, caseSensitive, wholeWord);
                searchField.setNoResult(!hasResult);
                leftPanel.getTreeModel().setRoot(filteredRoot);
                leftPanel.getTreeModel().reload();
                expandAll(leftPanel.getRequestTree(), true);
            }
        });
        searchField.addPropertyChangeListener("wholeWord", evt -> {
            if (!searchField.getText().isEmpty()) {
                CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
                String text = searchField.getText().trim();
                boolean caseSensitive = searchField.isCaseSensitive();
                boolean wholeWord = searchField.isWholeWord();
                DefaultMutableTreeNode filteredRoot = new DefaultMutableTreeNode(ROOT);
                boolean hasResult = filterNodes(leftPanel.getRootTreeNode(), filteredRoot, text, caseSensitive, wholeWord);
                searchField.setNoResult(!hasResult);
                leftPanel.getTreeModel().setRoot(filteredRoot);
                leftPanel.getTreeModel().reload();
                expandAll(leftPanel.getRequestTree(), true);
            }
        });
    }


    // Bộ sưu tập yêu cầu nhập khẩuJSONtập tin
    private void importRequestCollection() {
        CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                FILE_CHOOSER_STATE_COLLECTION_IMPORT,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_DIALOG_TITLE));
        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                // Khi nhập, dữ liệu cũ không bị xóa mà tất cả được thêm vào nhóm mới
                String groupName = "Dev Workbench";
                DefaultMutableTreeNode easyPostmanGroup = leftPanel.findGroupNode(leftPanel.getRootTreeNode(), groupName);
                if (easyPostmanGroup == null) {
                    RequestGroup group = new RequestGroup(groupName);
                    easyPostmanGroup = CollectionTreeNodes.groupNode(group);
                    leftPanel.getRootTreeNode().add(easyPostmanGroup);
                }
                CollectionDocument document = CollectionDocumentJsonCodec.read(fileToOpen);
                int repairedIds = CollectionIdNormalizer.ensureUniqueIds(document,
                        CollectionIdNormalizer.collectIds(
                                SwingCollectionTreeDocumentMapper.fromRoot(leftPanel.getRootTreeNode())));
                if (repairedIds > 0) {
                    log.info("Reassigned {} duplicate or blank collection IDs while importing {}",
                            repairedIds, fileToOpen.getName());
                }
                for (DefaultMutableTreeNode groupNode : SwingCollectionTreeDocumentMapper.toTreeNodes(document)) {
                    easyPostmanGroup.add(groupNode);
                }
                leftPanel.getTreeModel().reload();

                leftPanel.getCollectionTreePersistence().saveCurrentTree();
                leftPanel.getRequestTree().expandPath(new TreePath(easyPostmanGroup.getPath()));
                NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS));
            } catch (Exception ex) {
                log.error("Import error", ex);
                JOptionPane.showMessageDialog(mainFrame,
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Nhập khẩuPostmantập trung
    private void importPostmanCollection() {
        CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                FILE_CHOOSER_STATE_POSTMAN_IMPORT,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_POSTMAN_DIALOG_TITLE));
        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                CollectionParseResult parseResult =
                        PostmanCollectionParser.parsePostmanCollection(json);
                if (parseResult != null) {
                    DefaultMutableTreeNode collectionNode =
                            TreeNodeBuilder.buildFromParseResult(parseResult);
                    leftPanel.getRootTreeNode().add(collectionNode);
                    leftPanel.getTreeModel().reload();
                    leftPanel.getCollectionTreePersistence().saveCurrentTree();
                    leftPanel.getRequestTree().expandPath(new TreePath(collectionNode.getPath()));
                    NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS));
                } else {
                    NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_POSTMAN_INVALID));
                }
            } catch (Exception ex) {
                log.error("Import error", ex);
                NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()));
            }
        }
    }

    // Nhập khẩuHARtập trung
    private void importHarCollection() {
        CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                FILE_CHOOSER_STATE_HAR_IMPORT,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HAR_DIALOG_TITLE));
        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                DefaultMutableTreeNode collectionNode = HarParser.parseHar(json);
                if (collectionNode != null) {
                    leftPanel.getRootTreeNode().add(collectionNode);
                    leftPanel.getTreeModel().reload();
                    leftPanel.getCollectionTreePersistence().saveCurrentTree();
                    leftPanel.getRequestTree().expandPath(new TreePath(collectionNode.getPath()));
                    NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS));
                } else {
                    NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HAR_INVALID));
                }
            } catch (Exception ex) {
                log.error("Import HAR error", ex);
                NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()));
            }
        }
    }

    // Nhập khẩuSwagger/OpenAPItập trung
    private void importSwaggerCollection() {
        CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                FILE_CHOOSER_STATE_SWAGGER_IMPORT,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SWAGGER_DIALOG_TITLE));
        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                CollectionParseResult parseResult = SwaggerParser.parseSwagger(json);
                if (parseResult != null) {
                    DefaultMutableTreeNode collectionNode =
                            TreeNodeBuilder.buildFromParseResult(parseResult);
                    leftPanel.getRootTreeNode().add(collectionNode);
                    leftPanel.getTreeModel().reload();
                    leftPanel.getCollectionTreePersistence().saveCurrentTree();
                    leftPanel.getRequestTree().expandPath(new TreePath(collectionNode.getPath()));

                    // Nhập các biến môi trường
                    importEnvironmentsFromParseResult(parseResult);

                    NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS));
                } else {
                    NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SWAGGER_INVALID));
                }
            } catch (Exception ex) {
                log.error("Import Swagger error", ex);
                NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()));
            }
        }
    }

    /**
     * Nhập các biến môi trường từ kết quả được phân tích cú pháp
     */
    private void importEnvironmentsFromParseResult(CollectionParseResult parseResult) {
        if (parseResult.getEnvironments() == null || parseResult.getEnvironments().isEmpty()) {
            return;
        }

        int importedCount = 0;
        for (Environment env : parseResult.getEnvironments()) {
            try {
                EnvironmentService.saveEnvironment(env);
                importedCount++;
                log.info("Nhập các biến môi trường: {} ({}các biến)", env.getName(), env.getVariableList().size());
            } catch (Exception e) {
                log.error("Không thể nhập biến môi trường: {}", env.getName(), e);
            }
        }

        if (importedCount > 0) {
            log.info("Đã nhập thành công {} một môi trường", importedCount);

            // Làm mới TopMenuBar hộp thả xuống môi trường
            try {
                TopMenuBar topMenuBar = UiSingletonFactory.getInstance(TopMenuBar.class);
                if (topMenuBar.getEnvironmentComboBox() != null) {
                    topMenuBar.getEnvironmentComboBox().reload();
                    log.debug("Hộp thả xuống môi trường đã được làm mới");
                }
            } catch (Exception e) {
                log.error("Không làm mới được hộp thả xuống môi trường", e);
            }
        }
    }

    // Nhập khẩuHTTPtập tin
    private void importHttpFile() {
        CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                FILE_CHOOSER_STATE_HTTP_IMPORT,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HTTP_DIALOG_TITLE));
        SystemFileChooser.FileNameExtensionFilter httpFilter =
                FileChooserUtil.extensionFilter("HTTP Files (*.http)", "http");
        fileChooser.setFileFilter(httpFilter);
        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String content = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                String filename = fileToOpen.getName();
                CollectionParseResult parseResult =
                        IntelliJHttpParser.parseHttpFile(content, filename);
                if (parseResult != null) {
                    DefaultMutableTreeNode collectionNode =
                            TreeNodeBuilder.buildFromParseResult(parseResult);
                    leftPanel.getRootTreeNode().add(collectionNode);
                    leftPanel.getTreeModel().reload();
                    leftPanel.getCollectionTreePersistence().saveCurrentTree();
                    leftPanel.getRequestTree().expandPath(new TreePath(collectionNode.getPath()));
                    NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS));
                } else {
                    NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HTTP_INVALID));
                }
            } catch (Exception ex) {
                log.error("Import HTTP file error", ex);
                NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()));
            }
        }
    }

    /**
     * Nhập khẩu ApiPost Tài liệu
     */
    private void importApiPostCollection() {
        CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                FILE_CHOOSER_STATE_APIPOST_IMPORT,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_APIPOST_DIALOG_TITLE));
        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == SystemFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                CollectionParseResult parseResult =
                        ApiPostCollectionParser.parseApiPostCollection(json);
                if (parseResult != null) {
                    DefaultMutableTreeNode collectionNode =
                            TreeNodeBuilder.buildFromParseResult(parseResult);
                    leftPanel.getRootTreeNode().add(collectionNode);
                    leftPanel.getTreeModel().reload();
                    leftPanel.getCollectionTreePersistence().saveCurrentTree();
                    leftPanel.getRequestTree().expandPath(new TreePath(collectionNode.getPath()));

                    importEnvironmentsFromParseResult(parseResult);

                    NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS));
                } else {
                    NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_APIPOST_INVALID));
                }
            } catch (Exception ex) {
                log.error("Import ApiPost error", ex);
                NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()));
            }
        }
    }

    public void importCurlToGroup(RequestGroup targetGroup) {
        if (targetGroup == null) return;
        importCurlToCollection(null, targetGroup);
    }

    private void importCurlToCollection(String defaultCurl) {
        importCurlToCollection(defaultCurl, null);
    }

    private void importCurlToCollection(String defaultCurl, RequestGroup targetGroup) {
        MainFrame mainFrame = UiSingletonFactory.getInstance(MainFrame.class);
        String curlText = CurlImportDialog.show(mainFrame,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DIALOG_TITLE),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DIALOG_PROMPT),
                defaultCurl);
        if (curlText == null || curlText.trim().isEmpty()) return;
        try {
            CurlImportResult importResult = parseCurlImport(curlText);
            if (importResult.items().isEmpty()) {
                NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_PARSE_FAIL));
                return;
            }

            boolean batchInput = importResult.totalCommands() > 1;
            boolean saved;
            if (targetGroup != null) {
                saved = saveRequestsToGroup(importResult.items(), targetGroup);
            } else if (batchInput) {
                saved = saveRequestsWithGroupDialog(importResult.items());
            } else {
                saved = saveRequestWithGroupDialog(importResult.items().get(0));
            }

            // Xóa clipboard sau khi nhập thành công
            if (saved) {
                AsyncClipboardUtil.clearStringAsync();
                notifyCurlImportResult(importResult);
            }
        } catch (Exception ex) {
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_PARSE_ERROR, ex.getMessage()));
        }
    }

    private boolean containsCurlCommands(String text) {
        return text != null && !CurlParser.splitCommands(text).isEmpty();
    }

    private CurlImportResult parseCurlImport(String curlText) {
        List<String> commands = CurlParser.splitCommands(curlText);
        List<HttpRequestItem> items = new ArrayList<>();
        int failedCount = 0;

        for (String command : commands) {
            try {
                HttpRequestItem item = CurlImportUtil.fromCurlRequest(CurlParser.parse(command));
                if (item == null) {
                    failedCount++;
                    continue;
                }
                item.setName(defaultCurlRequestName(item));
                items.add(item);
            } catch (Exception ex) {
                failedCount++;
                log.warn("Parse cURL command failed during batch import", ex);
            }
        }

        return new CurlImportResult(items, commands.size(), failedCount);
    }

    private String defaultCurlRequestName(HttpRequestItem item) {
        String url = item.getUrl();
        if (url == null || url.trim().isEmpty()) {
            return I18nUtil.getMessage(MessageKeys.REQUEST_NAME);
        }
        return url;
    }

    private void notifyCurlImportResult(CurlImportResult importResult) {
        if (importResult.totalCommands() <= 1) {
            return;
        }
        if (importResult.failedCount() > 0) {
            NotificationCenter.showWarning(I18nUtil.getMessage(
                    MessageKeys.COLLECTIONS_IMPORT_CURL_BATCH_PARTIAL,
                    importResult.items().size(),
                    importResult.failedCount()
            ));
            return;
        }
        NotificationCenter.showSuccess(I18nUtil.getMessage(
                MessageKeys.COLLECTIONS_IMPORT_CURL_BATCH_SUCCESS,
                importResult.items().size()
        ));
    }


    /**
     * Một cửa sổ bật lên cho phép người dùng chọn nhóm, đặt tên và lưu HttpRequestItem toCollection(phương thức công khai, áp dụng cho cURLNhập khẩu và các kịch bản khác)
     *
     * @param item Yêu cầu lưu
     */
    private boolean saveRequestWithGroupDialog(HttpRequestItem item) {
        CollectionTreePanel collectionPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        RequestEditorPanel requestEditPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        TreeModel groupTreeModel = collectionPanel.getGroupTreeModel();
        RequestNameSelection selection = CollectionGroupSelectionDialog
                .chooseGroupAndRequestName(groupTreeModel, item.getName())
                .orElse(null);
        if (selection == null) return false;
        String requestName = selection.requestName();
        item.setName(requestName);
        item.setId(IdUtil.simpleUUID());
        collectionPanel.saveRequestToGroup(selection.group(), item);
        requestEditPanel.showOrCreateTab(item); // Mở một yêu cầu chỉnh sửatab
        // treeChọn nút yêu cầu mới
        collectionPanel.locateAndSelectRequest(item.getId());
        return true;
    }

    private boolean saveRequestsWithGroupDialog(List<HttpRequestItem> items) {
        CollectionTreePanel collectionPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        TreeModel groupTreeModel = collectionPanel.getGroupTreeModel();
        RequestGroup group = CollectionGroupSelectionDialog.chooseGroup(groupTreeModel).orElse(null);
        if (group == null) return false;

        return saveRequestsToGroup(items, group);
    }

    private boolean saveRequestsToGroup(List<HttpRequestItem> items, RequestGroup group) {
        if (items == null || items.isEmpty() || group == null) return false;
        CollectionTreePanel collectionPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        RequestEditorPanel requestEditPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        HttpRequestItem lastItem = null;
        for (HttpRequestItem item : items) {
            item.setId(IdUtil.simpleUUID());
            if (item.getName() == null || item.getName().trim().isEmpty()) {
                item.setName(defaultCurlRequestName(item));
            }
            collectionPanel.saveRequestToGroup(group, item);
            lastItem = item;
        }

        if (lastItem != null) {
            requestEditPanel.showOrCreateTab(lastItem);
            collectionPanel.locateAndSelectRequest(lastItem.getId());
        }
        return true;
    }

    private record CurlImportResult(List<HttpRequestItem> items, int totalCommands, int failedCount) {
    }


    // Nút lọc đệ quy, hỗ trợ phân biệt chữ hoa chữ thường và khớp toàn bộ từ
    private boolean filterNodes(DefaultMutableTreeNode src, DefaultMutableTreeNode dest, String keyword,
                                boolean caseSensitive, boolean wholeWord) {
        boolean matched = false;
        if (CollectionTreeNodes.isGroup(src)) {
            RequestGroup group = CollectionTreeNodes.group(src).orElseThrow();
            String groupName = group.getName();
            DefaultMutableTreeNode groupNode = CollectionTreeNodes.groupNode(group);
            boolean childMatched = false;
            for (int i = 0; i < src.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) src.getChildAt(i);
                if (filterNodes(child, groupNode, keyword, caseSensitive, wholeWord)) {
                    childMatched = true;
                }
            }
            if (matchesText(groupName, keyword, caseSensitive, wholeWord) || childMatched) {
                dest.add(groupNode);
                matched = true;
            }
        } else if (CollectionTreeNodes.isRequest(src)) {
            HttpRequestItem item = CollectionTreeNodes.request(src).orElseThrow();
            boolean nameMatch = item.getName() != null &&
                    matchesText(item.getName(), keyword, caseSensitive, wholeWord);
            boolean urlMatch = item.getUrl() != null &&
                    matchesText(item.getUrl(), keyword, caseSensitive, wholeWord);
            if (nameMatch || urlMatch) {
                dest.add(CollectionTreeNodes.requestNode(item));
                matched = true;
            }
        } else {
            // xử lý root nút
            boolean childMatched = false;
            for (int i = 0; i < src.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) src.getChildAt(i);
                if (filterNodes(child, dest, keyword, caseSensitive, wholeWord)) {
                    childMatched = true;
                }
            }
            matched = childMatched;
        }
        return matched;
    }

    /**
     * Xác định xem văn bản có khớp với từ khóa hay không, hỗ trợ phân biệt chữ hoa chữ thường và khớp toàn bộ từ
     */
    private boolean matchesText(String text, String keyword, boolean caseSensitive, boolean wholeWord) {
        if (text == null || keyword == null) {
            return false;
        }

        String searchText = caseSensitive ? text : text.toLowerCase();
        String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        if (!wholeWord) {
            // Đơn giản bao gồm kết hợp
            return searchText.contains(searchKeyword);
        }

        // So khớp toàn bộ từ: sử dụng biểu thức thông thường
        // \b Đại diện cho ranh giới từ
        String regex = "\\b" + java.util.regex.Pattern.quote(searchKeyword) + "\\b";
        int flags = caseSensitive ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, flags);
        return pattern.matcher(text).find();
    }

    // Mở rộng/Thu gọn tất cả các nút
    private void expandAll(JTree tree, boolean expand) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), expand);
    }

    private void expandAll(JTree tree, TreePath parent, boolean expand) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (int i = 0; i < node.getChildCount(); i++) {
                TreeNode n = node.getChildAt(i);
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }

}
