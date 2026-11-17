package com.laker.postman.panel.collections.editor.request.sub;

import java.awt.*;

/**
 * Bố cục mặt nạ thẻ đáp ứng.
 * <p>
 * cardPanel và loadingOverlay Yêu cầu chồng chéo hoàn toàn, nhúng tương tự bố cục độc lập ResponsePanel Nó dễ dàng hơn để sử dụng lại và hiểu.
 */
final class ResponseCardOverlayLayout implements LayoutManager2 {

    @Override
    public void addLayoutComponent(String name, Component comp) {
        // Bố cục này không yêu cầu thêm các thành phần dựa trên tên của chúng.
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        // Bố cục này không yêu cầu loại bỏ logic thành phần.
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return parent.getSize();
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(0, 0);
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            int width = parent.getWidth();
            int height = parent.getHeight();
            for (Component component : parent.getComponents()) {
                component.setBounds(0, 0, width, height);
            }
        }
    }

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        // Bố cục này không yêu cầu ràng buộc.
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0.5f;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0.5f;
    }

    @Override
    public void invalidateLayout(Container target) {
        // Bố cục này không yêu cầu bộ nhớ đệm và không có logic vô hiệu.
    }
}
