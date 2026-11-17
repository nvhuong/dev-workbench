package com.laker.postman.common.component.tab;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.collections.editor.RequestEditorEmptyStatePanel;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.service.setting.ShortcutManager;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Có thể đóng cửa Tab Thành phần tiêu đề.
 *
 * <p>Chức năng:
 * <ul>
 *   <li>Hiển thị phương thức yêu cầu/Logo thỏa thuận hoặc biểu tượng doanh nghiệp + Tiêu đề (tự động cắt ngắn nếu quá dài)</li>
 *   <li>Vẽ một nút đóng ở bên phải (hover hiển thị, nhấp để đóng Tab）</li>
 *   <li>Chấm nhỏ ở góc trên bên phải: màu vàng=Mới chưa được lưu, màu đỏ=Đã sửa đổi (bẩn)</li>
 *   <li>Chế độ tạm thời: Tiêu đề in nghiêng, biểu thị tạm thời có thể được sử dụng lại Tab</li>
 *   <li>Menu chuột phải: Đóng hiện tại / Đóng những người khác / Đóng phía bên phải / Đóng tất cả</li>
 *   <li>Hỗ trợ Tab Sắp xếp kéo và thả (append TabbedPaneDragHandler người nghe)</li>
 * </ul>
 */
@Slf4j
public class ClosableTabComponent extends JPanel {

    // ── Các hằng số bố cục ─────────────────────────────────────────────────────────────
    public static final String ELLIPSIS = "...";
    private static final int MAX_TAB_WIDTH     = 160;
    private static final int MIN_TAB_WIDTH     = 80;
    private static final int TAB_HEIGHT        = 28;
    private static final int CLOSE_DIAMETER    = 11;   // Đường kính vòng tròn nút đóng
    private static final int CLOSE_MARGIN      = 0;    // Nút đóng ở lề phải
    private static final int CLOSE_TEXT_SPACING = 0;   // Nút đóng và giãn cách văn bản
    private static final int CLOSE_HIT_PADDING = 3;    // Mở rộng khu vực nhấn nút đóng để cải thiện tính dễ sử dụng
    private static final int LABEL_LEFT_PAD    = 4;    // tiêu đề label Đệm trái

    // ── dữ liệu ─────────────────────────────────────────────────────────────────
    private final JLabel label;
    private final String labelText;
    private final JTabbedPane tabbedPane;

    // ── trạng thái ─────────────────────────────────────────────────────────────────
    private RequestTabMarkers markers = RequestTabMarkers.clean();
    private boolean hoverTab = false;            // Liệu con chuột có vượt qua toàn bộ hay không Tab thượng đẳng
    private boolean hoverClose = false;          // Liệu con chuột có ở trên nút đóng hay không

    // ── Người xây dựng ───────────────────────────────────────────────────────────────

    public ClosableTabComponent(String title, RequestItemProtocolEnum protocol, boolean isRoot) {
        this(title, null, protocol, isRoot, false);
    }

    public static ClosableTabComponent forRequest(String title, HttpRequestItem item) {
        Objects.requireNonNull(item, "item");
        return forRequest(title, item.getMethod(), item.getProtocol());
    }

    public static ClosableTabComponent forRequest(String title, String method, RequestItemProtocolEnum protocol) {
        return new ClosableTabComponent(title, method, protocol, false, true);
    }

    private ClosableTabComponent(String title, String method, RequestItemProtocolEnum protocol,
                                 boolean isRoot, boolean requestTab) {
        this.tabbedPane = UiSingletonFactory.getInstance(RequestEditorPanel.class).getTabbedPane();
        String safeTitle = title == null ? "" : title;

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setToolTipText(safeTitle);

        // Tính toán Tab chiều rộng và tạo label
        LabelPresentation labelPresentation = buildLabel(safeTitle, method, protocol, isRoot, requestTab);
        label = labelPresentation.label();
        labelText = labelPresentation.text();
        add(label, BorderLayout.CENTER);

        // Trình đơn nhấp chuột phải
        setComponentPopupMenu(buildPopupMenu());

        // Chuột:hover Hiển thị nút đóng, nhấp vào nút đóng, chọn Tab
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                updateHoverState(e.getX(), e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { updateHoverState(e.getX(), e.getY()); }
            @Override public void mouseExited(MouseEvent e)  { updateHoverState(-1, -1); }
            @Override public void mousePressed(MouseEvent e) { handleMousePress(e); }
        });

        // Hỗ trợ sắp xếp kéo và thả
        TabbedPaneDragHandler handler = UiSingletonFactory.getInstance(RequestEditorPanel.class).getDragHandler();
        if (handler != null) handler.attachTo(this);
    }

    // ── Hỗ trợ xây dựng: Xây dựng label ─────────────────────────────────────────────────

    private LabelPresentation buildLabel(String title, String method, RequestItemProtocolEnum protocol,
                                         boolean isRoot, boolean requestTab) {
        Font labelFont = FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1);
        FontMetrics fm = getFontMetrics(labelFont);
        RequestTabDisplayMetadata.Badge badge = requestTab
                ? RequestTabDisplayMetadata.badgeFor(method, protocol)
                : null;
        Icon icon = requestTab ? null : resolveIcon(protocol, isRoot);
        int closeSpace = CLOSE_DIAMETER + CLOSE_TEXT_SPACING + CLOSE_MARGIN;
        int iconSpace  = icon == null ? 0 : 20;
        int badgeSpace = badge == null ? 0 : fm.stringWidth(badge.text() + " ");
        int padding    = 20;

        int tabWidth = Math.max(
                Math.min(fm.stringWidth(title) + badgeSpace + iconSpace + closeSpace + padding, MAX_TAB_WIDTH),
                MIN_TAB_WIDTH);
        setPreferredSize(new Dimension(tabWidth, TAB_HEIGHT));

        // Văn bản sẽ bị cắt ngắn nếu quá dài
        String displayTitle = truncate(title, fm, tabWidth - badgeSpace - iconSpace - closeSpace - padding);
        String renderedText = RequestTabDisplayMetadata.labelText(badge, displayTitle);

        // label Nó không tự tiêu thụ các sự kiện chuột (để tránh chặn nút đóng)
        JLabel lbl = new JLabel(renderedText) {
            @Override public boolean contains(int x, int y) { return false; }
        };
        lbl.setFont(labelFont);
        lbl.setIcon(icon);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, LABEL_LEFT_PAD, 0, closeSpace + CLOSE_TEXT_SPACING));
        lbl.setHorizontalAlignment(SwingConstants.LEFT);
        lbl.setVerticalAlignment(SwingConstants.CENTER);
        return new LabelPresentation(lbl, renderedText);
    }

    private static String truncate(String title, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(title) <= maxWidth) return title;
        int len = title.length();
        while (len > 0 && fm.stringWidth(title.substring(0, len) + ELLIPSIS) > maxWidth) len--;
        return title.substring(0, len) + ELLIPSIS;
    }

    private static Icon resolveIcon(RequestItemProtocolEnum protocol, boolean isRoot) {
        if (protocol == RequestItemProtocolEnum.SAVED_RESPONSE) {
            return IconUtil.createThemed("icons/save-response.svg", 24, 24);
        }
        if (protocol != null) {
            return null;
        }
        return new FlatSVGIcon(isRoot ? "icons/collection.svg" : "icons/group.svg", 18, 18);
    }

    private record LabelPresentation(JLabel label, String text) {
    }

    // ── Xử lý sự kiện chuột ─────────────────────────────────────────────────────────

    private void updateHoverState(int x, int y) {
        boolean nextHoverTab = contains(x, y);
        boolean nextHoverClose = nextHoverTab && isInCloseButton(x, y);
        if (hoverTab != nextHoverTab || hoverClose != nextHoverClose) {
            hoverTab = nextHoverTab;
            hoverClose = nextHoverClose;
            repaint();
        }
    }

    private void handleMousePress(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        int idx = tabbedPane.indexOfTabComponent(this);
        if (idx < 0) return;

        if (isInCloseButton(e.getX(), e.getY())) {
            // Bấm vào nút đóng: chọn trước rồi đóng
            tabbedPane.setSelectedIndex(idx);
            UiSingletonFactory.getInstance(RequestEditorPanel.class).closeCurrentTab();
        } else {
            // Click bình thường: Chọn và định vị ở cây bên trái
            tabbedPane.setSelectedIndex(idx);
            locateInTree(idx);
        }
    }

    private void locateInTree(int idx) {
        Component comp = tabbedPane.getComponentAt(idx);
        if (!(comp instanceof RequestEditSubPanel subPanel)) return;
        HttpRequestItem req = subPanel.getCurrentRequest();
        if (req == null || req.getId() == null) return;
        SwingUtilities.invokeLater(() -> {
            try {
                UiSingletonFactory.getInstance(CollectionTreePanel.class)
                        .locateAndSelectRequest(req.getId());
            } catch (Exception ex) {
                log.error("Đã xảy ra lỗi khi định vị nút yêu cầu", ex);
            }
        });
    }

    // ── Đóng khu vực nút ─────────────────────────────────────────────────────────

    private Rectangle closeButtonBounds() {
        int x = getWidth() - CLOSE_DIAMETER - CLOSE_MARGIN;
        int y = (getHeight() - CLOSE_DIAMETER) / 2;
        return new Rectangle(x, y, CLOSE_DIAMETER, CLOSE_DIAMETER);
    }

    /** Xác định xem tọa độ có nằm trên nút đóng hay không (cho chính nó và TabbedPaneDragHandler sử dụng) */
    public boolean isInCloseButton(int x, int y) {
        Rectangle hitBounds = closeButtonBounds();
        hitBounds.grow(CLOSE_HIT_PADDING, CLOSE_HIT_PADDING);
        return hitBounds.contains(x, y);
    }

    // ── vẽ ─────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Rectangle cb = closeButtonBounds();
        int x = cb.x;
        int y = cb.y;
        int r = CLOSE_DIAMETER;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (hoverClose) {
            g2.setColor(closeButtonBg());
            g2.fillOval(x, y, r, r);
            g2.setColor(closeButtonFg());
            g2.setStroke(new BasicStroke(1.5f));
            int pad = 2;
            g2.drawLine(x + pad, y + pad, x + r - pad, y + r - pad);
            g2.drawLine(x + r - pad, y + pad, x + pad, y + r - pad);
        } else if (hoverTab) {
            g2.setColor(closeButtonFg());
            g2.setStroke(new BasicStroke(1.5f));
            int pad = 2;
            g2.drawLine(x + pad, y + pad, x + r - pad, y + r - pad);
            g2.drawLine(x + r - pad, y + pad, x + pad, y + r - pad);
        } else if (markers.isNewRequest()) {
            g2.setColor(new Color(255, 204, 0, 180));   // Chấm màu vàng
            g2.fillOval(x, y, r, r);
        } else if (markers.isDirty()) {
            g2.setColor(dirtyColor());                   // chấm đỏ
            g2.fillOval(x, y, r, r);
        }
        g2.dispose();
    }

    // ── Màu sắc phù hợp với chủ đề ───────────────────────────────────────────────────────────

    private Color closeButtonBg() {
        Color hoverColor = UIManager.getColor("TabbedPane.hoverColor");
        if (hoverColor != null) {
            return ModernColors.withAlpha(hoverColor, 160);
        }
        return ModernColors.withAlpha(ModernColors.getHoverBackgroundColor(), 160);
    }

    private Color closeButtonFg() {
        return closeButtonForegroundColor();
    }

    private Color dirtyColor() {
        return dirtyDotColor();
    }

    static Color closeButtonForegroundColor() {
        return ModernColors.getTextPrimary();
    }

    static Color dirtyDotColor() {
        return ModernColors.withAlpha(ModernColors.getError(), 180);
    }

    // ── Trình đơn nhấp chuột phải ─────────────────────────────────────────────────────────────

    private JPopupMenu buildPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);
        RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);

        JMenuItem closeCurrent = menuItem(MessageKeys.TAB_CLOSE_CURRENT, ShortcutManager.CLOSE_CURRENT_TAB,
                () -> { int i = tabbedPane.indexOfTabComponent(this); if (i >= 0) { tabbedPane.setSelectedIndex(i); editPanel.closeCurrentTab(); } });

        JMenuItem closeOthers = menuItem(MessageKeys.TAB_CLOSE_OTHERS, ShortcutManager.CLOSE_OTHER_TABS,
                () -> { int i = tabbedPane.indexOfTabComponent(this); if (i >= 0) { tabbedPane.setSelectedIndex(i); editPanel.closeOtherTabs(); } });

        JMenuItem closeRight = new JMenuItem(I18nUtil.getMessage(MessageKeys.TAB_CLOSE_RIGHT));
        closeRight.addActionListener(e -> closeTabsToTheRight());

        JMenuItem closeAll = menuItem(MessageKeys.TAB_CLOSE_ALL, ShortcutManager.CLOSE_ALL_TABS,
                editPanel::closeAllTabs);

        menu.add(closeCurrent);
        menu.addSeparator();
        menu.add(closeOthers);
        menu.add(closeRight);
        menu.addSeparator();
        menu.add(closeAll);
        return menu;
    }

    private JMenuItem menuItem(String msgKey, String shortcutKey, Runnable action) {
        JMenuItem item = new JMenuItem(I18nUtil.getMessage(msgKey));
        KeyStroke ks = ShortcutManager.getKeyStroke(shortcutKey);
        if (ks != null) item.setAccelerator(ks);
        item.addActionListener(e -> action.run());
        return item;
    }

    private void closeTabsToTheRight() {
        int thisIdx = tabbedPane.indexOfTabComponent(this);
        if (thisIdx < 0) return;
        RequestEditorPanel editPanel = UiSingletonFactory.getInstance(RequestEditorPanel.class);
        for (int i = tabbedPane.getTabCount() - 1; i > thisIdx; i--) {
            if (!(tabbedPane.getComponentAt(i) instanceof RequestEditorEmptyStatePanel)) {
                editPanel.removeTabAtWithCleanup(i);
            }
        }
    }

    // ── Marker state ─────────────────────────────────────────────────────────

    public RequestTabMarkers getMarkers() {
        return markers;
    }

    public void updateMarkers(UnaryOperator<RequestTabMarkers> markerUpdater) {
        if (markerUpdater == null) {
            return;
        }
        applyMarkers(markerUpdater.apply(markers));
    }

    private void applyMarkers(RequestTabMarkers markers) {
        this.markers = markers == null ? RequestTabMarkers.clean() : markers;
        label.setText(labelText);
        label.setFont(FontsUtil.getDefaultFontWithOffset(this.markers.isPreviewMode() ? Font.ITALIC : Font.PLAIN, -1));
        repaint();
    }
}
