package com.nvhuong.devworkbench.common.component;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatComboBoxUI;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.UIScale;
import com.nvhuong.devworkbench.util.FontsUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListDataEvent;
import javax.swing.plaf.ComboBoxUI;
import java.awt.*;

/**
 * Thành phần hộp thả xuống nâng cao
 * <p>
 * Các tính năng:
 * - Tự động điều chỉnh độ rộng theo mục đang được chọn, tiết kiệm không gian và giảm thiểu hạn chế/Chiều rộng tối đa (khi vượt quá chiều rộng tối đa tooltip Hiển thị đầy đủ nội dung)
 * - Hỗ trợ chế độ chiều rộng cố định
 * - Tự động thích ứng FlatLaf chủ đề
 * - Phông chữ và phong cách thống nhất
 * </p>
 *
 * @param <E> Loại mục hộp thả xuống
 */
public class DevWorkbenchComboBox<E> extends JComboBox<E> {

    /**
     * chế độ chiều rộng
     */
    public enum WidthMode {
        /**
         * Tự động điều chỉnh độ rộng dựa trên mục hiện được chọn
         */
        DYNAMIC,
        /**
         * Đã sửa chiều rộng dựa trên mục dài nhất trong tất cả các tùy chọn
         */
        FIXED_MAX,
        /**
         * Chỉ định chiều rộng cố định theo cách thủ công
         */
        FIXED_CUSTOM
    }

    private static final int DEFAULT_MIN_WIDTH = 80;

    private WidthMode widthMode = WidthMode.DYNAMIC;
    private int customWidth = -1;
    /** DYNAMIC Giới hạn dưới của chiều rộng chế độ (không chia tỷ lệ).≤0 Cho biết việc sử dụng hành vi mặc định */
    private int minDynamicWidth = -1;
    /** DYNAMIC Độ rộng chế độ tối đa (không chia tỷ lệ), .≤0 Cho biết không có hạn chế */
    private int maxDynamicWidth = -1;
    /** Chiều cao cố định (không chia tỷ lệ).≤0 Cho biết sử dụng LAF Tính chiều cao */
    private int fixedHeight = -1;

    /** model Số lượng thay đổi nội dung, được sử dụng để xác định xem bộ đệm kích thước đã hết hạn hay chưa */
    private int modelVersion;
    private JList<E> measureList;
    private SizeKey cachedKey;
    private DynamicLayout cachedLayout;

    /**
     * Tạo hộp thả xuống có chiều rộng động mặc định
     */
    public DevWorkbenchComboBox() {
        this(WidthMode.DYNAMIC);
    }

    /**
     * Tạo hộp thả xuống với chế độ chiều rộng được chỉ định
     *
     * @param widthMode chế độ chiều rộng
     */
    public DevWorkbenchComboBox(WidthMode widthMode) {
        super();
        this.widthMode = widthMode;
        initComponent();
    }

    /**
     * Tạo một hộp thả xuống với các tùy chọn và chế độ chiều rộng được chỉ định
     *
     * @param items     Tùy chọn hộp thả xuống
     * @param widthMode chế độ chiều rộng
     */
    public DevWorkbenchComboBox(E[] items, WidthMode widthMode) {
        super(items);
        this.widthMode = widthMode;
        initComponent();
    }

    /**
     * Tạo hộp thả xuống có chiều rộng động
     *
     * @param items Tùy chọn hộp thả xuống
     */
    public DevWorkbenchComboBox(E[] items) {
        this(items, WidthMode.DYNAMIC);
    }

    /**
     * Tạo một hộp thả xuống có chiều rộng cố định được chỉ định
     *
     * @param items Tùy chọn hộp thả xuống
     * @param width Chiều rộng cố định (pixel)
     */
    public DevWorkbenchComboBox(E[] items, int width) {
        super(items);
        this.widthMode = WidthMode.FIXED_CUSTOM;
        this.customWidth = width;
        initComponent();
    }

    private void initComponent() {
        setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        setFocusable(false);
        // Khi mục đã chọn thay đổi BasicComboBoxUI Sẽ không có sự sắp xếp lại.DYNAMIC Chế độ này cần được kích hoạt bằng tay
        addItemListener(e -> {
            if (isDynamicSizing()) {
                revalidate();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        if (widthMode == WidthMode.FIXED_CUSTOM && customWidth > 0) {
            return new Dimension(customWidth, resolveHeight(super.getPreferredSize().height));
        }
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        if (widthMode == WidthMode.FIXED_MAX) {
            Dimension size = super.getPreferredSize();
            return new Dimension(size.width, resolveHeight(size.height));
        }
        DynamicLayout layout = dynamicLayout();
        return new Dimension(layout.width(), layout.height());
    }

    @Override
    public Dimension getMaximumSize() {
        // BoxLayout Sẽ sử dụng nó getMaximumSize() Ràng buộc thành phần sao cho nó bằng  preferredSize Thế thôi
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension d = getPreferredSize();
        int min = minDynamicWidth > 0 ? UIScale.scale(minDynamicWidth) : DEFAULT_MIN_WIDTH;
        return new Dimension(Math.min(min, d.width), d.height);
    }

    /**
     * Khi mục đã chọn bị cắt bớt do chiều rộng tối đa, nội dung đầy đủ sẽ được hiển thị, nếu không thì cài đặt bên ngoài sẽ được hiển thị tooltip。
     */
    @Override
    public String getToolTipText() {
        if (isDynamicSizing() && dynamicLayout().truncated()) {
            return displayText(selectedElement());
        }
        return super.getToolTipText();
    }

    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(text);
        // super Vào text vì null sẽ bị đăng xuất ToolTipManager，Nó vẫn được yêu cầu cho các gợi ý cắt ngắn
        registerToolTipIfNeeded();
    }

    /**
     * cài đặt DYNAMIC Phạm vi chiều rộng của mẫu (pixel không được chia tỷ lệ, theo  UI phóng).
     * Sau khi đặt chiều rộng tối đa, mục đã chọn sẽ bị cắt bớt tooltip Hiển thị đầy đủ nội dung.
     */
    public void setDynamicWidthRange(int minWidth, int maxWidth) {
        this.minDynamicWidth = minWidth;
        this.maxDynamicWidth = maxWidth;
        registerToolTipIfNeeded();
        revalidate();
    }

    /**
     * Đặt chiều cao cố định (pixel chưa được chia tỷ lệ, nhấn UI phóng),≤0 Trở lại LAF Chiều cao tính toán.
     */
    public void setFixedHeight(int height) {
        this.fixedHeight = height;
        revalidate();
    }

    /**
     * Đặt chế độ chiều rộng
     *
     * @param widthMode chế độ chiều rộng
     */
    public void setWidthMode(WidthMode widthMode) {
        this.widthMode = widthMode;
        revalidate();
    }

    /**
     * Đặt chiều rộng cố định tùy chỉnh
     *
     * @param width Chiều rộng (pixel)
     */
    public void setCustomWidth(int width) {
        this.widthMode = WidthMode.FIXED_CUSTOM;
        this.customWidth = width;
        revalidate();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // Được sử dụng để xây dựng lại các phép đo sau khi chuyển đổi chủ đề JList，Làm được UI Phù hợp với chủ đề hiện tại
        measureList = null;
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        modelVersion++;
        super.contentsChanged(e);
    }

    @Override
    public void intervalAdded(ListDataEvent e) {
        modelVersion++;
        super.intervalAdded(e);
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        modelVersion++;
        super.intervalRemoved(e);
    }

    private boolean isDynamicSizing() {
        return widthMode != WidthMode.FIXED_MAX
                && !(widthMode == WidthMode.FIXED_CUSTOM && customWidth > 0)
                && !isPreferredSizeSet();
    }

    private void registerToolTipIfNeeded() {
        if (maxDynamicWidth > 0) {
            ToolTipManager.sharedInstance().registerComponent(this);
        }
    }

    /**
     * Tính toán và lưu trữ theo yêu cầu DYNAMIC kích cỡ. Bộ đệm được khóa bằng ảnh chụp nhanh trạng thái ảnh hưởng đến kích thước.
     * Nó không phụ thuộc vào sự hết hạn của sự kiện, vì vậy thành phần sẽ không hết hạn ngay cả khi nó không được thêm vào vùng chứa, chuyển đổi chủ đề, v.v. và sẽ không được sử dụng UI Tính toán trước thời gian trong quá trình cài đặt.
     */
    private DynamicLayout dynamicLayout() {
        SizeKey key = new SizeKey(getUI(), getFont(), getRenderer(), getPrototypeDisplayValue(), getBorder(),
                getModel(), modelVersion, getSelectedItem(), minDynamicWidth, maxDynamicWidth, fixedHeight,
                UIScale.getUserScaleFactor(), getClientProperty(FlatClientProperties.STYLE),
                getClientProperty(FlatClientProperties.STYLE_CLASS),
                getClientProperty(FlatClientProperties.MINIMUM_WIDTH));
        if (!key.equals(cachedKey) || cachedLayout == null) {
            cachedLayout = calcDynamicLayout();
            cachedKey = key;
        }
        return cachedLayout;
    }

    /**
     * super Chiều rộng ưa thích = max(Chiều rộng tham chiếu (prototype hoặc mục rộng nhất)+ Trang trí (phần đệm, nút mũi tên, đường viền), .LAF Chiều rộng tối thiểu)，
     * Chiều rộng trang trí bắt nguồn từ điều này mà không cần phải thay thế tạm thời model。super Đã được LAF Khi chiều rộng tối thiểu là đáy, chiều rộng của bất kỳ tùy chọn nào sẽ không vượt quá giá trị này và giá trị đáy được lấy trực tiếp.
     * Cài đặt prototype Khi sử dụng chiều rộng của nó (JComboBox Ngữ nghĩa), nếu không thì theo chiều rộng mục đã chọn và cuối cùng được giới hạn ở [min, max] khoảng thời gian.
     */
    private DynamicLayout calcDynamicLayout() {
        Dimension superSize = super.getPreferredSize();
        int height = resolveHeight(superSize.height);
        ListCellRenderer<? super E> renderer = getRenderer();
        if (renderer == null || getUI() == null) {
            return new DynamicLayout(clampDynamicWidth(superSize.width), height, false);
        }

        E prototype = getPrototypeDisplayValue();
        int referenceWidth = prototype != null ? measureWidth(prototype) : widestItemWidth();
        int lafMinWidth = lafMinimumWidth();
        E selected = selectedElement();
        int selectedNaturalWidth;
        if (superSize.width <= lafMinWidth) {
            selectedNaturalWidth = superSize.width;
        } else {
            int decoration = Math.max(0, superSize.width - referenceWidth);
            int contentWidth = selected == null ? 0 : measureWidth(selected) + decoration;
            selectedNaturalWidth = Math.max(contentWidth, lafMinWidth);
        }

        int width = clampDynamicWidth(prototype != null ? superSize.width : selectedNaturalWidth);
        return new DynamicLayout(width, height, selectedNaturalWidth > width);
    }

    /**
     * FlatLaf Có chiều rộng tối thiểu cho hộp thả xuống (ComboBox.minimumWidth，Có thể được style / JComponent.minimumWidth che phủ),
     * Và FlatComboBoxUI#getMinimumSize Hãy nhất quán; khác LAF Không có hạn chế như vậy.
     */
    private int lafMinimumWidth() {
        if (!(getUI() instanceof FlatComboBoxUI flatUI)) {
            return 0;
        }
        int minimumWidth;
        try {
            minimumWidth = ((Number) flatUI.getStyleableValue(this, "minimumWidth")).intValue();
        } catch (RuntimeException e) {
            minimumWidth = UIManager.getInt("ComboBox.minimumWidth");
        }
        int focusWidth = Math.round(FlatUIUtils.getBorderFocusWidth(this) * 2);
        return UIScale.scale(FlatUIUtils.minimumWidth(this, minimumWidth)) + focusWidth;
    }

    private int widestItemWidth() {
        int widest = 0;
        for (int i = 0; i < getItemCount(); i++) {
            widest = Math.max(widest, measureWidth(getItemAt(i)));
        }
        return widest;
    }

    private int clampDynamicWidth(int width) {
        if (maxDynamicWidth > 0) {
            width = Math.min(width, UIScale.scale(maxDynamicWidth));
        }
        if (minDynamicWidth > 0) {
            width = Math.max(width, UIScale.scale(minDynamicWidth));
        }
        return width;
    }

    private int resolveHeight(int lafHeight) {
        return fixedHeight > 0 ? UIScale.scale(fixedHeight) : lafHeight;
    }

    private int measureWidth(E value) {
        return renderComponent(value).getPreferredSize().width;
    }

    private String displayText(E value) {
        return renderComponent(value) instanceof JLabel label ? label.getText() : String.valueOf(value);
    }

    private Component renderComponent(E value) {
        if (measureList == null) {
            measureList = new JList<>();
        }
        measureList.setFont(getFont());
        return getRenderer().getListCellRendererComponent(measureList, value, -1, false, false);
    }

    @SuppressWarnings("unchecked")
    private E selectedElement() {
        return (E) getSelectedItem();
    }

    private record SizeKey(ComboBoxUI ui, Font font, ListCellRenderer<?> renderer, Object prototype, Border border,
                           ComboBoxModel<?> model, int modelVersion, Object selected,
                           int minWidth, int maxWidth, int fixedHeight, float scale,
                           Object style, Object styleClass, Object minimumWidthProperty) {
    }

    private record DynamicLayout(int width, int height, boolean truncated) {
    }
}
