package com.nvhuong.devworkbench.plugin.decompiler;

import com.nvhuong.devworkbench.common.component.FallbackAwareRSyntaxTextArea;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemFileChooser;
import com.nvhuong.devworkbench.common.component.ToolWindowActionToolbar;
import com.nvhuong.devworkbench.common.component.ToolWindowChrome;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.component.button.ClearButton;
import com.nvhuong.devworkbench.common.component.button.CopyButton;
import com.nvhuong.devworkbench.common.component.button.ModernButtonFactory;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.util.*;
import lombok.extern.slf4j.Slf4j;
import org.benf.cfr.reader.api.CfrDriver;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * JavaBảng dịch ngược
 * sử dụngCFRTrình dịch ngược hỗ trợ xemJAR、ZIP、Classtập tin
 *
 * @author nvhuong
 */
@Slf4j
public class DecompilerPanel extends JPanel {

    private static final int ROOT_TOP = 8;
    private static final int ROOT_LEFT = 10;
    private static final int ROOT_BOTTOM = 8;
    private static final int ROOT_RIGHT = 10;
    private static final int SECTION_GAP = 6;

    private static final String CLASS_EXTENSION = ".class";
    private static final String JAR_EXTENSION = ".jar";
    private static final String ZIP_EXTENSION = ".zip";
    private static final String WAR_EXTENSION = ".war";
    private static final String JAVA_EXTENSION = ".java";

    private JTextField filePathField;
    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private RSyntaxTextArea codeArea;
    private JLabel statusLabel;
    private JLabel compressionInfoLabel; // Nhãn hiển thị thông tin nén

    private File currentFile;
    private transient JarFile currentJarFile;
    private transient ZipFile currentZipFile;
    private final Map<String, byte[]> classFileCache = new HashMap<>();

    public DecompilerPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, SECTION_GAP));
        setBorder(BorderFactory.createEmptyBorder(ROOT_TOP, ROOT_LEFT, ROOT_BOTTOM, ROOT_RIGHT));
        ToolWindowSurfaceStyle.applyCard(this);

        add(createFileSelectionPanel(), BorderLayout.NORTH);

        JSplitPane splitPane = ToolWindowChrome.createHorizontalInvisibleInnerSplitPane(
                createTreePanel(),
                createCodePanel(),
                ToolWindowChrome.DEFAULT_SIDE_WIDTH
        );
        splitPane.setResizeWeight(0.3);
        add(splitPane, BorderLayout.CENTER);

        add(createStatusPanel(), BorderLayout.SOUTH);
    }

    /**
     * Tạo bảng chọn tập tin - Tối ưu hóa bố cục và hiệu ứng hình ảnh
     */
    private JPanel createFileSelectionPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel panel = new JPanel(new BorderLayout(10, 6));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JLabel sectionTitle = createSectionTitle(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_SELECT_JAR));
        panel.add(sectionTitle, BorderLayout.NORTH);

        JPanel fileInfoPanel = new JPanel(new BorderLayout(5, 0));
        fileInfoPanel.setOpaque(false);
        filePathField = new JTextField();
        filePathField.setEditable(false);
        filePathField.setFocusable(false);
        ToolWindowSurfaceStyle.applyTextComponentInput(filePathField);
        fileInfoPanel.add(filePathField, BorderLayout.CENTER);

        JButton browseButton = ModernButtonFactory.createCompactButton(
                DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_BROWSE),
                false,
                "icons/file.svg"
        );
        browseButton.setToolTipText(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_SELECT_FILE_PROMPT));
        browseButton.addActionListener(e -> browseFile());

        JButton clearButton = new ClearButton(IconUtil.SIZE_SMALL);
        clearButton.setToolTipText(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_CLEAR));
        clearButton.addActionListener(e -> clearAll());

        JPanel buttonPanel = ToolWindowActionToolbar.inlineRight(browseButton, clearButton);
        fileInfoPanel.add(buttonPanel, BorderLayout.EAST);

        panel.add(fileInfoPanel, BorderLayout.CENTER);

        JLabel dragDropLabel = new JLabel(
                DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_DRAG_DROP_HINT_TO_BELOW),
                SwingConstants.CENTER
        );
        dragDropLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.ITALIC, -1));
        dragDropLabel.setForeground(ModernColors.getTextSecondary());
        panel.add(dragDropLabel, BorderLayout.SOUTH);

        wrapper.add(panel, BorderLayout.CENTER);
        wrapper.add(new JSeparator(SwingConstants.HORIZONTAL), BorderLayout.SOUTH);

        return wrapper;
    }

    /**
     * Tạo một bảng cây tập tin
     */
    private JPanel createTreePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, SECTION_GAP));
        panel.setOpaque(false);

        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        topPanel.setOpaque(false);

        JLabel label = createSectionTitle(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_TREE_TITLE));
        topPanel.add(label, BorderLayout.WEST);

        compressionInfoLabel = new JLabel("");
        compressionInfoLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        compressionInfoLabel.setForeground(ModernColors.getTextSecondary());
        compressionInfoLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        topPanel.add(compressionInfoLabel, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_NO_FILE));
        treeModel = new DefaultTreeModel(root);
        fileTree = new JTree(treeModel);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        fileTree.setCellRenderer(new FileTreeCellRenderer());

        // Bấm để mở tập tin hoặc mở rộng nó/Thu gọn mục lục (phản hồi nhanh hơn)
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Chỉ các nhấp chuột trái được xử lý
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object userObject = node.getUserObject();

                        if (userObject instanceof FileNodeData fileData) {
                            if (fileData.isDirectory) {
                                // Mục lục: Click để mở rộng/Bỏ đi
                                if (fileTree.isExpanded(path)) {
                                    fileTree.collapsePath(path);
                                } else {
                                    fileTree.expandPath(path);
                                }
                                e.consume();
                            } else {
                                // Tập tin: Bấm để mở
                                handleTreeNodeClick(node);
                                e.consume();
                            }
                        }
                    }
                }
            }
        });

        // Đã thêm hỗ trợ nhập phím
        fileTree.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    TreePath path = fileTree.getSelectionPath();
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        handleTreeNodeClick(node);
                        e.consume();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(fileTree);
        ToolWindowSurfaceStyle.applyTreeScrollPaneCard(scrollPane, fileTree);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Đã thêm hỗ trợ kéo vào bảng cây
        setupDragAndDrop(scrollPane);

        JButton expandAllBtn = createToolbarIconButton(
                DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_EXPAND_ALL),
                "icons/expand.svg",
                () -> expandTree(fileTree, 3));

        JButton collapseAllBtn = createToolbarIconButton(
                DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_COLLAPSE_ALL),
                "icons/collapse.svg",
                () -> collapseTree(fileTree));

        JComponent separator1 = createToolbarSeparator();

        JButton sortByNameBtn = createToolbarIconButton(
                DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_SORT_BY_NAME),
                "icons/words.svg",
                this::sortTreeByName);

        JButton sortBySizeBtn = createToolbarIconButton(
                DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_SORT_BY_SIZE),
                "icons/detail.svg",
                this::sortTreeBySize);

        JPanel buttonPanel = ToolWindowActionToolbar.inlineLeft(
                expandAllBtn,
                collapseAllBtn,
                separator1,
                sortByNameBtn,
                sortBySizeBtn
        );
        JPanel footerPanel = new JPanel(new BorderLayout(0, 4));
        footerPanel.setOpaque(false);
        footerPanel.add(new JSeparator(SwingConstants.HORIZONTAL), BorderLayout.NORTH);
        footerPanel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(footerPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Tạo bảng hiển thị mã - Tối ưu hóa thanh công cụ và bố cục
     */
    private JPanel createCodePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, SECTION_GAP));
        panel.setOpaque(false);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel label = createSectionTitle(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_OUTPUT));
        headerPanel.add(label, BorderLayout.WEST);

        JButton copyBtn = new CopyButton();
        copyBtn.setToolTipText(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_COPY_CODE));
        copyBtn.addActionListener(e -> copyCode());

        JPanel toolPanel = ToolWindowActionToolbar.inlineRight(copyBtn);
        headerPanel.add(toolPanel, BorderLayout.EAST);

        panel.add(headerPanel, BorderLayout.NORTH);

        // Tạo một trình soạn thảo mã
        codeArea = new FallbackAwareRSyntaxTextArea();
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setEditable(false);
        codeArea.setMargin(new Insets(10, 10, 10, 10));

        // Tải thống nhất các chủ đề, phông chữ soạn thảo và bản vẽ dự phòng cho các ký tự bị thiếu
        EditorThemeUtil.loadTheme(codeArea);

        RTextScrollPane scrollPane = new RTextScrollPane(codeArea);
        scrollPane.setFoldIndicatorEnabled(true);
        scrollPane.setLineNumbersEnabled(true);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(scrollPane);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Đã thêm hỗ trợ kéo và thả vào bảng mã
        setupDragAndDrop(codeArea);

        return panel;
    }

    /**
     * Tạo một thanh trạng thái - Tối ưu hóa kiểu dáng và sự tách biệt
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), BorderLayout.NORTH);

        statusLabel = new JLabel(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_READY));
        statusLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        statusLabel.setForeground(ModernColors.getTextSecondary());

        JPanel statusContent = new JPanel(new BorderLayout());
        statusContent.setOpaque(false);
        statusContent.add(statusLabel, BorderLayout.WEST);
        panel.add(statusContent, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        return label;
    }

    private JButton createToolbarIconButton(String tooltip, String iconPath, Runnable action) {
        JButton button = new JButton(IconUtil.createThemed(iconPath, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        if (action != null) {
            button.addActionListener(e -> action.run());
        }
        return button;
    }

    private JComponent createToolbarSeparator() {
        JPanel separator = new JPanel();
        Dimension size = new Dimension(1, 20);
        separator.setOpaque(true);
        separator.setBackground(ModernColors.getDividerBorderColor());
        separator.setPreferredSize(size);
        separator.setMinimumSize(size);
        separator.setMaximumSize(size);
        return separator;
    }

    /**
     * Đặt hỗ trợ kéo và thả
     */
    private void setupDragAndDrop(JComponent component) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        new DropTarget(component, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (isDragAcceptable(dtde)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    // Thêm phản hồi trực quan
                    component.putClientProperty(FlatClientProperties.OUTLINE, ModernColors.getPrimary());
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                // Loại bỏ phản hồi trực quan
                component.putClientProperty(FlatClientProperties.OUTLINE, null);
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                // Loại bỏ phản hồi trực quan
                component.putClientProperty(FlatClientProperties.OUTLINE, null);

                try {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);

                        @SuppressWarnings("unchecked")
                        List<File> droppedFiles = (List<File>) dtde.getTransferable()
                                .getTransferData(DataFlavor.javaFileListFlavor);

                        if (!droppedFiles.isEmpty()) {
                            File file = droppedFiles.get(0);
                            loadFile(file);
                            dtde.dropComplete(true);
                        } else {
                            dtde.dropComplete(false);
                        }
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (Exception e) {
                    log.error("Failed to handle dropped file", e);
                    dtde.dropComplete(false);
                    NotificationCenter.showError(
                            DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_LOAD_ERROR) + ": " + e.getMessage()
                    );
                }
            }
        });
    }

    /**
     * Kiểm tra xem dữ liệu được kéo có được chấp nhận hay không
     */
    private boolean isDragAcceptable(DropTargetDragEvent dtde) {
        return dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    /**
     * Duyệt và chọn tập tin
     */
    private void browseFile() {
        SystemFileChooser fileChooser = FileChooserUtil.createOpenFileChooser(
                "plugin.decompiler.open",
                DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_SELECT_FILE_PROMPT));
        SystemFileChooser.FileNameExtensionFilter filter = FileChooserUtil.extensionFilter(
                "JAR/WAR/Class/Zip Files (*.jar, *.war, *.class, *.zip)", "jar", "war", "class", "zip");
        fileChooser.setFileFilter(filter);

        if (currentFile != null && currentFile.getParentFile() != null) {
            fileChooser.setCurrentDirectory(currentFile.getParentFile());
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == SystemFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            loadFile(selectedFile);
        }
    }

    /**
     * Tải tập tin
     */
    private void loadFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        String fileName = file.getName().toLowerCase();
        if (!fileName.endsWith(JAR_EXTENSION) && !fileName.endsWith(CLASS_EXTENSION) &&
                !fileName.endsWith(ZIP_EXTENSION) && !fileName.endsWith(WAR_EXTENSION)) {
            NotificationCenter.showError(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_UNSUPPORTED_FILE));
            return;
        }

        // Đóng các tập tin đã mở trước đó
        closeCurrentJar();

        currentFile = file;
        filePathField.setText(file.getAbsolutePath());

        // Tải tập tin trong một chủ đề nền
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                statusLabel.setText(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_LOADING));
                try {
                    if (fileName.endsWith(JAR_EXTENSION) || fileName.endsWith(WAR_EXTENSION)) {
                        // JAR và WAR Sử dụng cả hai JarFile Tải(WAR Bản chất là đặc biệt JAR）
                        loadJarFile(file);
                    } else if (fileName.endsWith(ZIP_EXTENSION)) {
                        loadZipFile(file);
                    } else if (fileName.endsWith(CLASS_EXTENSION)) {
                        loadClassFile(file);
                    }
                } catch (Exception e) {
                    log.error("Failed to load file: {}", file.getAbsolutePath(), e);
                    SwingUtilities.invokeLater(() -> {
                        NotificationCenter.showError(
                                DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_LOAD_ERROR) + ": " + e.getMessage()
                        );
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                statusLabel.setText(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_FILE_INFO) + ": " +
                        file.getName() + " (" + formatFileSize(file.length()) + ")");
            }
        };
        worker.execute();
    }

    /**
     * trọng tảiJARtập tin
     */
    private void loadJarFile(File file) throws IOException {
        currentJarFile = new JarFile(file);
        classFileCache.clear();

        // Được tạo cho nút gốcFileNodeData
        FileNodeData rootData = new FileNodeData(file.getName(), true, false);
        rootData.fullPath = file.getName();
        rootData.size = file.length(); // Đặt thành  JAR Kích thước file thực tế (sau khi nén)
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootData);
        Map<String, DefaultMutableTreeNode> packageNodes = new HashMap<>();

        long uncompressedTotal = 0; // Thống kê tổng kích thước không nén
        Enumeration<JarEntry> entries = currentJarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // Lưu trữ tất cả các tệp không có thư mục (bao gồm class、jar/zip、tệp văn bản và các loại tệp không xác định khác)
            if (!entry.isDirectory()) {
                try (InputStream is = currentJarFile.getInputStream(entry)) {
                    classFileCache.put(entryName, is.readAllBytes());
                }
                uncompressedTotal += entry.getSize(); // Kích thước không nén tích lũy
            }

            // Lưu ý:entry.getSize() là kích thước chưa nén nên tổng kích thước của các file con sẽ lớn hơnJARBản thân tập tin
            addEntryToTree(root, packageNodes, entryName, entry.isDirectory(), entry.getSize());
        }

        // Tính kích thước của tất cả các thư mục con (không sửa đổi nút gốc, vì nút gốc đã được đặt thành thựcJARkích thước)
        calculateDirectorySizesExceptRoot(root);

        // Tính tỷ số nén
        long compressedSize = file.length();
        final long finalUncompressedTotal = uncompressedTotal;

        SwingUtilities.invokeLater(() -> {
            treeModel.setRoot(root);
            expandTree(fileTree, 2);

            // Hiển thị thông tin nén ở đầu cây (luôn hiển thị)
            if (finalUncompressedTotal > 0) {
                double compressionRatio = (1 - (double) compressedSize / finalUncompressedTotal) * 100;
                String compressionRatioStr = String.format("%.1f%%", compressionRatio);

                compressionInfoLabel.setText(DecompilerI18n.t(
                        MessageKeys.TOOLBOX_DECOMPILER_COMPRESSION_INFO_FORMAT,
                        DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_ACTUAL_SIZE),
                        formatFileSize(compressedSize),
                        DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_UNCOMPRESSED_SIZE),
                        formatFileSize(finalUncompressedTotal),
                        DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_COMPRESSION_RATIO),
                        compressionRatioStr
                ));

                compressionInfoLabel.setToolTipText(DecompilerI18n.t(
                        MessageKeys.TOOLBOX_DECOMPILER_COMPRESSION_TOOLTIP_JAR,
                        formatFileSize(compressedSize),
                        formatFileSize(finalUncompressedTotal),
                        compressionRatioStr
                ));
            }
        });
    }

    /**
     * trọng tảiZIPtập tin
     */
    private void loadZipFile(File file) throws IOException {
        currentZipFile = new ZipFile(file);
        classFileCache.clear();

        // Được tạo cho nút gốcFileNodeData
        FileNodeData rootData = new FileNodeData(file.getName(), true, false);
        rootData.fullPath = file.getName();
        rootData.size = file.length(); // Đặt thành  ZIP Kích thước file thực tế (sau khi nén)
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootData);
        Map<String, DefaultMutableTreeNode> packageNodes = new HashMap<>();

        long uncompressedTotal = 0; // Thống kê tổng kích thước không nén
        Enumeration<? extends ZipEntry> entries = currentZipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String entryName = entry.getName();

            // Lưu trữ tất cả các tệp không có thư mục (bao gồm class、jar/zip、tệp văn bản và các loại tệp không xác định khác)
            if (!entry.isDirectory()) {
                try (InputStream is = currentZipFile.getInputStream(entry)) {
                    classFileCache.put(entryName, is.readAllBytes());
                }
                uncompressedTotal += entry.getSize(); // Kích thước không nén tích lũy
            }

            // Lưu ý:entry.getSize() là kích thước chưa nén nên tổng kích thước của các file con sẽ lớn hơnZIPBản thân tập tin
            addEntryToTree(root, packageNodes, entryName, entry.isDirectory(), entry.getSize());
        }

        // Tính kích thước của tất cả các thư mục con (không sửa đổi nút gốc, vì nút gốc đã được đặt thành thựcZIPkích thước)
        calculateDirectorySizesExceptRoot(root);

        // Tính tỷ số nén
        long compressedSize = file.length();
        final long finalUncompressedTotal = uncompressedTotal;

        SwingUtilities.invokeLater(() -> {
            treeModel.setRoot(root);
            expandTree(fileTree, 2);

            // Hiển thị thông tin nén ở đầu cây (luôn hiển thị)
            if (finalUncompressedTotal > 0) {
                double compressionRatio = (1 - (double) compressedSize / finalUncompressedTotal) * 100;
                String compressionRatioStr = String.format("%.1f%%", compressionRatio);

                compressionInfoLabel.setText(DecompilerI18n.t(
                        MessageKeys.TOOLBOX_DECOMPILER_COMPRESSION_INFO_FORMAT,
                        DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_ACTUAL_SIZE),
                        formatFileSize(compressedSize),
                        DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_UNCOMPRESSED_SIZE),
                        formatFileSize(finalUncompressedTotal),
                        DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_COMPRESSION_RATIO),
                        compressionRatioStr
                ));

                compressionInfoLabel.setToolTipText(DecompilerI18n.t(
                        MessageKeys.TOOLBOX_DECOMPILER_COMPRESSION_TOOLTIP_ZIP,
                        formatFileSize(compressedSize),
                        formatFileSize(finalUncompressedTotal),
                        compressionRatioStr
                ));
            }
        });
    }


    /**
     * Tải đơnClasstập tin
     */
    private void loadClassFile(File file) throws IOException {
        classFileCache.clear();
        byte[] classBytes = Files.readAllBytes(file.toPath());
        classFileCache.put(file.getName(), classBytes);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(file.getName());
        root.setUserObject(new FileNodeData(file.getName(), false, true));

        SwingUtilities.invokeLater(() -> {
            treeModel.setRoot(root);
            fileTree.expandRow(0);
        });
    }

    /**
     * Thêm các mục vào cây
     */
    private void addEntryToTree(DefaultMutableTreeNode root, Map<String, DefaultMutableTreeNode> packageNodes,
                                String entryName, boolean isDirectory, long size) {
        String[] parts = entryName.split("/");
        DefaultMutableTreeNode parentNode = root;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            // Xây dựng đường dẫn đầy đủ của nút hiện tại
            if (!currentPath.isEmpty()) {
                currentPath.append("/");
            }
            currentPath.append(part);
            String nodePath = currentPath.toString();

            boolean isLastPart = (i == parts.length - 1);
            boolean isFile = isLastPart && !isDirectory;
            boolean isClassFile = isFile && part.endsWith(CLASS_EXTENSION);
            boolean isJarFile = isFile && (part.toLowerCase().endsWith(JAR_EXTENSION) ||
                    part.toLowerCase().endsWith(ZIP_EXTENSION) ||
                    part.toLowerCase().endsWith(WAR_EXTENSION));

            // Kiểm tra xem nút đã tồn tại chưa
            DefaultMutableTreeNode node = packageNodes.get(nodePath);
            if (node == null) {
                // Tạo nút mới
                FileNodeData nodeData = new FileNodeData(part, !isFile, isClassFile);
                nodeData.fullPath = nodePath;
                nodeData.isJarFile = isJarFile;
                // Chỉ đặt kích thước cho tệp, kích thước thư mục sẽ được tính sau
                if (isFile && size >= 0) {
                    nodeData.size = size;
                }
                node = new DefaultMutableTreeNode(nodeData);
                packageNodes.put(nodePath, node);
                parentNode.add(node);
            }

            // Chuyển sang cấp độ tiếp theo
            parentNode = node;
        }
    }

    /**
     * Tính toán đệ quy kích thước thư mục (tổng của tất cả các tệp con)
     */
    private long calculateDirectorySizes(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (!(userObject instanceof FileNodeData fileData)) {
            return 0;
        }

        // Nếu đó là một tập tin, kích thước của nó sẽ được trả về trực tiếp
        if (!fileData.isDirectory) {
            return fileData.size;
        }

        // Nếu đó là một thư mục, kích thước của tất cả các nút con được tính toán đệ quy
        long totalSize = 0;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) node.getChildAt(i);
            totalSize += calculateDirectorySizes(childNode);
        }

        // Đặt tổng kích thước của thư mục
        fileData.size = totalSize;
        return totalSize;
    }

    /**
     * Tính toán đệ quy kích thước của tất cả các thư mục con mà không sửa đổi nút gốc
     * （Được sử dụng choJAR/ZIPtệp, nút gốc hiển thị kích thước tệp thực tế và các nút con hiển thị kích thước không nén)
     */
    private void calculateDirectorySizesExceptRoot(DefaultMutableTreeNode root) {
        // Chỉ có nút con trực tiếp của nút gốc và con cháu của chúng mới được tính
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) root.getChildAt(i);
            calculateDirectorySizes(childNode);
        }
    }

    /**
     * Xử lý các lần nhấp chuột vào nút cây
     */
    private void handleTreeNodeClick(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();

        if (userObject instanceof FileNodeData fileData) {
            if (fileData.isClassFile) {
                decompileAndShow(fileData.fullPath != null ? fileData.fullPath : fileData.name);
            } else if (fileData.isJarFile) {
                // Xử lý những cái lồng nhauJAR/ZIPtập tin
                loadNestedJar(fileData.fullPath, node);
            } else if (!fileData.isDirectory) {
                // Hãy thử hiển thị một tập tin văn bản
                showTextFile(fileData.fullPath != null ? fileData.fullPath : fileData.name);
            }
        } else if (userObject instanceof String fileName && fileName.endsWith(CLASS_EXTENSION)) {
            // đơnclassTrạng thái của tập tin
            decompileAndShow(fileName);
        }
    }

    /**
     * Tải lồng nhauJAR/ZIPtập tin
     */
    private void loadNestedJar(String jarPath, DefaultMutableTreeNode parentNode) {
        // Nếu nút đã có nút con, điều đó có nghĩa là nút đó đã được mở rộng và trả về trực tiếp
        if (parentNode.getChildCount() > 0) {
            return;
        }

        byte[] jarBytes = classFileCache.get(jarPath);
        if (jarBytes == null) {
            NotificationCenter.showWarning("JAR file not found in cache: " + jarPath);
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    statusLabel.setText(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_LOADING) + ": " + jarPath);

                    // Tạo tập tin tạm thời để lưu lồng nhauJAR
                    File tempJar = Files.createTempFile("nested-jar-", ".jar").toFile();
                    tempJar.deleteOnExit();
                    Files.write(tempJar.toPath(), jarBytes);

                    // sử dụngJarFileĐọc lồng nhauJAR
                    Map<String, DefaultMutableTreeNode> packageNodes = new HashMap<>();
                    try (JarFile nestedJar = new JarFile(tempJar)) {
                        Enumeration<JarEntry> entries = nestedJar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String entryName = entry.getName();
                            String fullEntryPath = jarPath + "!/" + entryName;

                            // Lưu trữ tất cả các tệp không có thư mục (bao gồm class、jar/zip、tệp văn bản và các loại tệp không xác định khác)
                            if (!entry.isDirectory()) {
                                try (InputStream is = nestedJar.getInputStream(entry)) {
                                    classFileCache.put(fullEntryPath, is.readAllBytes());
                                }
                            }

                            // Thêm vào cây (so với hiện tạiJARnút)
                            addNestedEntryToTree(parentNode, packageNodes, entryName,
                                    fullEntryPath, entry.isDirectory(), entry.getSize());
                        }
                    }

                    // Tính toán lồng nhauJARKích thước của tất cả các thư mục trong
                    calculateDirectorySizes(parentNode);

                    // Xóa các tập tin tạm thời
                    Files.deleteIfExists(tempJar.toPath());

                } catch (Exception e) {
                    log.error("Failed to load nested JAR: {}", jarPath, e);
                    SwingUtilities.invokeLater(() ->
                            NotificationCenter.showError("Failed to load nested JAR: " + e.getMessage())
                    );
                }
                return null;
            }

            @Override
            protected void done() {
                // Làm mới màn hình cây
                treeModel.nodeStructureChanged(parentNode);
                // Mở rộng nút
                TreePath path = new TreePath(parentNode.getPath());
                fileTree.expandPath(path);
                statusLabel.setText(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_READY));
            }
        };
        worker.execute();
    }

    /**
     * Thêm lồng nhauJARcác mục vào trong cây
     */
    private void addNestedEntryToTree(DefaultMutableTreeNode parentNode,
                                      Map<String, DefaultMutableTreeNode> packageNodes,
                                      String entryName, String fullPath, boolean isDirectory, long size) {
        String[] parts = entryName.split("/");
        DefaultMutableTreeNode currentParent = parentNode;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            // Xây dựng một đường dẫn tương đối đến nút hiện tại
            if (!currentPath.isEmpty()) {
                currentPath.append("/");
            }
            currentPath.append(part);
            String nodePath = currentPath.toString();

            boolean isLastPart = (i == parts.length - 1);
            boolean isFile = isLastPart && !isDirectory;
            boolean isClassFile = isFile && part.endsWith(CLASS_EXTENSION);
            boolean isJarFile = isFile && (part.toLowerCase().endsWith(JAR_EXTENSION) ||
                    part.toLowerCase().endsWith(ZIP_EXTENSION) ||
                    part.toLowerCase().endsWith(WAR_EXTENSION));

            // Kiểm tra xem nút đã tồn tại chưa
            DefaultMutableTreeNode node = packageNodes.get(nodePath);
            if (node == null) {
                // Tạo nút mới
                FileNodeData nodeData = new FileNodeData(part, !isFile, isClassFile);
                // Sử dụng đường dẫn đầy đủ (bao gồm cả parentJARcon đường)
                nodeData.fullPath = fullPath.substring(0, fullPath.lastIndexOf("!/") + 2) + nodePath;
                nodeData.isJarFile = isJarFile;
                // Chỉ đặt kích thước cho tệp, kích thước thư mục sẽ được tính sau
                if (isFile && size >= 0) {
                    nodeData.size = size;
                }
                node = new DefaultMutableTreeNode(nodeData);
                packageNodes.put(nodePath, node);
                currentParent.add(node);
            }

            // Chuyển sang cấp độ tiếp theo
            currentParent = node;
        }
    }

    /**
     * Dịch ngược và hiển thị mã
     */
    private void decompileAndShow(String className) {
        byte[] classBytes = classFileCache.get(className);
        if (classBytes == null) {
            codeArea.setText("// " + DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_ERROR) +
                    ": Class file not found");
            return;
        }

        // Xử lý đặc biệt:module-info.class Vâng Java 9+ mô tả mô-đun,CFR Không thể dịch ngược được
        if (className.toLowerCase().endsWith("module-info.class") ||
                className.toLowerCase().contains("/module-info.class")) {
            codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
            codeArea.setText("""
                    // module-info.class - Java Module Descriptor
                    // This file cannot be decompiled as it's a special module descriptor file.
                    //
                    // Module info files define:
                    // - Module name
                    // - Required modules
                    // - Exported packages
                    // - Provided services
                    //
                    // To view module information, use: jar --describe-module --file=<jar-file>
                    """);
            statusLabel.setText("Module descriptor file (not decompilable)");
            return;
        }

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                statusLabel.setText(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_DECOMPILING) + ": " + className);
                try {
                    return decompileClass(classBytes, className);
                } catch (Exception e) {
                    log.error("Failed to decompile class: {}", className, e);
                    return "// " + DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_ERROR) +
                            ": " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String code = get();
                    // cài đặt Java Syntax highlighting (the decompiled code is Java）
                    codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                    codeArea.setText(code);
                    codeArea.setCaretPosition(0);

                    // Hiển thị thông tin lớp học
                    String classInfo = extractClassInfo(classBytes);
                    statusLabel.setText(classInfo);
                } catch (Exception e) {
                    log.error("Error displaying decompiled code", e);
                    codeArea.setText("// " + DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_ERROR));
                }
            }
        };
        worker.execute();
    }

    /**
     * sử dụngCFRDịch ngượcClassMã byte
     */
    private String decompileClass(byte[] classBytes, String className) {
        try {
            // Tạo một thư mục tạm thời để lưuclasstập tin
            File tempDir = Files.createTempDirectory("cfr-decompile").toFile();

            // cứu class tập tin
            File classFile = new File(tempDir, className);
            File parentDir = classFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                log.warn("Failed to create parent directory: {}", parentDir);
            }
            Files.write(classFile.toPath(), classBytes);

            // Tạo một thư mục đầu ra
            File outputDir = new File(tempDir, "output");
            if (!outputDir.mkdirs() && !outputDir.exists()) {
                log.warn("Failed to create output directory: {}", outputDir);
            }

            // CFRcấu hình - Xuất ra tập tin
            Map<String, String> options = new HashMap<>();
            options.put("outputpath", outputDir.getAbsolutePath());  // sử dụng outputpath Thay vào đó outputdir
            options.put("showversion", "false");  // Không hiển thị thông tin phiên bản
            options.put("hideutf", "false");      // trình diễn UTF-8 nhân vật
            options.put("innerclasses", "true");  // Dịch ngược các lớp bên trong
            options.put("skipbatchinnerclasses", "false");  // Đừng bỏ qua việc phân nhóm các lớp bên trong

            log.debug("CFR decompiling: {} to {}", classFile.getAbsolutePath(), outputDir.getAbsolutePath());

            // Thực hiện dịch ngược
            CfrDriver driver = new CfrDriver.Builder()
                    .withOptions(options)
                    .build();
            driver.analyse(Collections.singletonList(classFile.getAbsolutePath()));

            // Đọc phiên bản đã dịch ngược Java tập tin
            String result = readDecompiledFile(outputDir, className);

            log.debug("Decompilation result length: {}", result.length());

            // Làm sạch các tập tin tạm thời
            deleteDirectory(tempDir);

            return !result.isEmpty() ? result : "// Failed to decompile: No output from CFR";

        } catch (Exception e) {
            log.error("CFR decompilation failed", e);
            return "// Decompilation failed: " + e.getMessage();
        }
    }

    /**
     * Đọc phiên bản đã dịch ngược từ thư mục đầu ra Java tập tin
     */
    private String readDecompiledFile(File outputDir, String className) {
        try {
            log.debug("Reading decompiled file for class: {} from dir: {}", className, outputDir.getAbsolutePath());

            // Xử lý việc lồng nhau JAR con đường:BOOT-INF/lib/xxx.jar!/com/example/Test.class
            // Trích xuất đường dẫn lớp thực tế (!/ các phần tiếp theo)
            String actualClassName = className;
            if (className.contains("!/")) {
                actualClassName = className.substring(className.lastIndexOf("!/") + 2);
                log.debug("Detected nested JAR path, extracted actual class: {}", actualClassName);
            }

            // Sẽ className Chuyển đổi sang đường dẫn tập tin
            // Ví dụ: com/example/Test.class -> com/example/Test.java
            String javaFileName = actualClassName.replace(CLASS_EXTENSION, JAVA_EXTENSION);
            File javaFile = new File(outputDir, javaFileName);

            log.debug("Looking for Java file at: {}", javaFile.getAbsolutePath());

            if (javaFile.exists()) {
                log.debug("Found Java file directly at expected path");
                String content = Files.readString(javaFile.toPath(), StandardCharsets.UTF_8);
                // Dọn dẹp thông tin gỡ lỗi có thể
                return cleanupDecompiledCode(content);
            }

            // Nếu đường dẫn trực tiếp không tồn tại, hãy thử tìm trong thư mục đầu ra .java tập tin
            log.debug("Java file not found at expected path, searching in output directory...");
            File[] javaFiles = findJavaFiles(outputDir);
            log.debug("Found {} Java files in output directory", javaFiles.length);

            if (javaFiles.length > 0) {
                log.debug("Using first found Java file: {}", javaFiles[0].getAbsolutePath());
                // Trả về cái đầu tiên được tìm thấy Java tập tin
                String content = Files.readString(javaFiles[0].toPath(), StandardCharsets.UTF_8);
                return cleanupDecompiledCode(content);
            }

            // Liệt kê nội dung thư mục đầu ra cho mục đích gỡ lỗi
            log.debug("Output directory contents:");
            listDirectoryContents(outputDir, "  ");

            return "// No Java file found in output directory\n// Expected: " + javaFileName +
                    "\n// Output dir: " + outputDir.getAbsolutePath();
        } catch (IOException e) {
            log.error("Failed to read decompiled file", e);
            return "// Failed to read decompiled file: " + e.getMessage();
        }
    }

    /**
     * Liệt kê nội dung thư mục (để gỡ lỗi)
     */
    private void listDirectoryContents(File dir, String indent) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                log.debug("{}{}  ({})", indent, file.getName(), file.isDirectory() ? "DIR" : "FILE");
                if (file.isDirectory()) {
                    listDirectoryContents(file, indent + "  ");
                }
            }
        }
    }

    /**
     * Tìm kiếm đệ quy tất cả các mục trong một thư mục .java tập tin
     */
    private File[] findJavaFiles(File dir) {
        if (!dir.isDirectory()) {
            return new File[0];
        }

        List<File> javaFiles = new ArrayList<>();
        findJavaFilesRecursive(dir, javaFiles);
        return javaFiles.toArray(new File[0]);
    }

    /**
     * Tìm kiếm đệ quy Java tập tin
     */
    private void findJavaFilesRecursive(File dir, List<File> javaFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findJavaFilesRecursive(file, javaFiles);
                } else if (file.getName().endsWith(JAVA_EXTENSION)) {
                    javaFiles.add(file);
                }
            }
        }
    }

    /**
     * Làm sạch mã đã dịch ngược và xóa thông tin gỡ lỗi
     */
    private String cleanupDecompiledCode(String code) {
        // Chia theo hàng
        String[] lines = code.split("\n");
        StringBuilder cleaned = new StringBuilder();
        boolean startedOutput = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // Bỏ qua các dòng thông tin gỡ lỗi hàng đầu
            if (!startedOutput && isDebugLine(trimmed)) {
                continue;
            }

            // Khi gặp thông tin không gỡ lỗi, đầu ra sẽ bắt đầu
            if (!startedOutput && !trimmed.isEmpty() && !isDebugLine(trimmed)) {
                startedOutput = true;
            }

            // Khi đầu ra bắt đầu, hãy giữ mọi thứ (kể cả các dòng trống)
            if (startedOutput) {
                cleaned.append(line).append("\n");
            }
        }

        return cleaned.toString();
    }

    /**
     * Xác định xem đó có phải là dòng thông tin gỡ lỗi hay không
     */
    private boolean isDebugLine(String trimmed) {
        return trimmed.startsWith("Analysing ") ||
                trimmed.startsWith("Processing ") ||
                trimmed.startsWith("Decompiling ") ||
                (trimmed.startsWith("/*") && trimmed.contains("Decompiled with CFR"));
    }

    /**
     * Trích xuất thông tin lớp (số phiên bản, v.v.)
     */
    private String extractClassInfo(byte[] classBytes) {
        if (classBytes.length < 8) {
            return "";
        }

        try {
            // ClassĐịnh dạng tập tin: magic(4) + minor(2) + major(2)
            int major = ((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF);
            String javaVersion = getJavaVersion(major);

            return DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_CLASS_VERSION) + ": " + major +
                    " (" + DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_JAVA_VERSION) + ": " + javaVersion + ")";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Hiển thị nội dung tập tin văn bản
     */
    private void showTextFile(String fileName) {
        try {
            String content = null;

            // Đọc từ bộ đệm trước (hỗ trợ lồng nhau JAR các tập tin trong )
            byte[] cachedBytes = classFileCache.get(fileName);
            if (cachedBytes != null) {
                content = new String(cachedBytes, StandardCharsets.UTF_8);
            }
            // Mở từ cái hiện tại JAR Đọc từ tập tin
            else if (currentJarFile != null) {
                JarEntry entry = currentJarFile.getJarEntry(fileName);
                if (entry != null) {
                    try (InputStream is = currentJarFile.getInputStream(entry)) {
                        content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            }
            // Mở từ cái hiện tại ZIP Đọc từ tập tin
            else if (currentZipFile != null) {
                ZipEntry entry = currentZipFile.getEntry(fileName);
                if (entry != null) {
                    try (InputStream is = currentZipFile.getInputStream(entry)) {
                        content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
            }

            if (content != null) {
                codeArea.setSyntaxEditingStyle(getSyntaxStyle(fileName));
                codeArea.setText(content);
                codeArea.setCaretPosition(0);
                statusLabel.setText(fileName);
            } else {
                codeArea.setText("// File not found: " + fileName);
                statusLabel.setText("File not found");
            }
        } catch (Exception e) {
            log.error("Failed to read file: {}", fileName, e);
            codeArea.setText("// Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Nhận kiểu đánh dấu cú pháp
     */
    private String getSyntaxStyle(String fileName) {
        String name = fileName.toLowerCase();
        // .java và .class Sử dụng cả hai Java Syntax highlighting (.class Sau khi dịch ngược nó là Java mã số)
        if (name.endsWith(JAVA_EXTENSION) || name.endsWith(CLASS_EXTENSION)) {
            return SyntaxConstants.SYNTAX_STYLE_JAVA;
        }
        if (name.endsWith(".xml")) return SyntaxConstants.SYNTAX_STYLE_XML;
        if (name.endsWith(".json")) return SyntaxConstants.SYNTAX_STYLE_JSON;
        if (name.endsWith(".properties")) return SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE;
        if (name.endsWith(".yaml") || name.endsWith(".yml")) return SyntaxConstants.SYNTAX_STYLE_YAML;
        if (name.endsWith(".html")) return SyntaxConstants.SYNTAX_STYLE_HTML;
        if (name.endsWith(".js")) return SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
        return SyntaxConstants.SYNTAX_STYLE_NONE;
    }

    /**
     * Sao chép mã vào clipboard
     */
    private void copyCode() {
        String code = codeArea.getText();
        if (code != null && !code.isEmpty()) {
            StringSelection selection = new StringSelection(code);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            statusLabel.setText(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_CODE_COPIED));
            NotificationCenter.showSuccess(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_CODE_COPIED));
        }
    }


    /**
     * Xóa tất cả nội dung
     */
    private void clearAll() {
        // Đóng tập tin hiện đang mở
        closeCurrentJar();

        // Xóa đường dẫn tập tin
        currentFile = null;
        filePathField.setText("");

        // Xóa vùng mã
        codeArea.setText("");

        // Xóa thông tin nén
        if (compressionInfoLabel != null) {
            compressionInfoLabel.setText("");
            compressionInfoLabel.setToolTipText(null);
        }

        // Đặt lại cây
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_NO_FILE));
        treeModel.setRoot(root);

        // Cập nhật trạng thái
        statusLabel.setText(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_READY));

        NotificationCenter.showSuccess(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_CLEARED));
    }

    /**
     * Đóng cái hiện đang mởJAR/ZIPtập tin
     */
    private void closeCurrentJar() {
        try {
            if (currentJarFile != null) {
                currentJarFile.close();
                currentJarFile = null;
            }
            if (currentZipFile != null) {
                currentZipFile.close();
                currentZipFile = null;
            }
            classFileCache.clear();
        } catch (IOException e) {
            log.error("Failed to close jar/zip file", e);
        }
    }

    /**
     * Định dạng kích thước tập tin
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " " + DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_BYTES);
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        return String.format("%.2f MB", size / (1024.0 * 1024.0));
    }

    /**
     * LấyJavaSố phiên bản
     */
    private String getJavaVersion(int majorVersion) {
        return switch (majorVersion) {
            case 45 -> "1.1";
            case 46 -> "1.2";
            case 47 -> "1.3";
            case 48 -> "1.4";
            case 49 -> "5";
            case 50 -> "6";
            case 51 -> "7";
            case 52 -> "8";
            case 53 -> "9";
            case 54 -> "10";
            case 55 -> "11";
            case 56 -> "12";
            case 57 -> "13";
            case 58 -> "14";
            case 59 -> "15";
            case 60 -> "16";
            case 61 -> "17";
            case 62 -> "18";
            case 63 -> "19";
            case 64 -> "20";
            case 65 -> "21";
            default -> majorVersion + " (unknown)";
        };
    }

    /**
     * Mở rộng cây đến mức quy định
     */
    private void expandTree(JTree tree, int level) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        expandNode(tree, root, 0, level);
    }

    private void expandNode(JTree tree, DefaultMutableTreeNode node, int currentLevel, int targetLevel) {
        if (currentLevel < targetLevel) {
            tree.expandPath(new TreePath(node.getPath()));
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                expandNode(tree, child, currentLevel + 1, targetLevel);
            }
        }
    }

    /**
     * Nhét toàn bộ cây vào
     */
    private void collapseTree(JTree tree) {
        for (int i = tree.getRowCount() - 1; i >= 1; i--) {
            tree.collapseRow(i);
        }
    }

    /**
     * Sắp xếp các nút cây theo tên
     */
    private void sortTreeByName() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        sortNodeByName(root);
        treeModel.reload();
        expandTree(fileTree, 2);
        statusLabel.setText(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_SORTED_BY_NAME));
    }

    /**
     * Sắp xếp đệ quy các nút con của nút theo tên
     */
    private void sortNodeByName(DefaultMutableTreeNode node) {
        if (node.getChildCount() == 0) {
            return;
        }

        // Nhận tất cả các nút con
        List<DefaultMutableTreeNode> children = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            children.add((DefaultMutableTreeNode) node.getChildAt(i));
        }

        // Sắp xếp theo tên (thư mục đầu tiên, sau đó theo thứ tự bảng chữ cái tên)
        children.sort((n1, n2) -> {
            Object o1 = n1.getUserObject();
            Object o2 = n2.getUserObject();

            if (o1 instanceof FileNodeData f1 && o2 instanceof FileNodeData f2) {
                // Thư mục có trước tập tin
                if (f1.isDirectory != f2.isDirectory) {
                    return f1.isDirectory ? -1 : 1;
                }
                // Các loại giống nhau được sắp xếp theo tên
                return f1.name.compareToIgnoreCase(f2.name);
            }
            return 0;
        });

        // Xóa tất cả các nút con và thêm chúng trở lại theo thứ tự được sắp xếp
        node.removeAllChildren();
        for (DefaultMutableTreeNode child : children) {
            node.add(child);
            // Sắp xếp các nút con theo cách đệ quy
            sortNodeByName(child);
        }
    }

    /**
     * Sắp xếp các nút cây theo kích thước
     */
    private void sortTreeBySize() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        sortNodeBySize(root);
        treeModel.reload();
        expandTree(fileTree, 2);
        statusLabel.setText(DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_SORTED_BY_SIZE));
    }

    /**
     * Sắp xếp đệ quy các nút con của nút theo kích thước
     */
    private void sortNodeBySize(DefaultMutableTreeNode node) {
        if (node.getChildCount() == 0) {
            return;
        }

        // Nhận tất cả các nút con
        List<DefaultMutableTreeNode> children = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            children.add((DefaultMutableTreeNode) node.getChildAt(i));
        }

        // Sắp xếp theo kích thước (giảm dần, tệp lớn trước)
        children.sort((n1, n2) -> {
            Object o1 = n1.getUserObject();
            Object o2 = n2.getUserObject();

            if (o1 instanceof FileNodeData f1 && o2 instanceof FileNodeData f2) {
                // Thư mục có trước tập tin
                if (f1.isDirectory != f2.isDirectory) {
                    return f1.isDirectory ? -1 : 1;
                }
                // Các loại giống nhau được sắp xếp theo thứ tự kích thước giảm dần
                return Long.compare(f2.size, f1.size);
            }
            return 0;
        });

        // Xóa tất cả các nút con và thêm chúng trở lại theo thứ tự được sắp xếp
        node.removeAllChildren();
        for (DefaultMutableTreeNode child : children) {
            node.add(child);
            // Sắp xếp các nút con theo cách đệ quy
            sortNodeBySize(child);
        }
    }

    /**
     * Xóa thư mục
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        try {
            Files.deleteIfExists(dir.toPath());
        } catch (IOException e) {
            log.warn("Failed to delete: {}", dir.getAbsolutePath(), e);
        }
    }

    /**
     * Dữ liệu nút tệp
     */
    private static class FileNodeData {
        String name;
        boolean isDirectory;
        boolean isClassFile;
        boolean isJarFile;
        String fullPath;
        long size; // Kích thước tệp, tính bằng byte, của một thư mục, tổng kích thước của tất cả các tệp con của nó

        FileNodeData(String name, boolean isDirectory, boolean isClassFile) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.isClassFile = isClassFile;
            this.isJarFile = false;
            this.size = 0;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Trình kết xuất cây tập tin - Sử dụng màu sắc SVG Biểu tượng, hiển thị dựa trên loại tập tin
     */
    private static class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final int ICON_SIZE = 16;

        // Biểu tượng bộ đệm để cải thiện hiệu suất
        private static final Map<String, Icon> iconCache = new HashMap<>();

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode node) {
                Object userObject = node.getUserObject();
                if (userObject instanceof FileNodeData fileData) {
                    Icon icon = getIconForFileData(fileData);
                    if (icon != null) {
                        setIcon(icon);
                    }

                    // Hiển thị tập tin/Tên và kích thước thư mục
                    if (fileData.size > 0) {
                        String sizeStr = formatFileSize(fileData.size);

                        // Xác định xem đó có phải là nút gốc (JAR/ZIP/WARtập tin)
                        boolean isRootNode = node.getParent() == null;
                        boolean isArchiveFile = fileData.name.toLowerCase().endsWith(".jar") ||
                                fileData.name.toLowerCase().endsWith(".zip") ||
                                fileData.name.toLowerCase().endsWith(".war");

                        if (isRootNode && isArchiveFile) {
                            // Nút gốc được đánh dấu đặc biệt và hiển thị dưới dạng kích thước thực của nó sau khi nén
                            String compressedLabel = DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_COMPRESSED);
                            setText(fileData.name + " (" + sizeStr + " " + compressedLabel + ")");

                            String tooltipLine1 = DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_ROOT_TOOLTIP_LINE1);
                            String tooltipLine2 = DecompilerI18n.t(MessageKeys.TOOLBOX_DECOMPILER_ROOT_TOOLTIP_LINE2);
                            setToolTipText("<html>" + tooltipLine1 + "<br>" + tooltipLine2 + "</html>");
                        } else {
                            setText(fileData.name + " (" + sizeStr + ")");
                            setToolTipText(null);
                        }
                    } else {
                        setText(fileData.name);
                        setToolTipText(null);
                    }
                }
            }

            return this;
        }

        /**
         * Định dạng kích thước tệp thành định dạng mà con người có thể đọc được
         */
        private String formatFileSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024.0));
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }

        /**
         * Lấy màu tương ứng dựa trên dữ liệu tệp SVG biểu tượng
         */
        private Icon getIconForFileData(FileNodeData fileData) {
            String iconKey = "java-file";

            if (fileData.isDirectory) {
                // Biểu tượng thư mục - Theo trạng thái mở rộng
                iconKey = "group";
            } else if (fileData.isClassFile) {
                // Class Các tập tin cũng được sử dụng java-file biểu tượng
                iconKey = "java-file";
            } else if (fileData.isJarFile) {
                // JAR/ZIP tập tin - Phân biệt dựa trên phần mở rộng
                String fileName = fileData.name.toLowerCase();
                if (fileName.endsWith(".jar")) {
                    iconKey = "jar-file";
                }
            } else {
                // Các tài liệu khác - Phân biệt dựa trên phần mở rộng
                iconKey = getIconKeyByExtension(fileData.name);
            }

            return getOrCreateIcon(iconKey);
        }

        /**
         * Lấy khóa biểu tượng dựa trên phần mở rộng của tệp
         */
        private String getIconKeyByExtension(String fileName) {
            String lowerName = fileName.toLowerCase();

            // .java và .class Sử dụng cả hai java-file biểu tượng
            if (lowerName.endsWith(JAVA_EXTENSION) || lowerName.endsWith(CLASS_EXTENSION)) {
                return "java-file";
            } else if (lowerName.endsWith(".xml")) {
                return "xml-file";
            } else if (lowerName.endsWith(".json")) {
                return "json-file";
            } else if (lowerName.endsWith(".properties")) {
                return "properties-file";
            } else if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
                    lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif") ||
                    lowerName.endsWith(".svg")) {
                return "image-file";
            } else {
                return "text-file";
            }
        }

        /**
         * Nhận hoặc tạo biểu tượng (có bộ nhớ đệm)
         */
        private Icon getOrCreateIcon(String iconKey) {
            return iconCache.computeIfAbsent(iconKey, key -> new FlatSVGIcon("icons/" + key + ".svg", ICON_SIZE, ICON_SIZE));
        }
    }
}
