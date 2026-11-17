package com.laker.postman.common.component.tree;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.request.model.HttpRequestItem;


import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.common.component.HttpRequestDisplayMetadata;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.IconUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Trình kết xuất nút cây tùy chỉnh để làm đẹp JTree các nút được hiển thị
 * Khi chuột di chuột qua GROUP Nút được hiển thị ở bên phải "+" biểu tượng (tương tự như  Postman）
 * Tất cả các nút hover Toàn bộ hàng được đánh dấu bằng màu nền
 */
public class RequestTreeCellRenderer extends DefaultTreeCellRenderer {

    private static final int ICON_SIZE = 18;

    /**
     * Số dòng hiện tại mà chuột đang di chuột qua.-1 Cho biết không có sự đình chỉ; từ bên ngoài (MouseMotionListener）đổi mới
     */
    private int hoveredRow = -1;

    public void setHoveredRow(int row) {
        this.hoveredRow = row;
    }

    public int getHoveredRow() {
        return hoveredRow;
    }

    /**
     * "+" Chiều rộng vùng biểu tượng, khoảng cách từ cạnh phải: .more(20) + plus(20) = bên phải40pxbên trong
     */
    public static final int ADD_BUTTON_WIDTH = 20;
    /**
     * "⋯" more Icon ôm sát mép phải, rộng 20px
     */
    public static final int MORE_BUTTON_WIDTH = 20;

    /**
     * Liệu dòng kết xuất hiện tại có cần được hiển thị hay không "+" biểu tượng(GROUP nút hover thời gian)
     */
    private boolean showAddButton = false;
    /**
     * Liệu dòng kết xuất hiện tại có cần được hiển thị hay không "⋯" more biểu tượng(GROUP nút hover thời gian)
     */
    private boolean showMoreButton = false;

    /**
     * plus.svg Biểu tượng, chuyển thể chủ đề, lười tải
     */
    private transient Icon plusIcon = null;
    /**
     * more.svg Biểu tượng, chuyển thể chủ đề, lười tải
     */
    private transient Icon moreIcon = null;

    private Icon getPlusIcon() {
        if (plusIcon == null) {
            plusIcon = IconUtil.createThemed("icons/plus.svg", ICON_SIZE, ICON_SIZE);
        }
        return plusIcon;
    }

    private Icon getMoreIcon() {
        if (moreIcon == null) {
            moreIcon = IconUtil.createThemed("icons/more.svg", ICON_SIZE, ICON_SIZE);
        }
        return moreIcon;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        showAddButton = false;
        showMoreButton = false;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        CollectionTreeNodes.group(node).ifPresentOrElse(
                group -> renderGroupNode(node, group, row),
                () -> CollectionTreeNodes.request(node).ifPresentOrElse(
                        this::applyRequestRendering,
                        () -> CollectionTreeNodes.savedResponse(node).ifPresent(this::applySavedResponseRendering)
                )
        );

        return this;
    }

    /**
     * Tổng chiều rộng của cả hai biểu tượng + khoảng cách,hover Lề phải dành riêng cho văn bản
     */
    private static final int BUTTONS_RESERVED_WIDTH = ADD_BUTTON_WIDTH + MORE_BUTTON_WIDTH + 4;

    private void renderGroupNode(DefaultMutableTreeNode node, RequestGroup group, int row) {
        String groupName = group.getName();
        boolean isRootLevel = node.getParent() instanceof DefaultMutableTreeNode p &&
                CollectionTreePanel.ROOT.equals(String.valueOf(p.getUserObject()));
        boolean isHover = (row == hoveredRow);

        if (isRootLevel) {
            setIcon(new FlatSVGIcon("icons/collection.svg", ICON_SIZE, ICON_SIZE));

        } else {
            setIcon(new FlatSVGIcon("icons/group.svg", ICON_SIZE, ICON_SIZE));
        }

        if (isHover) {
            // hover Khi sử dụng văn bản thuần túy,JLabel Văn bản rất dài có thể được tự động bỏ qua
            setText(groupName);
        } else {
            String nameColor = toHex(ModernColors.getTextPrimary());
            setText("<html><nobr><span style='color:" + nameColor + "'>"
                    + escapeHtml(groupName) + "</span></nobr></html>");
        }

        if (isHover) {
            showAddButton = true;
            showMoreButton = true;
            // Thêm vào bên phải padding，JLabel Hình elip cắt bớt văn bản rất dài để nhường chỗ cho hai biểu tượng
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, BUTTONS_RESERVED_WIDTH));
        }
    }

    /**
     * Vẽ phía bên phải "+" và "⋯" biểu tượng(GROUP hover giờ).
     * Bố cục (từ phải sang trái):| more(20px) | plus(20px) |
     * Toàn bộ hàng hover Màu nền được cho bởi  JTree.paint() Vẽ đồng đều.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!showAddButton && !showMoreButton) return;

        int w = getWidth();
        int h = getHeight();

        // more Biểu tượng: dán vào cạnh phải
        if (showMoreButton) {
            Icon more = getMoreIcon();
            int x = w - more.getIconWidth() - 2;
            int y = (h - more.getIconHeight()) / 2;
            more.paintIcon(this, g, x, y);
        }

        // plus Biểu tượng: trong more trái
        if (showAddButton) {
            Icon plus = getPlusIcon();
            int x = w - MORE_BUTTON_WIDTH - plus.getIconWidth() - 2;
            int y = (h - plus.getIconHeight()) / 2;
            plus.paintIcon(this, g, x, y);
        }
    }

    private void applyRequestRendering(HttpRequestItem item) {
        String method = item.getMethod();
        String name = item.getName();
        RequestItemProtocolEnum protocol = item.getProtocol();
        String methodColor = HttpRequestDisplayMetadata.methodColorHex(method);

        if (protocol.isWebSocketProtocol()) {
            method = "WS";
            methodColor = HttpRequestDisplayMetadata.protocolColorHex(method);
        } else {
            method = HttpRequestDisplayMetadata.methodLabel(method);
        }
        setText(buildStyledText(method, methodColor, name));
    }

    private void applySavedResponseRendering(SavedResponse savedResponse) {
        setIcon(IconUtil.createThemed("icons/save-response.svg", IconUtil.SIZE_LARGE, IconUtil.SIZE_LARGE));
        String name = savedResponse.getName();
        int code = savedResponse.getCode();
        long timestamp = savedResponse.getTimestamp();
        String timeStr = new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date(timestamp));
        String statusColor = getStatusColor(code);
        setText(buildSavedResponseText(name, code, timeStr, statusColor));
    }

    private static String getStatusColor(int code) {
        if (code >= 200 && code < 300) return ModernColors.toHtmlColor(ModernColors.getSuccess());
        else if (code >= 300 && code < 400) return ModernColors.toHtmlColor(ModernColors.getInfo());
        else if (code >= 400 && code < 500) return ModernColors.toHtmlColor(ModernColors.getWarning());
        else if (code >= 500) return ModernColors.toHtmlColor(ModernColors.getError());
        return ModernColors.toHtmlColor(ModernColors.getNeutral());
    }

    private static String toHex(Color color) {
        return ModernColors.toHtmlColor(color);
    }

    private static String buildSavedResponseText(String name, int code, String timeStr, String statusColor) {
        String safeName = name == null ? "" : escapeHtml(name);
        int baseFontSize = SettingManager.getUiFontSize();
        int nameFontSize = Math.max(8, baseFontSize - 4);
        int statusFontSize = Math.max(7, baseFontSize - 5);
        int timeFontSize = Math.max(7, baseFontSize - 6);
        return "<html><nobr>"
                + "<span style='font-size:" + nameFontSize + "px'>" + safeName + "</span> "
                + "<span style='color:" + statusColor + ";font-size:" + statusFontSize + "px'>" + code + "</span> "
                + "<span style='color:" + ModernColors.toHtmlColor(ModernColors.getTextHint()) + ";font-size:"
                + timeFontSize + "px'>" + timeStr + "</span>"
                + "</nobr></html>";
    }

    private static String buildStyledText(String method, String methodColor, String name) {
        String safeMethod = method == null ? "" : escapeHtml(method);
        String safeName = name == null ? "" : escapeHtml(name);
        String color = methodColor == null ? ModernColors.toHtmlColor(ModernColors.getTextPrimary()) : methodColor;
        int baseFontSize = SettingManager.getUiFontSize();
        int methodFontSize = Math.max(7, baseFontSize - 5);
        int nameFontSize = Math.max(8, baseFontSize - 4);
        return "<html><nobr>"
                + "<span style='color:" + color + ";font-size:" + methodFontSize + "px'>" + safeMethod + "</span> "
                + "<span style='font-size:" + nameFontSize + "px'>" + safeName + "</span>"
                + "</nobr></html>";
    }

    private static String escapeHtml(String s) {
        if (s == null) return null;
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
