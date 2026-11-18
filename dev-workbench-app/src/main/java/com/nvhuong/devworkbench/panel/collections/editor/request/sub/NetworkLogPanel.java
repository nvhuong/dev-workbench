package com.nvhuong.devworkbench.panel.collections.editor.request.sub;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.http.runtime.model.HttpResponse;
import com.nvhuong.devworkbench.http.runtime.model.PreparedRequest;
import com.nvhuong.devworkbench.http.runtime.observation.NetworkLogEvent;
import com.nvhuong.devworkbench.http.runtime.observation.NetworkLogEventStage;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.service.curl.CurlParser;
import com.nvhuong.devworkbench.service.render.HttpHtmlRenderer;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.util.IconUtil;
import com.nvhuong.devworkbench.util.I18nUtil;
import com.nvhuong.devworkbench.util.MessageKeys;
import com.nvhuong.devworkbench.common.component.notification.NotificationCenter;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Bảng nhật ký mạng, bao gồm nhật ký mạng, chi tiết yêu cầu và chi tiết phản hồiTab
 */
public class NetworkLogPanel extends JPanel {
    private final NetworkLogTextPane logArea;
    private final StyledDocument doc;
    private final JTabbedPane tabbedPane;
    private final JTextPane requestDetailsPane;
    private final JTextPane responseDetailsPane;
    private final JButton copyCurrentTabButton;
    private final JButton copyActualCurlButton;
    private PreparedRequest currentRequest;
    private boolean hasResponseDetails;

    // Cấu hình tối ưu hóa hiệu suất - Giảm giới hạn để tránh bị trễ
    private static final int MAX_LINE_LENGTH = 500; // Độ dài tối đa của một dòng
    private static final int MAX_LINES_PER_MESSAGE = 30; // Số dòng tối đa trong một tin nhắn
    private static final int MAX_TOTAL_LENGTH = 50000; // Tổng giới hạn độ dài nhật ký (số ký tự)

    public NetworkLogPanel() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        // Đặt lề
        setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));

        copyCurrentTabButton = createCopyCurrentTabButton();
        copyActualCurlButton = createCopyActualCurlButton();
        add(createToolbar(), BorderLayout.NORTH);

        // Tạo TabbedPane
        tabbedPane = new JTabbedPane(SwingConstants.LEFT);
        ToolWindowSurfaceStyle.applyTabbedPaneCard(tabbedPane);
        tabbedPane.addChangeListener(e -> updateCopyCurrentTabButtonState());

        // 1. Network Log Tab
        logArea = new NetworkLogTextPane();
        logArea.setEditable(false);
        logArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        ToolWindowSurfaceStyle.applyTextComponentCard(logArea);
        doc = logArea.getStyledDocument();
        JScrollPane logScroll = new JScrollPane(logArea);
        ToolWindowSurfaceStyle.applyScrollPaneCard(logScroll);
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.NETWORK_LOG_TAB_LOG), logScroll);

        // 2. Request Details Tab
        requestDetailsPane = createDetailPane();
        JScrollPane requestDetailsScroll = new JScrollPane(requestDetailsPane);
        ToolWindowSurfaceStyle.applyScrollPaneCard(requestDetailsScroll);
        requestDetailsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        requestDetailsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.NETWORK_LOG_TAB_REQUEST), requestDetailsScroll);

        // 3. Response Details Tab
        responseDetailsPane = createDetailPane();
        JScrollPane responseDetailsScroll = new JScrollPane(responseDetailsPane);
        ToolWindowSurfaceStyle.applyScrollPaneCard(responseDetailsScroll);
        responseDetailsScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        responseDetailsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tabbedPane.addTab(I18nUtil.getMessage(MessageKeys.NETWORK_LOG_TAB_RESPONSE), responseDetailsScroll);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        ToolWindowSurfaceStyle.applySectionHeader(toolbar, 0, 0, 4, 0);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(copyCurrentTabButton);
        toolbar.add(Box.createHorizontalStrut(1));
        toolbar.add(copyActualCurlButton);
        return toolbar;
    }

    private JButton createCopyCurrentTabButton() {
        String label = I18nUtil.getMessage(MessageKeys.BUTTON_COPY);
        JButton button = new JButton(IconUtil.createThemed("icons/copy.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        configureToolbarButton(button, label,
                I18nUtil.getMessage(MessageKeys.NETWORK_LOG_COPY_CURRENT_TAB_TOOLTIP));
        button.setEnabled(false);
        button.addActionListener(e -> copyCurrentTabToClipboard());
        return button;
    }

    private JButton createCopyActualCurlButton() {
        String label = I18nUtil.getMessage(MessageKeys.NETWORK_LOG_COPY_ACTUAL_CURL);
        JButton button = new JButton(IconUtil.create("icons/curl.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        configureToolbarButton(button, label,
                I18nUtil.getMessage(MessageKeys.NETWORK_LOG_COPY_ACTUAL_CURL_TOOLTIP));
        button.setEnabled(false);
        button.addActionListener(e -> copyActualCurlToClipboard());
        return button;
    }

    private void configureToolbarButton(JButton button, String label, String tooltip) {
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(label);
    }

    /**
     * Tạo bảng chi tiết
     */
    private JTextPane createDetailPane() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        ToolWindowSurfaceStyle.applyTextComponentCard(pane);
        return pane;
    }

    /**
     * Thêm nhật ký
     *
     * @param stage Bảng liệt kê giai đoạn nhật ký
     * @param msg   Nội dung tin nhắn
     */
    public void appendLog(NetworkLogStage stage, String msg) {
        appendLog(stage, msg, null);
    }

    /**
     * kết xuất HTTP Ghi lại các sự kiện được xuất bản bởi lớp thực thi.
     * <p>
     * Lớp thực thi chỉ cung cấp các giai đoạn và nội dung, màu sắc/Các chi tiết hiển thị như biểu tượng vẫn được giữ nguyên Swing Bảng điều khiển cục bộ NetworkLogStage quyết định.
     */
    public void appendLog(NetworkLogEvent event) {
        if (event == null) {
            return;
        }
        NetworkLogEventStage eventStage = event.stage();
        appendLog(NetworkLogStage.fromEventStage(eventStage),
                NetworkLogMessageFormatter.format(eventStage, event.message()),
                event.elapsedMs(), event.durationMs(), event.message());
    }

    /**
     * Đã thêm nhật ký (hỗ trợ bù thời gian)
     *
     * @param stage     Bảng liệt kê giai đoạn nhật ký
     * @param msg       Nội dung tin nhắn
     * @param elapsedMs Thời gian đã trôi qua (mili giây), có thể null
     */
    public void appendLog(NetworkLogStage stage, String msg, Long elapsedMs) {
        appendLog(stage, msg, elapsedMs, null);
    }

    public void appendLog(NetworkLogStage stage, String msg, Long elapsedMs, Long durationMs) {
        appendLog(stage, msg, elapsedMs, durationMs, msg);
    }

    private void appendLog(NetworkLogStage stage, String msg, Long elapsedMs, Long durationMs, String rawMessage) {
        SwingUtilities.invokeLater(() -> {
            try {
                NetworkLogStage resolvedStage = NetworkLogPresentation.resolveStage(stage, rawMessage);
                // Kiểm tra và giới hạn tổng chiều dài nhật ký để tránh tràn bộ nhớ
                if (doc.getLength() > MAX_TOTAL_LENGTH) {
                    // trước khi xóa1/3nội dung để giữ cho nhật ký có thể đọc được
                    int removeLength = Math.min(doc.getLength(),
                            doc.getParagraphElement(MAX_TOTAL_LENGTH / 3).getEndOffset());
                    AttributeSet retainedParagraphStyle = doc.getParagraphElement(removeLength)
                            .getAttributes().copyAttributes();
                    doc.remove(0, removeLength);
                    // Removing from offset zero merges paragraph attributes; retain the surviving row's icon.
                    doc.setParagraphAttributes(0, 0, retainedParagraphStyle, true);
                }

                // Tối ưu hóa việc cắt ngắn nội dung: Nếu nội dung quá dài, hãy cắt bớt nội dung đó
                String content = resolvedStage == NetworkLogStage.CANCELED && stage != resolvedStage
                        ? I18nUtil.getMessage(MessageKeys.NETWORK_LOG_MESSAGE_CALL_CANCELED)
                        : msg != null ? msg : "";
                if (content.length() > MAX_LINE_LENGTH * MAX_LINES_PER_MESSAGE) {
                    int originalLength = content.length();
                    content = content.substring(0, MAX_LINE_LENGTH * MAX_LINES_PER_MESSAGE)
                            + "\n" + I18nUtil.getMessage(MessageKeys.NETWORK_LOG_CONTENT_TRUNCATED, originalLength);
                }

                NetworkLogPresentation.Style presentation = NetworkLogPresentation.resolveStyle(resolvedStage, rawMessage);
                int fontSize = FontsUtil.getDefaultFont(Font.PLAIN).getSize();
                SimpleAttributeSet stageStyle = createTextAttributes(presentation.color(), presentation.bold(), fontSize);
                SimpleAttributeSet timingStyle = createTextAttributes(getDefaultTextColor(), false, fontSize);

                int entryStart = doc.getLength();
                doc.insertString(entryStart, "[" + resolvedStage.getDisplayName() + "]", stageStyle);

                StringBuilder timingText = new StringBuilder();
                if (elapsedMs != null) {
                    timingText.append(" +").append(elapsedMs).append("ms");
                }
                if (durationMs != null && durationMs > 0) {
                    timingText.append(elapsedMs == null ? " " : ", ")
                            .append(I18nUtil.getMessage(MessageKeys.NETWORK_LOG_PHASE_DURATION, durationMs));
                }
                timingText.append(" ");
                doc.insertString(doc.getLength(), timingText.toString(), timingStyle);
                appendContent(resolvedStage, content, rawMessage, fontSize);
                logArea.styleEntry(entryStart, presentation, fontSize);

                // Tự động cuộn xuống phía dưới
                logArea.setCaretPosition(doc.getLength());
                updateCopyCurrentTabButtonState();
            } catch (BadLocationException e) {
                // ignore
            }
        });
    }

    /**
     * Lấy màu văn bản mặc định để điều chỉnh chủ đề
     */
    private Color getDefaultTextColor() {
        return NetworkLogStage.DEFAULT.getColor();
    }

    private SimpleAttributeSet createTextAttributes(Color color, boolean bold, int fontSize) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setForeground(attributes, color);
        StyleConstants.setBold(attributes, bold);
        StyleConstants.setFontSize(attributes, fontSize);
        return attributes;
    }

    private void appendContent(NetworkLogStage stage, String content, String rawMessage, int fontSize)
            throws BadLocationException {
        String[] lines = content.split("\\n");
        String[] rawLines = rawMessage == null ? new String[0] : rawMessage.split("\\n");
        Integer status = NetworkLogPresentation.responseStatus(stage, rawMessage);
        boolean statusRendered = false;
        SimpleAttributeSet bodyStyle = createTextAttributes(getDefaultTextColor(), false, fontSize);
        int lineCount = Math.min(lines.length, MAX_LINES_PER_MESSAGE);
        for (int i = 0; i < lineCount; i++) {
            String line = lines[i];
            if (line.length() > MAX_LINE_LENGTH) {
                line = line.substring(0, MAX_LINE_LENGTH) + "...";
            }
            if (i > 0) {
                doc.insertString(doc.getLength(), "\n", bodyStyle);
            }
            SimpleAttributeSet lineStyle = bodyStyle;
            if (status != null && !statusRendered && !line.isBlank()) {
                lineStyle = createTextAttributes(NetworkLogPresentation.statusColor(status), true, fontSize);
                statusRendered = true;
            } else if (i < rawLines.length && NetworkLogPresentation.isCertificateWarning(stage, rawLines[i])) {
                line = line.replaceFirst("^⚠️?\\s*", "");
                lineStyle = createTextAttributes(NetworkLogPresentation.warningColor(), true, fontSize);
            } else if (i == 0 && stage.isError()) {
                lineStyle = createTextAttributes(NetworkLogPresentation.resolveStyle(stage, rawMessage).color(), true, fontSize);
            }
            doc.insertString(doc.getLength(), line, lineStyle);
        }
        if (lines.length > MAX_LINES_PER_MESSAGE) {
            doc.insertString(doc.getLength(), "\n" + I18nUtil.getMessage(MessageKeys.NETWORK_LOG_LINES_OMITTED,
                    lines.length - MAX_LINES_PER_MESSAGE), bodyStyle);
        }
        doc.insertString(doc.getLength(), "\n", bodyStyle);
    }


    public void clearLog() {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.remove(0, doc.getLength());
                logArea.clearDecoration();
                updateCopyCurrentTabButtonState();
            } catch (BadLocationException e) {
                // ignore
            }
        });
    }

    /**
     * Cập nhật chi tiết yêu cầu
     */
    public void setRequestDetails(PreparedRequest request) {
        if (requestDetailsPane == null) return;
        currentRequest = request;
        updateCopyActualCurlButtonState();
        if (request == null) {
            requestDetailsPane.setText(I18nUtil.getMessage(MessageKeys.HISTORY_EMPTY_BODY));
            updateCopyCurrentTabButtonState();
            return;
        }
        String html = HttpHtmlRenderer.renderRequest(request);
        requestDetailsPane.setText(html);
        requestDetailsPane.setCaretPosition(0);
        updateCopyCurrentTabButtonState();
    }

    /**
     * Cập nhật chi tiết phản hồi
     */
    public void setResponseDetails(HttpResponse response) {
        if (responseDetailsPane == null) return;
        hasResponseDetails = response != null;
        if (response == null) {
            responseDetailsPane.setText(I18nUtil.getMessage(MessageKeys.HISTORY_EMPTY_BODY));
            updateCopyCurrentTabButtonState();
            return;
        }
        String html = HttpHtmlRenderer.renderResponse(response);
        responseDetailsPane.setText(html);
        responseDetailsPane.setCaretPosition(0);
        updateCopyCurrentTabButtonState();
    }

    /**
     * Xóa tất cả các bảng chi tiết
     */
    public void clearAllDetails() {
        currentRequest = null;
        hasResponseDetails = false;
        updateCopyActualCurlButtonState();
        if (requestDetailsPane != null) {
            requestDetailsPane.setText("");
        }
        if (responseDetailsPane != null) {
            responseDetailsPane.setText("");
        }
        updateCopyCurrentTabButtonState();
    }

    private void updateCopyActualCurlButtonState() {
        if (copyActualCurlButton != null) {
            copyActualCurlButton.setEnabled(CurlParser.canExportActualCurl(currentRequest));
        }
    }

    private void updateCopyCurrentTabButtonState() {
        if (copyCurrentTabButton == null || tabbedPane == null) {
            return;
        }
        boolean hasContent = switch (tabbedPane.getSelectedIndex()) {
            case 0 -> doc != null && doc.getLength() > 0;
            case 1 -> currentRequest != null;
            case 2 -> hasResponseDetails;
            default -> false;
        };
        copyCurrentTabButton.setEnabled(hasContent);
    }

    private void copyCurrentTabToClipboard() {
        try {
            String content = getSelectedTabText();
            if (content.isBlank()) {
                return;
            }
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(content), null);
            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.NETWORK_LOG_COPY_CURRENT_TAB_SUCCESS));
        } catch (Exception ex) {
            NotificationCenter.showError(I18nUtil.getMessage(
                    MessageKeys.NETWORK_LOG_COPY_CURRENT_TAB_FAIL, ex.getMessage()));
        }
    }

    private String getSelectedTabText() throws BadLocationException {
        JTextPane selectedPane = switch (tabbedPane.getSelectedIndex()) {
            case 0 -> logArea;
            case 1 -> requestDetailsPane;
            case 2 -> responseDetailsPane;
            default -> null;
        };
        if (selectedPane == null) {
            return "";
        }
        return selectedPane.getDocument().getText(0, selectedPane.getDocument().getLength()).strip();
    }

    private void copyActualCurlToClipboard() {
        try {
            String curl = CurlParser.toActualCurl(currentRequest);
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(curl), null);
            NotificationCenter.showSuccess(I18nUtil.getMessage(MessageKeys.NETWORK_LOG_COPY_ACTUAL_CURL_SUCCESS));
        } catch (Exception ex) {
            NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.NETWORK_LOG_COPY_ACTUAL_CURL_FAIL, ex.getMessage()));
        }
    }
}
