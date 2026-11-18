package com.nvhuong.devworkbench.common.component;

import com.formdev.flatlaf.extras.components.FlatTextField;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.variable.VariableInfo;
import com.nvhuong.devworkbench.variable.VariableSegment;
import com.nvhuong.devworkbench.service.variable.VariableResolver;
import com.nvhuong.devworkbench.variable.VariableType;
import com.nvhuong.devworkbench.util.FontsUtil;
import com.nvhuong.devworkbench.variable.VariableParser;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 1.Hỗ trợ DevWorkbench Làm nổi bật biến kiểu và hộp nhập văn bản gợi ý nổi
 * Dạng biến đổi:{{varName}}
 * Các biến đã xác định được hiển thị với nền màu xanh lam và các biến không xác định được hiển thị với nền màu đỏ
 * Hiển thị gợi ý về giá trị của biến khi di chuột qua biến
 * 2.Hỗ trợwinvàmacCác phím tắt hoàn tác và làm lại bên dưới
 * win：Ctrl+Z Rút tiền,Ctrl+Y làm lại
 * mac：Cmd+Z Rút tiền,Cmd+Shift+Z làm lại
 * 3.Hỗ trợ chức năng tự động hoàn thành biến
 * Nhập {{ Một danh sách các biến, bao gồm các biến môi trường và các hàm tích hợp, sẽ tự động bật lên
 */
@Slf4j
public class DevWorkbenchTextField extends FlatTextField {
    private final UndoManager undoManager = new UndoManager();

    // Tự động hoàn thành có liên quan
    private JWindow autocompleteWindow;
    private JList<VariableInfo> autocompleteList;
    private DefaultListModel<VariableInfo> autocompleteModel;
    private int autocompleteStartPos = -1;

    public DevWorkbenchTextField(int columns) {
        super();
        setColumns(columns);
        // Kích hoạt ToolTip Được hỗ trợ, phải được đặt (ngay cả khi nội dung trống)
        setToolTipText("");
        initUndoRedo();
        initAutocomplete();
    }

    public DevWorkbenchTextField(String text, int columns) {
        super();
        setText(text);
        setColumns(columns);
        // Kích hoạt ToolTip Được hỗ trợ, phải được đặt (ngay cả khi nội dung trống)
        setToolTipText("");
        initUndoRedo();
        initAutocomplete();
    }

    public DevWorkbenchTextField(String text, int columns, String placeholderText) {
        super();
        setText(text);
        setColumns(columns);
        setPlaceholderText(placeholderText);
        // Kích hoạt ToolTip Được hỗ trợ, phải được đặt (ngay cả khi nội dung trống)
        setToolTipText("");
        initUndoRedo();
        initAutocomplete();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Đầu tiên vẽ văn bản, con trỏ và vùng chọn
        super.paintComponent(g);

        String value = getText();
        List<VariableSegment> segments = VariableParser.getVariableSegments(value);
        if (segments.isEmpty()) return;

        try {
            // Thêm vàonullKiểm tra và ngăn chặnmodelToView2Dtrở lạinull
            var view = modelToView2D(0);
            if (view == null) {
                return; // nếuviewvìnull，Trả về trực tiếp mà không cần tô sáng biến
            }

            Rectangle startRect = view.getBounds();
            FontMetrics fm = getFontMetrics(getFont());
            int x = startRect.x;
            int baseY = startRect.y;
            int h = fm.getHeight();
            int last = 0;

            for (VariableSegment seg : segments) {
                // Tính chiều rộng văn bản trước biến
                if (seg.start > last) {
                    String before = value.substring(last, seg.start);
                    int w = fm.stringWidth(before);
                    x += w;
                }
                // Xác định trạng thái biến: biến ngữ cảnh thực thi, biến môi trường hoặc hàm dựng sẵn
                boolean isDefined = VariableResolver.isVariableDefined(seg.name);
                Color bgColor = isDefined
                        ? ModernColors.getDefinedVariableBadgeBackground()
                        : ModernColors.getUndefinedVariableBadgeBackground();
                Color borderColor = isDefined
                        ? ModernColors.getDefinedVariableBadgeBorder()
                        : ModernColors.getUndefinedVariableBadgeBorder();
                String varText = value.substring(seg.start, seg.end);
                int varWidth = fm.stringWidth(varText);
                // Chỉ có nền và đường viền bán trong suốt của biến được vẽ chứ không phải văn bản
                g.setColor(bgColor);
                g.fillRoundRect(x, baseY, varWidth, h, 8, 8);
                g.setColor(borderColor);
                g.drawRoundRect(x, baseY, varWidth, h, 8, 8);
                x += varWidth;
                last = seg.end;
            }
        } catch (Exception e) {
            log.error("paintComponent", e);
        }
    }

    /**
     * Đã rút lại quá trình khởi tạo/Làm lại chức năng
     */
    private void initUndoRedo() {
        getDocument().addUndoableEditListener(undoManager);

        // Undo
        getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        getInputMap().put(KeyStroke.getKeyStroke("meta Z"), "Undo"); // macOS Cmd+Z
        getActionMap().put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canUndo()) undoManager.undo();
                } catch (CannotUndoException ignored) {
                }
            }
        });

        // Redo
        getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
        getInputMap().put(KeyStroke.getKeyStroke("meta shift Z"), "Redo"); // macOS Cmd+Shift+Z
        getActionMap().put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canRedo()) undoManager.redo();
                } catch (CannotRedoException ignored) {
                }
            }
        });
    }

    /**
     * Khởi tạo chức năng tự động hoàn thành
     */
    private void initAutocomplete() {
        // Tạo một cửa sổ bật lên (sử dụng JWindowthay thếJPopupMenuđể kiểm soát tốt hơn)
        autocompleteWindow = new JWindow(SwingUtilities.getWindowAncestor(this));
        autocompleteWindow.setFocusableWindowState(false);

        autocompleteModel = new DefaultListModel<>();
        autocompleteList = new JList<>(autocompleteModel);
        autocompleteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        autocompleteList.setVisibleRowCount(10);
        autocompleteList.setBackground(ModernColors.getCardBackgroundColor());
        autocompleteList.setSelectionBackground(ModernColors.getSelectionBackgroundColor());
        autocompleteList.setSelectionForeground(ModernColors.getTextPrimary());
        autocompleteList.setFont(getFont());
        // Đã sửa lỗi chiều rộng danh sách để ngăn cuộn ngang do nội dung quá nhiều
        autocompleteList.setFixedCellWidth(384); // 400 - Đường viền và phần đệm

        // Trình kết xuất danh sách tùy chỉnh
        autocompleteList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JPanel panel = new JPanel(new BorderLayout(8, 0));
                panel.setOpaque(true);
                panel.setBorder(new EmptyBorder(4, 8, 4, 8));
                // Đặt kích thước cố định để tránh cuộn ngang
                panel.setPreferredSize(new Dimension(384, 32));
                panel.setMaximumSize(new Dimension(384, 32));

                if (isSelected) {
                    panel.setBackground(ModernColors.getSelectionBackgroundColor());
                } else {
                    panel.setBackground(ModernColors.getCardBackgroundColor());
                }

                if (value instanceof VariableInfo varInfo) {
                    String varName = varInfo.getName();
                    String varValue = varInfo.getValue();
                    VariableType varType = varInfo.getType();
                    Color labelColor = varType.getColor();
                    String symbol = varType.getIconSymbol();

                    // Thay vào đó hãy sử dụng các chấm màu Emoji（hiệu suất đa nền tảng tốt hơn)
                    JPanel iconPanel = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            Graphics2D g2d = (Graphics2D) g.create();
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            FontsUtil.applyTextRenderingHints(g2d);

                            // Nhận thông tin số liệu phông chữ để căn chỉnh theo chiều dọc
                            int panelHeight = getHeight();

                            // Tính tọa độ tâm thẳng đứng của đường tròn
                            int circleSize = 12;
                            int circleY = (panelHeight - circleSize) / 2;

                            // Vẽ biểu tượng vòng tròn
                            g2d.setColor(varType.getColor());
                            g2d.fillOval(2, circleY, circleSize, circleSize);

                            g2d.setColor(ModernColors.getTextInverse());
                            g2d.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2)); // Nhỏ hơn kích thước phông chữ tiêu chuẩn2Không
                            FontMetrics symbolFm = g2d.getFontMetrics();
                            int symbolWidth = symbolFm.stringWidth(symbol);
                            int symbolAscent = symbolFm.getAscent();
                            int symbolDescent = symbolFm.getDescent();
                            int symbolHeight = symbolAscent + symbolDescent;

                            // Biểu tượng được căn giữa theo chiều dọc trong dấu chấm
                            int symbolX = 2 + (circleSize - symbolWidth) / 2;
                            int symbolY = circleY + (circleSize - symbolHeight) / 2 + symbolAscent;
                            g2d.drawString(symbol, symbolX, symbolY);

                            g2d.dispose();
                        }

                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(16, 24);
                        }
                    };
                    iconPanel.setOpaque(false);
                    panel.add(iconPanel, BorderLayout.WEST);

                    // Khu vực giữa: Được bố trí bằng các tấm có chiều rộng cố địnhnamevàvalue，Hãy để mỗi người chiếm một nửa không gian
                    JPanel contentPanel = new JPanel(new GridLayout(1, 2, 8, 0));
                    contentPanel.setOpaque(false);

                    // Bên trái: tên biến (nửa khoảng trắng)
                    JPanel namePanel = new JPanel(new BorderLayout());
                    namePanel.setOpaque(false);

                    String displayName = varName;
                    String fullName = varName;

                    // Độ dài tối đa của tên biến (khoảng một nửa chiều rộng)
                    int maxNameLength = 25;
                    if (varName.length() > maxNameLength) {
                        displayName = varName.substring(0, maxNameLength - 3) + "...";
                    }

                    JLabel nameLabel = new JLabel(displayName);
                    nameLabel.setFont(getFont().deriveFont(Font.BOLD));
                    nameLabel.setForeground(labelColor);

                    // Thêm chú giải công cụ cho tên biến
                    if (!displayName.equals(fullName)) {
                        nameLabel.setToolTipText(fullName);
                    }

                    namePanel.add(nameLabel, BorderLayout.WEST);
                    contentPanel.add(namePanel);

                    // Bên phải: giá trị biến hoặc mô tả (nửa khoảng trắng)
                    JPanel valuePanel = new JPanel(new BorderLayout());
                    valuePanel.setOpaque(false);

                    if (varValue != null && !varValue.isEmpty()) {
                        String displayValue = varValue;
                        String fullValue = varValue;

                        // Độ dài tối đa của giá trị biến (khoảng một nửa chiều rộng)
                        int maxValueLength = 25;
                        if (varValue.length() > maxValueLength) {
                            displayValue = varValue.substring(0, maxValueLength - 3) + "...";
                        }

                        JLabel valueLabel = new JLabel(displayValue);
                        valueLabel.setFont(getFont().deriveFont(Font.PLAIN, getFont().getSize() - 1));
                        valueLabel.setForeground(ModernColors.getTextSecondary());

                        // Thêm chú giải công cụ cho giá trị (hiển thị nội dung đầy đủ)
                        if (!displayValue.equals(fullValue) || fullValue.length() > 20) {
                            // Chú giải công cụ định dạng có hỗ trợ ngắt dòng
                            String tooltipText = formatTooltipText(fullValue, 60);
                            valueLabel.setToolTipText("<html>" + tooltipText + "</html>");
                        }

                        valuePanel.add(valueLabel, BorderLayout.WEST);
                    }
                    contentPanel.add(valuePanel);

                    panel.add(contentPanel, BorderLayout.CENTER);

                    // Thêm chú giải công cụ vào toàn bộ bảng điều khiển
                    StringBuilder tooltipBuilder = new StringBuilder("<html>");
                    tooltipBuilder.append("<b>").append(escapeHtml(varName)).append("</b>");
                    tooltipBuilder.append(" <span style='color:")
                            .append(ModernColors.toHtmlColor(ModernColors.getTextHint()))
                            .append("'>(").append(varType.getDisplayName()).append(")</span>");
                    if (varValue != null && !varValue.isEmpty()) {
                        tooltipBuilder.append("<br/>").append(escapeHtml(varValue));
                    }
                    tooltipBuilder.append("</html>");
                    panel.setToolTipText(tooltipBuilder.toString());
                }

                return panel;
            }
        });

        JScrollPane scrollPane = new JScrollPane(autocompleteList);
        // Vô hiệu hóa thanh cuộn ngang, chỉ để lại thanh cuộn dọc
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getDividerBorderColor(), 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        autocompleteWindow.add(scrollPane);

        // Theo dõi những thay đổi của tài liệu
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> checkForAutocomplete());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> checkForAutocomplete());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> checkForAutocomplete());
            }
        });

        // Liệt kê sự kiện click chuột
        autocompleteList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 || e.getClickCount() == 1) {
                    insertSelectedVariable();
                }
            }
        });

        // Sự kiện bàn phím hộp văn bản
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (autocompleteWindow.isVisible()) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            int currentIndex = autocompleteList.getSelectedIndex();
                            int nextIndex = currentIndex + 1;
                            // Điều hướng vòng tròn: Quay lại phần đầu tiên sau khi đến phần cuối cùng
                            if (nextIndex >= autocompleteModel.getSize()) {
                                nextIndex = 0;
                            }
                            autocompleteList.setSelectedIndex(nextIndex);
                            autocompleteList.ensureIndexIsVisible(nextIndex);
                            e.consume();
                            break;
                        case KeyEvent.VK_UP:
                            int currentUpIndex = autocompleteList.getSelectedIndex();
                            int prevIndex = currentUpIndex - 1;
                            // Điều hướng vòng tròn: chuyển đến tùy chọn cuối cùng trước tùy chọn đầu tiên
                            if (prevIndex < 0) {
                                prevIndex = autocompleteModel.getSize() - 1;
                            }
                            autocompleteList.setSelectedIndex(prevIndex);
                            autocompleteList.ensureIndexIsVisible(prevIndex);
                            e.consume();
                            break;
                        case KeyEvent.VK_ENTER:
                        case KeyEvent.VK_TAB:
                            insertSelectedVariable();
                            e.consume();
                            break;
                        case KeyEvent.VK_ESCAPE:
                            hideAutocomplete();
                            e.consume();
                            break;
                        default:
                            break;
                    }
                }
            }
        });

        // Ẩn khi mất tiêu điểm
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                // Trì hoãn việc ẩn, cho phép nhấp vào các mục trong danh sách
                SwingUtilities.invokeLater(() -> {
                    if (!autocompleteList.hasFocus()) {
                        hideAutocomplete();
                    }
                });
            }
        });
    }

    /**
     * Định dạng văn bản chú giải công cụ với tính năng ngắt dòng tự động
     */
    private String formatTooltipText(String text, int maxLineLength) {
        if (text == null || text.length() <= maxLineLength) {
            return escapeHtml(text);
        }

        StringBuilder formatted = new StringBuilder();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLineLength, text.length());
            // Hãy thử ngắt dòng ở khoảng trắng
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start && lastSpace - start > maxLineLength / 2) {
                    end = lastSpace;
                }
            }
            formatted.append(escapeHtml(text.substring(start, end)));
            if (end < text.length()) {
                formatted.append("<br/>");
            }
            start = end + 1;
        }
        return formatted.toString();
    }

    /**
     * HTMLThoát để ngăn các ký tự đặc biệt phá vỡ chú giải công cụ
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * Kiểm tra xem có cần hiển thị tính năng tự động hoàn thành hay không
     */
    private void checkForAutocomplete() {
        String text = getText();
        int caretPos = getCaretPosition();

        if (text == null || caretPos < 2) {
            hideAutocomplete();
            return;
        }

        // Tìm cái gần con trỏ nhất {{
        int openBracePos = text.lastIndexOf("{{", caretPos - 1);
        if (openBracePos == -1) {
            hideAutocomplete();
            return;
        }

        // nghiên cứu {{ Có sau này không }}
        int closeBracePos = text.indexOf("}}", openBracePos);
        if (closeBracePos != -1 && closeBracePos < caretPos) {
            hideAutocomplete();
            return;
        }

        // Lấy {{ Văn bản sau đây đóng vai trò là tiền tố bộ lọc
        String prefix = text.substring(openBracePos + 2, caretPos);

        // Lọc danh sách biến
        List<VariableInfo> filteredVariables = VariableResolver.filterVariablesWithType(prefix);

        if (filteredVariables.isEmpty()) {
            hideAutocomplete();
            return;
        }

        // Cập nhật danh sách
        autocompleteStartPos = openBracePos + 2;
        autocompleteModel.clear();
        for (VariableInfo varInfo : filteredVariables) {
            autocompleteModel.addElement(varInfo);
        }

        // Mục đầu tiên được chọn theo mặc định
        if (autocompleteModel.getSize() > 0) {
            autocompleteList.setSelectedIndex(0);
            autocompleteList.ensureIndexIsVisible(0);
        }

        // Hiển thị menu bật lên
        showAutocomplete();
    }

    /**
     * Hiển thị cửa sổ bật lên tự động hoàn thành
     */
    private void showAutocomplete() {
        if (autocompleteModel.getSize() == 0) {
            return;
        }

        // Kiểm tra xem thành phần có được hiển thị trên màn hình hay không
        if (!isShowing()) {
            return;
        }

        try {
            // Lấy vị trí con trỏ
            Rectangle rect = modelToView2D(getCaretPosition()).getBounds();
            Point screenPos = getLocationOnScreen();

            // Tính kích thước cửa sổ bật lên - Chiều rộng cố định là 400
            int itemHeight = 32; // chiều cao của từng hạng mục
            int popupWidth = 400;
            int popupHeight = Math.min(autocompleteModel.getSize() * itemHeight + 10, 320);

            // Đặt kích thước và vị trí cửa sổ
            autocompleteWindow.setSize(popupWidth, popupHeight);
            autocompleteWindow.setLocation(
                    screenPos.x + rect.x,
                    screenPos.y + rect.y + rect.height + 2
            );

            if (!autocompleteWindow.isVisible()) {
                autocompleteWindow.setVisible(true);
            }
        } catch (Exception e) {
            log.error("showAutocomplete", e);
        }
    }

    /**
     * Ẩn cửa sổ bật lên tự động hoàn thành
     */
    private void hideAutocomplete() {
        if (autocompleteWindow != null && autocompleteWindow.isVisible()) {
            autocompleteWindow.setVisible(false);
        }
        autocompleteStartPos = -1;
    }

    /**
     * Chèn biến đã chọn
     */
    private void insertSelectedVariable() {
        VariableInfo selected = autocompleteList.getSelectedValue();
        if (selected == null || autocompleteStartPos == -1) {
            return;
        }

        try {
            String text = getText();
            int caretPos = getCaretPosition();

            // xuất hiện {{ vị trí
            int openBracePos = text.lastIndexOf("{{", caretPos - 1);
            if (openBracePos == -1) {
                return;
            }

            // thay thế {{ Văn bản đến vị trí con trỏ là {{selected}}
            String before = text.substring(0, openBracePos);
            String after = text.substring(caretPos);

            // Kiểm tra xem đã có cái nào ở phía sau chưa }}，Nếu có thì bỏ qua để tránh trùng lặp
            String closingBraces = "}}";
            String varName = selected.getName();
            if (after.startsWith(closingBraces)) {
                // Đã có một cái ở phía sau rồi }}，Không có thêm bổ sung
                String newText = before + "{{" + varName + after;
                setText(newText);
                setCaretPosition(openBracePos + varName.length() + 4); // Con trỏ được đặt trên  }} sau đó
            } else {
                // Không có gì ở phía sau }}，Thêm bình thường
                String newText = before + "{{" + varName + "}}" + after;
                setText(newText);
                setCaretPosition(openBracePos + varName.length() + 4); // Con trỏ được đặt trên  }} sau đó
            }

            hideAutocomplete();
        } catch (Exception e) {
            log.error("insertSelectedVariable", e);
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        String value = getText();
        List<VariableSegment> segments = VariableParser.getVariableSegments(value);
        if (segments.isEmpty()) return super.getToolTipText(event);

        try {
            int mouseX = event.getX();
            var view = modelToView2D(0);
            if (view == null) {
                return super.getToolTipText(event);
            }

            Rectangle startRect = view.getBounds();
            FontMetrics fm = getFontMetrics(getFont());
            int x = startRect.x;
            int last = 0;

            for (VariableSegment seg : segments) {
                if (seg.start > last) {
                    String before = value.substring(last, seg.start);
                    int w = fm.stringWidth(before);
                    x += w;
                }
                String varText = value.substring(seg.start, seg.end);
                int varWidth = fm.stringWidth(varText);
                if (mouseX >= x && mouseX <= x + varWidth) {
                    // Di chuột qua biến
                    String varName = seg.name;

                    // Nhận loại và giá trị biến
                    VariableType varType = VariableResolver.getVariableType(varName);
                    String varValue = VariableResolver.resolveVariable(varName);

                    if (varType != null && varValue != null) {
                        // Biến được xác định
                        return VariableTooltipHtmlBuilder.variableTooltip(varName, varValue, varType);
                    } else {
                        // Biến không được xác định
                        return VariableTooltipHtmlBuilder.undefinedVariableTooltip(varName);
                    }
                }
                x += varWidth;
                last = seg.end;
            }
        } catch (Exception e) {
            log.error("getToolTipText", e);
        }
        return super.getToolTipText(event);
    }

}
