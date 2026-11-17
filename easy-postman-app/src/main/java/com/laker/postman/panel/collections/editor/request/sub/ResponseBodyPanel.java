package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.component.notification.NotificationCenter;

import cn.hutool.core.util.XmlUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.EasyComboBox;
import com.laker.postman.common.component.FallbackAwareRSyntaxTextArea;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.*;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.http.runtime.file.ResponseFiles;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.*;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.function.Consumer;

/**
 * Bảng điều khiển phản hồi, màn hình hiển thị HTTP Nội dung nội dung phản hồi
 * <p>
 * Chức năng chính:
 * - Syntax highlighting (JSON、XML、HTML、JavaScript、CSS Chờ đợi)
 * - tự động/Định dạng thủ công
 * - Tìm kiếm văn bản
 * - Kiểm soát gói dòng tự động
 * - Tải xuống nội dung phản hồi
 * - Tối ưu hóa xử lý các tập tin lớn
 * </p>
 */
public class ResponseBodyPanel extends JPanel {
    @Getter
    private final RSyntaxTextArea responseBodyPane;
    private final DownloadButton downloadButton;
    @Getter
    private SaveResponseButton saveResponseButton; // Nút lưu phản hồi (chỉHTTPyêu cầu)
    private final SearchButton searchButton; // Nút tìm kiếm
    private HttpResponse currentResponse;
    private final ResponseBodyDownloader downloader;
    private final EasyComboBox<String> syntaxComboBox;
    private final FormatButton formatButton;
    private final JToggleButton autoSortJsonKeysButton;
    private final CopyButton copyButton;
    private final GenerateModelButton generateModelButton;
    private final WrapToggleButton wrapButton;
    private final SearchableTextArea searchableTextArea; // Trình soạn thảo văn bản có chức năng tìm kiếm
    private final JPanel centerPanel;
    private final FileResponsePanel fileResponsePanel;
    private final JButton fileOverviewButton;
    private boolean fileRawPreviewActive;

    // Định nghĩa hằng số
    private static final int LARGE_RESPONSE_THRESHOLD = 500 * 1024; // 500KB threshold
    private static final String CARD_TEXT = "TEXT";
    private static final String CARD_IMAGE = "IMAGE";
    private static final String CARD_MEDIA = "MEDIA";
    private static final String CARD_FILE = "FILE";

    // Thành phần xem trước hình ảnh
    private final JLabel imagePreviewLabel;
    private final MediaResponsePanel mediaResponsePanel;


    private final JLabel sizeWarningLabel;

    public ResponseBodyPanel(boolean enableSaveButton) {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        // Đặt lề
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        responseBodyPane = new FallbackAwareRSyntaxTextArea();
        responseBodyPane.setEditable(false);
        responseBodyPane.setCodeFoldingEnabled(true);
        responseBodyPane.setLineWrap(false); // Tắt tính năng ngắt dòng để cải thiện hiệu suất văn bản lớn
        responseBodyPane.setHighlightCurrentLine(false); // Tắt đánh dấu hàng đã chọn
        // Tắt lời nhắc lớp nổi nhỏ để khớp khung để tránh phần nội dung phản hồi quá dài JSON Loại bỏ nội dung khi cuộn
        responseBodyPane.setShowMatchedBracketPopup(false);
        // Tải thống nhất các chủ đề, phông chữ soạn thảo và bản vẽ dự phòng cho các ký tự bị thiếu
        EditorThemeUtil.loadTheme(responseBodyPane);
        EditorThemeUtil.installViewportClippedTokenPainter(responseBodyPane);

        // sử dụng SearchableTextArea Wrapper, tắt chức năng thay thế (chỉ tìm kiếm)
        searchableTextArea = new SearchableTextArea(responseBodyPane, false);

        // Thành phần xem trước hình ảnh
        imagePreviewLabel = new JLabel();
        imagePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imagePreviewLabel.setVerticalAlignment(SwingConstants.CENTER);
        JScrollPane imageScrollPane = new JScrollPane(imagePreviewLabel);
        ToolWindowSurfaceStyle.applyScrollPaneCard(imageScrollPane);
        imageScrollPane.setBorder(BorderFactory.createEmptyBorder());

        mediaResponsePanel = new MediaResponsePanel();
        fileResponsePanel = new FileResponsePanel(this::downloadFromFileOverview, this::showFileRawPreview);
        downloader = new ResponseBodyDownloader(this::onSavingChanged);

        // sử dụng CardLayout Chuyển đổi giữa tổng quan về văn bản, hình ảnh, phương tiện và tập tin
        centerPanel = new JPanel(new CardLayout());
        ToolWindowSurfaceStyle.applyCard(centerPanel);
        centerPanel.add(searchableTextArea, CARD_TEXT);
        centerPanel.add(imageScrollPane, CARD_IMAGE);
        centerPanel.add(mediaResponsePanel, CARD_MEDIA);
        centerPanel.add(fileResponsePanel, CARD_FILE);
        add(centerPanel, BorderLayout.CENTER);

        JPanel toolBarPanel = new JPanel();
        ToolWindowSurfaceStyle.applyCard(toolBarPanel);
        toolBarPanel.setLayout(new BoxLayout(toolBarPanel, BoxLayout.X_AXIS));
        toolBarPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        // Grammar selection drop-down box
        syntaxComboBox = new EasyComboBox<>(SyntaxType.getDisplayNames(), EasyComboBox.WidthMode.DYNAMIC);
        syntaxComboBox.setFocusable(false);
        toolBarPanel.add(syntaxComboBox);
        toolBarPanel.add(Box.createHorizontalStrut(4)); // khoảng thời gian

        // Thêm nhãn nhắc nhở kích thước
        sizeWarningLabel = new JLabel();
        sizeWarningLabel.setForeground(ModernColors.getWarningDark());
        sizeWarningLabel.setMinimumSize(new Dimension(
                0, sizeWarningLabel.getFontMetrics(sizeWarningLabel.getFont()).getHeight()));
        sizeWarningLabel.setVisible(false);
        toolBarPanel.add(sizeWarningLabel);

        // Không gian linh hoạt, đẩy điều khiển bên phải sang bên phải
        toolBarPanel.add(Box.createHorizontalGlue());

        fileOverviewButton = new JButton();
        fileOverviewButton.setIcon(IconUtil.createThemed(
                "icons/file.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        fileOverviewButton.setToolTipText(
                I18nUtil.getMessage(MessageKeys.RESPONSE_FILE_BACK_TO_OVERVIEW));
        fileOverviewButton.getAccessibleContext().setAccessibleName(
                I18nUtil.getMessage(MessageKeys.RESPONSE_FILE_BACK_TO_OVERVIEW));
        fileOverviewButton.setFocusable(false);
        fileOverviewButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        fileOverviewButton.putClientProperty(
                FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        fileOverviewButton.setVisible(false);
        fileOverviewButton.addActionListener(event -> showFileOverview());
        toolBarPanel.add(fileOverviewButton);
        toolBarPanel.add(Box.createHorizontalStrut(1));

        // Nút tìm kiếm
        searchButton = new SearchButton();
        searchButton.addActionListener(e -> {
            responseBodyPane.requestFocusInWindow();
            searchableTextArea.showSearch();
        });
        toolBarPanel.add(searchButton);
        toolBarPanel.add(Box.createHorizontalStrut(1)); // khoảng thời gian

        // Key Nút chuyển đổi sắp xếp tuân theo hình ảnh trạng thái đã chọn của nút công cụ ngắt dòng
        autoSortJsonKeysButton = new JToggleButton();
        autoSortJsonKeysButton.setToolTipText(
                I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_AUTO_SORT_JSON_KEYS_TOOLTIP));
        autoSortJsonKeysButton.getAccessibleContext().setAccessibleName(
                I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_AUTO_SORT_JSON_KEYS));
        autoSortJsonKeysButton.setFocusable(false);
        autoSortJsonKeysButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        autoSortJsonKeysButton.putClientProperty(
                FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        autoSortJsonKeysButton.setSelected(false);
        updateAutoSortJsonKeysIcon(autoSortJsonKeysButton.isSelected());
        autoSortJsonKeysButton.addItemListener(e -> updateAutoSortJsonKeysIcon(autoSortJsonKeysButton.isSelected()));
        toolBarPanel.add(autoSortJsonKeysButton);
        toolBarPanel.add(Box.createHorizontalStrut(1));

        // Nút ngắt dòng
        wrapButton = new WrapToggleButton();
        toolBarPanel.add(wrapButton);
        toolBarPanel.add(Box.createHorizontalStrut(1));

        // Nút định dạng
        formatButton = new FormatButton();
        toolBarPanel.add(formatButton);
        toolBarPanel.add(Box.createHorizontalStrut(1));

        // Nút sao chép
        copyButton = new CopyButton();
        toolBarPanel.add(copyButton);
        toolBarPanel.add(Box.createHorizontalStrut(1));

        generateModelButton = new GenerateModelButton(I18nUtil.getMessage(MessageKeys.JSON_MODEL_GENERATOR_TITLE));
        generateModelButton.addActionListener(e -> JsonModelGeneratorDialog.showDialog(this, responseBodyPane.getText(), "Response"));
        toolBarPanel.add(generateModelButton);
        toolBarPanel.add(Box.createHorizontalStrut(1));

        // Nút tải xuống
        downloadButton = new DownloadButton();
        toolBarPanel.add(downloadButton);

        // Chỉ HTTP Nút lưu phản hồi chỉ được hiển thị theo yêu cầu
        if (enableSaveButton) {
            toolBarPanel.add(Box.createHorizontalStrut(1));
            saveResponseButton = new SaveResponseButton();
            toolBarPanel.add(saveResponseButton);
        }

        add(toolBarPanel, BorderLayout.NORTH);

        downloadButton.addActionListener(e -> downloadFromToolbar());
        formatButton.addActionListener(e -> formatContent());
        copyButton.addActionListener(e -> copyToClipboard());
        wrapButton.addActionListener(e -> toggleLineWrap());
        syntaxComboBox.addActionListener(e -> onSyntaxComboChanged());
        autoSortJsonKeysButton.addActionListener(e -> {
            if (currentResponse != null) {
                setBodyText(currentResponse);
            }
        });
        updateGenerateModelButtonState();
    }

    /**
     * Syntax type drop-down box change event handling
     * Cập nhật tính năng tô sáng cú pháp của trình soạn thảo dựa trên loại cú pháp do người dùng chọn
     */
    private void onSyntaxComboChanged() {
        int idx = syntaxComboBox.getSelectedIndex();
        SyntaxType syntaxType = SyntaxType.getByIndex(idx);

        String syntax;
        if (syntaxType == SyntaxType.AUTO_DETECT) {
            // Tự động phát hiện các loại cú pháp
            syntax = detectResponseSyntax(responseBodyPane.getText(), currentContentType());
        } else {
            // Sử dụng loại cú pháp do người dùng chọn
            syntax = syntaxType.getSyntaxStyle();
        }

        responseBodyPane.setSyntaxEditingStyle(syntax);
    }

    /**
     * Chuyển đổi trạng thái ngắt dòng tự động
     */
    private void toggleLineWrap() {
        boolean isWrapEnabled = wrapButton.isSelected();
        responseBodyPane.setLineWrap(isWrapEnabled);
    }


    /**
     * Tải xuống thanh công cụ (ở dạng xem trước văn bản): thông báo sẽ được nhắc sau khi lưu thành công; nếu phản hồi hiện tại có tổng quan về tệp thì vị trí lưu sẽ được hiển thị đồng thời.
     * Bạn sẽ không thấy đường dẫn đã hết hạn khi quay lại phần tổng quan.
     */
    private void downloadFromToolbar() {
        HttpResponse responseAtStart = currentResponse;
        startDownload(savedFile -> {
            ResponseBodyDownloader.notifySaved(savedFile);
            if (currentResponse == responseAtStart && FileResponseModel.shouldShow(currentResponse)) {
                fileResponsePanel.showSavedFile(savedFile);
            }
        });
    }

    /**
     * Tải xuống tổng quan về tệp: Sau khi lưu thành công, vị trí lưu sẽ được hiển thị trong phần tổng quan và lối vào sẽ được mở.
     */
    private void downloadFromFileOverview() {
        HttpResponse responseAtStart = currentResponse;
        startDownload(savedFile -> {
            // Khi bạn đã chuyển sang phản hồi khác trong khi lưu, kết quả không thể hiển thị trong phần tổng quan về phản hồi mới
            if (currentResponse == responseAtStart) {
                fileResponsePanel.showSavedFile(savedFile);
            } else {
                ResponseBodyDownloader.notifySaved(savedFile);
            }
        });
    }

    private void startDownload(Consumer<File> onSaved) {
        downloader.download(currentBodySource(), ResponseBodyDownloader.defaultFileName(currentResponse), onSaved);
    }

    private void onSavingChanged(boolean saving) {
        downloadButton.setEnabled(!saving && isEnabled());
        fileResponsePanel.setDownloading(saving);
    }

    /**
     * Ghi đồng bộ nội dung phản hồi hiện tại vào tệp đích. Xem quy tắc viết {@link ResponseBodyDownloader.Source}。
     */
    void writeResponseBody(File destFile) throws IOException {
        currentBodySource().writeTo(destFile);
    }

    private ResponseBodyDownloader.Source currentBodySource() {
        return ResponseBodyDownloader.Source.of(currentResponse, responseBodyPane.getText());
    }

    /**
     * Định dạng nội dung
     * theo Content-Type Vâng JSON hoặc XML Định dạng và làm đẹp
     */
    private void formatContent() {
        String text = responseBodyPane.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        String contentType = currentContentType();

        try {
            String formatted = null;
            if (contentType.contains("json") || JsonUtil.isTypeJSON(text)) {
                formatted = formatJson(text, autoSortJsonKeysButton.isSelected());
            } else if (contentType.contains("xml")) {
                formatted = XmlUtil.format(text);
            }

            if (formatted != null) {
                responseBodyPane.setText(formatted);
                responseBodyPane.setCaretPosition(0);
            }
        } catch (Exception ex) {
            NotificationCenter.showError("Format Error: " + ex.getMessage());
        }
    }

    /**
     * Sao chép nội dung vào clipboard
     * Sao chép nội dung nội dung phản hồi vào bảng nhớ tạm của hệ thống
     */
    private void copyToClipboard() {
        String text = responseBodyPane.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        copyTextToClipboard(text);
    }

    private void copyTextToClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            NotificationCenter.showInfo("Content copied to clipboard");
        } catch (Exception ex) {
            NotificationCenter.showError("Copy Error: " + ex.getMessage());
        }
    }

    private String currentContentType() {
        return ResponseFiles.contentType(currentResponse);
    }

    /**
     * Đặt nội dung nội dung phản hồi
     * <p>
     * Phương pháp này sẽ:
     * 1. Tự động phát hiện các loại cú pháp và thiết lập đánh dấu
     * 2. Hiển thị cảnh báo kích thước tệp nếu vượt quá ngưỡng
     * 3. Xác định xem có tự động định dạng hay không dựa trên cài đặt
     * </p>
     *
     * @param resp HTTP đối tượng phản hồi
     */
    public void setBodyText(HttpResponse resp) {
        if (resp == null) {
            clearResponseBody();
            return;
        }

        this.currentResponse = resp;
        this.fileRawPreviewActive = false;
        sizeWarningLabel.setToolTipText(null);
        fileResponsePanel.clear();
        String contentType = ResponseFiles.contentType(resp);

        String mainContentType = FileExtensionUtil.extractMainType(contentType);
        boolean isAudio = "audio".equalsIgnoreCase(mainContentType);
        boolean isVideo = "video".equalsIgnoreCase(mainContentType);

        // Java Sound Tích hợp tính năng phát lại âm thanh được hỗ trợ nguyên bản; mã hóa âm thanh và video khác được để lại cho trình phát hệ thống.
        if ((isAudio || isVideo) && resp.filePath != null && !resp.filePath.isEmpty()) {
            generateModelButton.setEnabled(false);
            imagePreviewLabel.setIcon(null);
            imagePreviewLabel.setText(null);
            sizeWarningLabel.setVisible(false);
            if (isAudio) {
                mediaResponsePanel.setAudioSource(resp.filePath, resp.fileName, resp.bodySize);
            } else {
                mediaResponsePanel.setVideoSource(resp.filePath, resp.fileName, resp.bodySize);
            }
            switchCard(CARD_MEDIA);
            return;
        }

        mediaResponsePanel.clear();

        // Xem trước hình ảnh
        if (resp.isImage && resp.filePath != null && !resp.filePath.isEmpty()) {
            generateModelButton.setEnabled(false);
            showImagePreview(resp.filePath, resp.bodySize);
            return;
        }

        if (FileResponseModel.shouldShow(resp)) {
            showFileResponse(resp);
            return;
        }

        // Chuyển về chế độ xem văn bản
        switchCard(CARD_TEXT);
        String text = resp.body;

        int textSize = text != null ? text.getBytes().length : 0;
        boolean isLargeResponse = textSize > LARGE_RESPONSE_THRESHOLD;

        // Hiển thị thông tin kích thước
        updateSizeWarning(textSize, isLargeResponse);

        // Tự động chọn loại đánh dấu
        String syntax = detectResponseSyntax(text, contentType);

        // Tự động khớp các tùy chọn hộp thả xuống
        int syntaxIndex = SyntaxType.getBySyntaxStyle(syntax).getIndex();
        syntaxComboBox.setSelectedIndex(syntaxIndex);

        // Đặt đánh dấu cú pháp và văn bản
        responseBodyPane.setSyntaxEditingStyle(syntax);
        responseBodyPane.setText(text);
        updateGenerateModelButtonState();

        // Xác định xem có tự động định dạng hay không dựa trên cài đặt và kích thước
        boolean shouldAutoFormat = SettingManager.isAutoFormatResponse();
        boolean shouldAutoSortKeys = autoSortJsonKeysButton.isSelected();
        if (shouldAutoFormat || shouldAutoSortKeys) {
            if (textSize <= LARGE_RESPONSE_THRESHOLD) {
                autoFormatIfPossible(text, contentType, shouldAutoFormat, shouldAutoSortKeys);
            } else {
                sizeWarningLabel.setText(sizeWarningLabel.getText() + "  "
                        + I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_SKIP_AUTO_PROCESSING_LARGE));
            }
        }

        responseBodyPane.setCaretPosition(0);
    }

    private void showFileResponse(HttpResponse response) {
        responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        // Tổng quan về tệp không hiển thị trình chỉnh sửa, văn bản nhị phân được trì hoãn“Xem bằng văn bản”để tránh tải lại EDT Một đoạn mã lớn bị cắt xén được hiển thị trên màn hình
        responseBodyPane.setText("");
        syntaxComboBox.setSelectedIndex(SyntaxType.PLAIN_TEXT.getIndex());
        sizeWarningLabel.setVisible(false);
        generateModelButton.setEnabled(false);
        fileResponsePanel.setResponse(FileResponseModel.from(response));
        switchCard(CARD_FILE);
    }

    private void showFileRawPreview() {
        if (currentResponse == null || currentResponse.rawBodyBytes == null) {
            return;
        }
        fileRawPreviewActive = true;
        responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        responseBodyPane.setText(currentResponse.body == null ? "" : currentResponse.body);
        responseBodyPane.setCaretPosition(0);
        sizeWarningLabel.setText("  "
                + I18nUtil.getMessage(MessageKeys.RESPONSE_FILE_TEXT_PREVIEW_BADGE));
        sizeWarningLabel.setToolTipText(
                I18nUtil.getMessage(MessageKeys.RESPONSE_FILE_TEXT_PREVIEW_NOTICE));
        sizeWarningLabel.setVisible(true);
        switchCard(CARD_TEXT);
        responseBodyPane.requestFocusInWindow();
    }

    private void showFileOverview() {
        if (currentResponse == null || !fileRawPreviewActive) {
            return;
        }
        fileRawPreviewActive = false;
        sizeWarningLabel.setVisible(false);
        sizeWarningLabel.setToolTipText(null);
        switchCard(CARD_FILE);
    }

    /**
     * Phản hồi nhị phân hiển thị trong chế độ xem văn bản là lời nhắc nhận, không phải nội dung phản hồi ban đầu.
     */
    private String detectResponseSyntax(String text, String contentType) {
        if (currentResponse != null
                && !currentResponse.isImage
                && FileExtensionUtil.isBinaryType(contentType)) {
            return SyntaxConstants.SYNTAX_STYLE_NONE;
        }
        return detectSyntax(text, contentType);
    }

    /**
     * Tự động phát hiện các loại cú pháp
     * <p>
     * Chiến lược phát hiện:
     * 1. Cơ sở ưu tiên Content-Type Đánh giá tiêu đề phản hồi
     * 2. Thứ hai, nó được đánh giá dựa trên đặc điểm nội dung (chẳng hạn như JSON của {} hoặc []，XML của < >）
     * </p>
     *
     * @param text        Nội dung văn bản
     * @param contentType Content-Type tiêu đề phản hồi
     * @return Syntax type constant (from  SyntaxConstants）
     */
    private String detectSyntax(String text, String contentType) {
        if (contentType != null) contentType = contentType.toLowerCase();
        if (JsonUtil.isTypeJSON(text)) {
            return SyntaxConstants.SYNTAX_STYLE_JSON;
        }
        if (contentType != null) {
            if (contentType.contains("json")) return SyntaxConstants.SYNTAX_STYLE_JSON;
            if (contentType.contains("xml")) return SyntaxConstants.SYNTAX_STYLE_XML;
            if (contentType.contains("html")) return SyntaxConstants.SYNTAX_STYLE_HTML;
            if (contentType.contains("javascript")) return SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
            if (contentType.contains("css")) return SyntaxConstants.SYNTAX_STYLE_CSS;
            if (contentType.contains("text")) return SyntaxConstants.SYNTAX_STYLE_NONE;
        }
        // Nhận dạng nội dung tự động
        if (text == null || text.isEmpty()) return SyntaxConstants.SYNTAX_STYLE_NONE;
        String t = text.trim();
        if ((t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"))) {
            return SyntaxConstants.SYNTAX_STYLE_JSON;
        }
        if (t.startsWith("<") && t.endsWith(">")) {
            if (t.toLowerCase().contains("<html")) return SyntaxConstants.SYNTAX_STYLE_HTML;
            if (t.toLowerCase().contains("<?xml")) return SyntaxConstants.SYNTAX_STYLE_XML;
        }
        return SyntaxConstants.SYNTAX_STYLE_NONE;
    }

    /**
     * Tự động định dạng nội dung (nếu có thể)
     * <p>
     * Cho ít hơn hoặc bằng 500KB Phản hồi được xử lý theo cài đặt của người dùng JSON/XML；key Sắp xếp chỉ hoạt động trên  JSON。
     * </p>
     *
     * @param text        Nội dung văn bản
     * @param contentType Content-Type tiêu đề phản hồi
     */
    private void autoFormatIfPossible(String text, String contentType,
                                      boolean autoFormat, boolean sortJsonKeys) {
        if (text == null || text.isEmpty()) {
            return;
        }

        try {
            boolean isJson = (contentType != null && contentType.toLowerCase().contains("json"))
                    || JsonUtil.isTypeJSON(text);
            if (isJson) {
                responseBodyPane.setText(formatJson(text, sortJsonKeys));
            } else if (autoFormat && contentType != null && contentType.toLowerCase().contains("xml")) {
                responseBodyPane.setText(XmlUtil.format(text));
            }
        } catch (Exception ex) {
            // Giữ nguyên nội dung phản hồi gốc khi định dạng hoặc sắp xếp không thành công.
        }
    }

    private String formatJson(String json, boolean sortKeys) throws Exception {
        if (!sortKeys) {
            return JsonUtil.toJsonPrettyStr(json);
        }
        try {
            Object parsed = cn.hutool.json.JSONUtil.parse(json);
            return JsonUtil.toJsonPrettyStr(sortJsonValue(parsed));
        } catch (Exception parseFailure) {
            // JsonUtil accepts commented JSON; keep that existing behavior if Hutool cannot parse it.
            return JsonUtil.toJsonPrettyStr(json);
        }
    }

    private void updateAutoSortJsonKeysIcon(boolean selected) {
        String iconPath = "icons/sort-keys.svg";
        if (selected) {
            autoSortJsonKeysButton.setIcon(IconUtil.createOnPrimary(
                    iconPath, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        } else {
            autoSortJsonKeysButton.setIcon(IconUtil.createThemed(
                    iconPath, IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        }
    }

    private Object sortJsonValue(Object value) {
        if (value instanceof cn.hutool.json.JSONObject object) {
            cn.hutool.json.JSONObject sorted = new cn.hutool.json.JSONObject(true);
            object.keySet().stream().sorted().forEach(key -> sorted.set(key, sortJsonValue(object.get(key))));
            return sorted;
        }
        if (value instanceof cn.hutool.json.JSONArray array) {
            cn.hutool.json.JSONArray sorted = new cn.hutool.json.JSONArray();
            for (Object item : array) {
                sorted.add(sortJsonValue(item));
            }
            return sorted;
        }
        return value;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        responseBodyPane.setEnabled(enabled);
        syntaxComboBox.setEnabled(enabled);
        searchButton.setEnabled(enabled);
        downloadButton.setEnabled(enabled && !downloader.isSaving());
        fileOverviewButton.setEnabled(enabled);
        searchableTextArea.setEnabled(enabled);

        if (formatButton != null) formatButton.setEnabled(enabled);
        if (autoSortJsonKeysButton != null) autoSortJsonKeysButton.setEnabled(enabled);
        if (copyButton != null) copyButton.setEnabled(enabled);
        updateGenerateModelButtonState();
        if (wrapButton != null) wrapButton.setEnabled(enabled);
        if (saveResponseButton != null) saveResponseButton.setEnabled(enabled);
        mediaResponsePanel.setEnabled(enabled);
        fileResponsePanel.setEnabled(enabled);
    }

    private void updateGenerateModelButtonState() {
        generateModelButton.setEnabled(isEnabled() && JsonUtil.isTypeJSON(responseBodyPane.getText()));
    }

    // ========== Các phương pháp trợ giúp ==========

    /**
     * Chuyển đổi các thẻ phản hồi được hiển thị ở khu vực trung tâm.
     */
    private void switchCard(String cardName) {
        boolean fileCard = CARD_FILE.equals(cardName);
        boolean fileRawPreview = CARD_TEXT.equals(cardName) && fileRawPreviewActive;

        syntaxComboBox.setVisible(CARD_TEXT.equals(cardName) && !fileRawPreview);
        fileOverviewButton.setVisible(fileRawPreview);
        // Thẻ tổng quan về tệp có nút tải xuống riêng và nút tải xuống trên thanh công cụ được ẩn để tránh trùng lặp
        downloadButton.setVisible(!fileCard);
        searchButton.setVisible(!fileCard);
        wrapButton.setVisible(!fileCard);
        copyButton.setVisible(!fileCard);
        autoSortJsonKeysButton.setVisible(!fileCard && !fileRawPreview);
        formatButton.setVisible(!fileCard && !fileRawPreview);
        generateModelButton.setVisible(!fileCard && !fileRawPreview);

        if (centerPanel.getLayout() instanceof CardLayout cl) {
            cl.show(centerPanel, cardName);
        }
    }

    /**
     * Load hình ảnh và chuyển sang thẻ xem trước hình ảnh
     * <p>
     * - GIF  → ImageIcon(URL)（Swing Hỗ trợ hoạt ảnh tích hợp, bảo tồn hoạt ảnh)
     * - WebP → TwelveMonkeys ImageIO Sau khi đăng ký tự động ImageIO.read() Có thể giải mã được
     * - SVG  → Hạ cấp: hiển thị bản gốc trực tiếp bằng trình soạn thảo văn bản XML nội dung
     * - khác → ImageIO.read() + ImageIcon
     * </p>
     *
     * @param filePath Đường dẫn tập tin tạm thời
     * @param bodySize Số byte trong hình ảnh
     */
    private void showImagePreview(String filePath, long bodySize) {
        File file = new File(filePath);
        String nameLower = file.getName().toLowerCase();

        try {
            // ── GIF GIF: Sử dụng ImageIcon(URL) Bảo tồn hình ảnh động ──────────────────────
            if (nameLower.endsWith(".gif")) {
                ImageIcon gifIcon = new ImageIcon(file.toURI().toURL());
                sizeWarningLabel.setText(String.format("  %d×%d  [%.2f KB]",
                        gifIcon.getIconWidth(), gifIcon.getIconHeight(), bodySize / 1024.0));
                sizeWarningLabel.setVisible(true);
                imagePreviewLabel.setIcon(gifIcon);
                imagePreviewLabel.setText(null);
                switchCard(CARD_IMAGE);
                return;
            }

            // ── WebP / PNG / JPG / BMP / Những người khác (bao gồm SVG hạ cấp)────────────────
            // TwelveMonkeys Đi qua SPI Đăng ký tự động WebP reader，ImageIO.read() Hỗ trợ trực tiếp
            // SVG Không thể được ImageIO giải mã(return null），sẽ vào showImageError Xử lý hạ cấp
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                // SVG hoặc các định dạng không thể giải mã khác: đọc nội dung văn bản tệp và hiển thị nó trong trình chỉnh sửa
                if (nameLower.endsWith(".svg")) {
                    String svgText = Files.readString(file.toPath());
                    responseBodyPane.setText(svgText);
                    syntaxComboBox.setSelectedIndex(SyntaxType.XML.getIndex());
                    responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                    responseBodyPane.setCaretPosition(0);
                    sizeWarningLabel.setText(String.format("  SVG  [%.2f KB]", bodySize / 1024.0));
                    sizeWarningLabel.setVisible(true);
                    switchCard(CARD_TEXT);
                } else {
                    showImageError("[Image cannot be decoded: " + file.getName() + "]", bodySize);
                }
                return;
            }
            sizeWarningLabel.setText(String.format("  %d×%d  [%.2f KB]",
                    img.getWidth(), img.getHeight(), bodySize / 1024.0));
            sizeWarningLabel.setVisible(true);
            imagePreviewLabel.setIcon(new ImageIcon(img));
            imagePreviewLabel.setText(null);
            switchCard(CARD_IMAGE);

        } catch (Exception e) {
            showImageError("[Failed to load image: " + e.getMessage() + "]", bodySize);
        }
    }

    /** Khi hình ảnh không tải được sẽ rơi trở lại thẻ văn bản hiển thị thông báo lỗi */
    private void showImageError(String message, long bodySize) {
        responseBodyPane.setText(message);
        syntaxComboBox.setSelectedIndex(SyntaxType.PLAIN_TEXT.getIndex());
        responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        sizeWarningLabel.setText(String.format("  [%.2f KB]", bodySize / 1024.0));
        sizeWarningLabel.setVisible(true);
        switchCard(CARD_TEXT);
    }

    /**
     * Xóa nội dung nội dung phản hồi
     */
    private void clearResponseBody() {
        responseBodyPane.setText("");
        updateGenerateModelButtonState();
        currentResponse = null;
        fileRawPreviewActive = false;
        responseBodyPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        responseBodyPane.setCaretPosition(0);
        syntaxComboBox.setSelectedIndex(SyntaxType.AUTO_DETECT.getIndex());
        sizeWarningLabel.setVisible(false);
        sizeWarningLabel.setToolTipText(null);
        imagePreviewLabel.setIcon(null);
        imagePreviewLabel.setText(null);
        mediaResponsePanel.clear();
        fileResponsePanel.clear();
        switchCard(CARD_TEXT);
    }

    /**
     * Nhãn cảnh báo kích thước được cập nhật
     */
    private void updateSizeWarning(int textSize, boolean isLargeResponse) {
        if (isLargeResponse) {
            sizeWarningLabel.setText(String.format("  [%.2f MB]", textSize / 1024.0 / 1024.0));
            sizeWarningLabel.setVisible(true);
        } else {
            sizeWarningLabel.setVisible(false);
        }
    }

}
