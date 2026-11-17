package com.laker.postman.common.component.tab;

import com.laker.postman.common.constants.ModernColors;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tab Kéo bộ xử lý sắp xếp (IDEA phong cách)
 *
 * <p>Bởi vì {@link ClosableTabComponent} Được che phủ Tab Khu vực đầu,
 * tabbedPane Bản thân tôi không thể nhận được mouseDragged sự kiện, vì vậy trình nghe phải được cài đặt trên mỗi sự kiện tabComponent thượng đẳng.
 * Mỗi người được tạo ra ClosableTabComponent，gọi {@link #attachTo} Chỉ cần đăng ký một lần.
 *
 * <p>Quá trình kéo: nhấn → Chuyển động vượt quá ngưỡng → Một đường thẳng đứng màu xanh được hiển thị cho biết vị trí → Hãy đi để hoàn thành việc phân loại.
 * "+" Tab Luôn xếp cuối cùng, không thể kéo hoặc nhắm mục tiêu.
 */
@Slf4j
public class TabbedPaneDragHandler {

    // ── Gọi lại, được sử dụng để đồng bộ hóa RequestEditorPanel trong trackedTabIndex ─────────────────
    public interface IndexGetter { int get(); }
    public interface IndexSetter { void set(int value); }

    // ── Trạng thái thoáng qua trong quá trình kéo (quản lý tập trung, thuận tiện reset）────────────────────────
    private static class DragState {
        int draggedTabIndex = -1;   // đang bị kéo đi Tab chỉ số
        int dropSlotIndex = -1;     // Khe chèn di chuột hiện tại nằm trong phạm vi 0..bình thường Tab Số lượng
        Point pressPointInPane;     // Bật khi nhấn tabbedPane Vị trí của hệ tọa độ
        boolean active = false;     // Liệu nó đã vượt quá ngưỡng và bước vào trạng thái kéo thực sự hay chưa

        boolean isReady() {
            return draggedTabIndex >= 0 && pressPointInPane != null;
        }

        void reset() {
            draggedTabIndex = -1;
            dropSlotIndex = -1;
            pressPointInPane = null;
            active = false;
        }
    }

    private static final int DRAG_THRESHOLD = 5; // Độ dịch chuyển pixel tối thiểu kích hoạt thao tác kéo

    private final JTabbedPane tabbedPane;
    private final IndexGetter trackedIndexGetter;
    private final IndexSetter trackedIndexSetter;

    private final DragState state = new DragState();

    // GlassPane Có liên quan
    private TabDropIndicator dropIndicator;    // Kéo đường chỉ báo
    private Component savedGlassPane;          // Tiếp tục sau khi kéo

    // ── Phương pháp nhà máy ─────────────────────────────────────────────────────────────

    private TabbedPaneDragHandler(JTabbedPane tabbedPane,
                                   IndexGetter trackedIndexGetter,
                                   IndexSetter trackedIndexSetter) {
        this.tabbedPane = tabbedPane;
        this.trackedIndexGetter = trackedIndexGetter;
        this.trackedIndexSetter = trackedIndexSetter;
    }

    /** Tạo và ràng buộc với quy định tabbedPane */
    public static TabbedPaneDragHandler install(JTabbedPane tabbedPane,
                                                 IndexGetter trackedIndexGetter,
                                                 IndexSetter trackedIndexSetter) {
        return new TabbedPaneDragHandler(tabbedPane, trackedIndexGetter, trackedIndexSetter);
    }

    // ── Phương thức công khai: Đăng ký tabComponent ────────────────────────────────────────

    /**
     * Liên kết trình nghe kéo với a tabComponent。
     * Vào {@link ClosableTabComponent} Được gọi ở cuối hàm tạo.
     */
    public void attachTo(Component tabComponent) {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                onPress(e, tabComponent);
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                onDrag(e, tabComponent);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                onRelease();
            }
        };
        tabComponent.addMouseListener(adapter);
        tabComponent.addMouseMotionListener(adapter);
    }

    // ── Xử lý sự kiện (riêng tư)─────────────────────────────────────────────────────

    private void onPress(MouseEvent e, Component tabComponent) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        // Đừng bắt đầu kéo khi nhấp vào nút đóng
        if (tabComponent instanceof ClosableTabComponent ctc && ctc.isInCloseButton(e.getX(), e.getY())) return;

        Point panePoint = toPanePoint(e.getPoint(), tabComponent);
        int idx = tabIndexAt(panePoint, tabComponent);
        if (idx < 0 || isPlusTab(idx)) return;

        state.draggedTabIndex = idx;
        state.pressPointInPane = panePoint;
        state.active = false;
    }

    private void onDrag(MouseEvent e, Component tabComponent) {
        if (!state.isReady()) return;
        Point panePoint = toPanePoint(e.getPoint(), tabComponent);

        // Khi vượt quá ngưỡng, thao tác kéo thực sự được thực hiện
        if (!state.active) {
            if (panePoint.distance(state.pressPointInPane) < DRAG_THRESHOLD) return;
            state.active = true;
            tabbedPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            showDropIndicator();
        }

        int slot = dropSlotAt(panePoint);
        if (slot != state.dropSlotIndex) {
            state.dropSlotIndex = slot;
            updateDropIndicator(slot);
        }
    }

    private void onRelease() {
        try {
            if (state.active && state.dropSlotIndex >= 0) {
                moveTabToSlot(state.draggedTabIndex, state.dropSlotIndex);
            }
        } finally {
            endDrag();
        }
    }

    // ── điều phối & Công cụ lập chỉ mục ──────────────────────────────────────────────────────

    /** Sẽ tabComponent Các điểm trên được chuyển đổi thành tabbedPane hệ tọa độ */
    private Point toPanePoint(Point componentPoint, Component tabComponent) {
        return SwingUtilities.convertPoint(tabComponent, componentPoint, tabbedPane);
    }

    /** Tìm theo tọa độ Tab Chỉ mục, được sử dụng nếu không tìm thấy indexOfTabComponent Nhận được đáy của nó */
    private int tabIndexAt(Point panePoint, Component tabComponent) {
        int idx = tabbedPane.indexAtLocation(panePoint.x, panePoint.y);
        if (idx < 0) idx = tabbedPane.indexOfTabComponent(tabComponent);
        return idx;
    }

    /** "+" Tab Luôn là người cuối cùng và không tham gia kéo */
    private boolean isPlusTab(int idx) {
        return idx == tabbedPane.getTabCount() - 1;
    }

    /**
     * Tính toán khe chèn dựa trên vị trí chuột (không bao gồm "+" Tab）。
     * Con chuột ở đâu đó Tab nửa bên trái → Rơi tại chỗ Tab Mặt trận; nửa bên phải → Rơi tại chỗ Tab mặt sau.
     */
    int dropSlotAt(Point panePoint) {
        int count = normalTabCount();
        if (count <= 0) {
            return -1;
        }

        Rectangle rowReference = nearestRowReference(panePoint.y, count);
        if (rowReference == null) {
            return -1;
        }

        List<Integer> rowIndices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Rectangle b = tabbedPane.getBoundsAt(i);
            if (b != null && sameVisualRow(rowReference, b)) {
                rowIndices.add(i);
            }
        }
        rowIndices.sort(Comparator.comparingInt(index -> tabbedPane.getBoundsAt(index).x));

        for (Integer index : rowIndices) {
            Rectangle b = tabbedPane.getBoundsAt(index);
            if (panePoint.x < b.x + b.width / 2) {
                return index;
            }
        }
        return rowIndices.isEmpty() ? -1 : rowIndices.get(rowIndices.size() - 1) + 1;
    }

    private Rectangle nearestRowReference(int paneY, int normalTabCount) {
        Rectangle nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < normalTabCount; i++) {
            Rectangle b = tabbedPane.getBoundsAt(i);
            if (b == null) {
                continue;
            }
            if (paneY >= b.y && paneY <= b.y + b.height) {
                return b;
            }
            int distance = paneY < b.y ? b.y - paneY : paneY - (b.y + b.height);
            if (distance < nearestDistance) {
                nearest = b;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean sameVisualRow(Rectangle rowReference, Rectangle bounds) {
        int referenceCenter = rowReference.y + rowReference.height / 2;
        int boundsCenter = bounds.y + bounds.height / 2;
        return Math.abs(referenceCenter - boundsCenter) <= Math.max(rowReference.height, bounds.height) / 2;
    }

    private int normalTabCount() {
        return Math.max(0, tabbedPane.getTabCount() - 1);
    }

    // ── GlassPane đường chỉ báo ─────────────────────────────────────────────────────

    private void showDropIndicator() {
        JRootPane rootPane = SwingUtilities.getRootPane(tabbedPane);
        if (rootPane == null) return;
        savedGlassPane = rootPane.getGlassPane();
        dropIndicator = new TabDropIndicator();
        rootPane.setGlassPane(dropIndicator);
        dropIndicator.setVisible(true);
    }

    private void updateDropIndicator(int dropSlot) {
        if (dropIndicator == null) return;
        if (dropSlot < 0) { dropIndicator.clearLine(); return; }

        Rectangle line = dropIndicatorLine(dropSlot);
        if (line == null) {
            dropIndicator.clearLine();
            return;
        }

        Point p = SwingUtilities.convertPoint(tabbedPane, line.x, line.y, dropIndicator);
        dropIndicator.show(p.x, p.y, line.height);
    }

    private Rectangle dropIndicatorLine(int dropSlot) {
        int normalTabCount = normalTabCount();
        if (normalTabCount <= 0) {
            return null;
        }

        if (dropSlot <= 0) {
            Rectangle first = tabbedPane.getBoundsAt(0);
            return first == null ? null : new Rectangle(first.x, first.y, 0, first.height);
        }
        if (dropSlot >= normalTabCount) {
            Rectangle last = tabbedPane.getBoundsAt(normalTabCount - 1);
            return last == null ? null : new Rectangle(last.x + last.width, last.y, 0, last.height);
        }

        Rectangle target = tabbedPane.getBoundsAt(dropSlot);
        return target == null ? null : new Rectangle(target.x, target.y, 0, target.height);
    }

    private void endDrag() {
        state.reset();
        tabbedPane.setCursor(Cursor.getDefaultCursor());

        if (dropIndicator != null) dropIndicator.setVisible(false);

        JRootPane rootPane = SwingUtilities.getRootPane(tabbedPane);
        if (rootPane != null && savedGlassPane != null) {
            rootPane.setGlassPane(savedGlassPane);
            savedGlassPane.setVisible(false);
        }
        dropIndicator = null;
        savedGlassPane = null;
    }

    // ── Tab di chuyển ──────────────────────────────────────────────────────────────

    /**
     * Sẽ from vị trí Tab Di chuyển đến khe chèn được chỉ định.
     * Giữ nó nguyên vẹn tabComponent（ClosableTabComponent）、content、title、icon、tooltip、enabled，
     * và đồng bộ hóa selectedIndex Và trackedTabIndex。
     */
    void moveTabToSlot(int from, int dropSlot) {
        int normalTabCount = normalTabCount();
        if (from < 0 || from >= normalTabCount || dropSlot < 0 || dropSlot > normalTabCount) {
            return;
        }
        if (dropSlot == from || dropSlot == from + 1) {
            return;
        }

        int insertAt = dropSlot > from ? dropSlot - 1 : dropSlot;
        moveTabToInsertIndex(from, insertAt);
    }

    private void moveTabToInsertIndex(int from, int insertAt) {
        int tabCount = tabbedPane.getTabCount();
        if (from == insertAt || from < 0 || insertAt < 0 || from >= tabCount || insertAt >= tabCount) return;

        // 1. Đọc from vị trí Tab tất cả dữ liệu
        String title      = tabbedPane.getTitleAt(from);
        Icon icon         = tabbedPane.getIconAt(from);
        Component content = tabbedPane.getComponentAt(from);
        String tooltip    = tabbedPane.getToolTipTextAt(from);
        boolean enabled   = tabbedPane.isEnabledAt(from);
        Component tabComp = tabbedPane.getTabComponentAt(from);
        int selectedIndex = tabbedPane.getSelectedIndex();
        int trackedIdx    = trackedIndexGetter.get();

        // 2. Xóa vị trí ban đầu và chèn nó vào vị trí đích
        tabbedPane.removeTabAt(from);
        insertAt = Math.min(insertAt, tabbedPane.getTabCount()); // Ngăn chặn việc vượt qua ranh giới
        tabbedPane.insertTab(title, icon, content, tooltip, insertAt);
        tabbedPane.setEnabledAt(insertAt, enabled);
        if (tabComp != null) tabbedPane.setTabComponentAt(insertAt, tabComp);

        // 3. đồng bộ selectedIndex và trackedTabIndex（Nguyên nhân chỉ số remove+insert xảy ra sự bù đắp)
        tabbedPane.setSelectedIndex(recalcIndex(selectedIndex, from, insertAt));
        if (trackedIdx >= 0) trackedIndexSetter.set(recalcIndex(trackedIdx, from, insertAt));

        log.debug("Tab moved: {} → {}", from, insertAt);
    }

    /**
     * Tính chỉ số tại removeTabAt(from) + insertTab(insertAt) địa điểm mới sau đó.
     */
    private int recalcIndex(int idx, int removedFrom, int insertAt) {
        if (idx == removedFrom) return insertAt;               // Đó là cái đã được di chuyển
        int afterRemove = (idx > removedFrom) ? idx - 1 : idx; // mô phỏng remove phần bù tiếp theo
        return (afterRemove >= insertAt) ? afterRemove + 1 : afterRemove; // mô phỏng insert phần bù tiếp theo
    }

    // ── Lớp bên trong: Kéo dòng chỉ báo vị trí (được bao phủ trong RootPane GlassPane ở trên)────────────────

    /**
     * Bảng trong suốt, vẽ một đường thẳng đứng màu xanh + Mũi tên lên xuống, hướng dẫn Tab Vị trí cần chèn.
     * Màu sắc sẽ tự động theo sau FlatLaf chủ đề.
     */
    static class TabDropIndicator extends JPanel {

        private int lineX = -1;
        private int lineY;
        private int lineHeight;

        TabDropIndicator() {
            setOpaque(false);
            setEnabled(false); // Không sử dụng các sự kiện chuột
        }

        void show(int x, int y, int height) {
            lineX = x; lineY = y; lineHeight = height;
            repaint();
        }

        void clearLine() {
            lineX = -1;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (lineX < 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Màu sắc thích ứng chủ đề: ưu tiên FlatLaf Màu sắc tập trung
            Color color = UIManager.getColor("Component.focusColor");
            if (color == null) color = UIManager.getColor("TabbedPane.underlineColor");
            if (color == null) color = ModernColors.getPrimary();
            g2.setColor(color);

            // Đường dọc
            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(lineX, lineY, lineX, lineY + lineHeight);

            // Đỉnh hướng xuống dưới trong một hình tam giác
            int a = 5;
            g2.fillPolygon(
                    new int[]{lineX - a, lineX + a, lineX},
                    new int[]{lineY, lineY, lineY + a * 2}, 3);
            // Tam giác từ dưới lên
            int by = lineY + lineHeight;
            g2.fillPolygon(
                    new int[]{lineX - a, lineX + a, lineX},
                    new int[]{by, by, by - a * 2}, 3);

            g2.dispose();
        }
    }
}
