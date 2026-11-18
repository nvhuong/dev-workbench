package com.nvhuong.devworkbench.util;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;

@UtilityClass
public class JComponentUtils {

    /**
     * Hình elip (.) được hiển thị vượt quá chiều rộng...）。Mặc địnhicon/paddingChiều rộng là 30px，Chiều rộng tối thiểu là200px。
     *
     * @param text      Văn bản gốc
     * @param component Thành phần được sử dụng để tính chiều rộng (ví dụJList、JLabelChờ đợi)
     * @return Văn bản vừa với chiều rộng sẽ tự động được bỏ qua và thêm vào...，Nếu không thì văn bản gốc
     */
    public static String ellipsisText(String text, JComponent component) {
        return ellipsisText(text, component, 30, 200);
    }

    /**
     * Hình elip (.) được hiển thị vượt quá chiều rộng...），Có thể tùy chỉnhicon/paddingchiều rộng và chiều rộng tối thiểu.
     *
     * @param text          Văn bản gốc
     * @param component     Thành phần được sử dụng để tính chiều rộng (ví dụJList、JLabelChờ đợi)
     * @param reservedWidth Chiều rộng dành riêng (ví dụiconvàpadding，đơn vịpx）Điều này được trừ khi tính chiều rộng văn bản tối đa30pixel và không gian còn lại được sử dụng để hiển thị văn bản
     * @param minWidth      Chiều rộng tối thiểu (đơn vịpx），Chỉ ra rằng ngay cả khi chiều rộng thành phần là0，Người ta cũng cho rằng có200Pixel có thể được sử dụng để hiển thị văn bản.
     * @return Văn bản vừa với chiều rộng sẽ tự động được bỏ qua và thêm vào...，Nếu không thì văn bản gốc
     */
    public static String ellipsisText(String text, JComponent component, int reservedWidth, int minWidth) {
        if (text == null) return null;
        FontMetrics fm = component.getFontMetrics(component.getFont()); // Lấy số liệu phông chữ
        int maxWidth = component.getWidth() > 0 ? component.getWidth() - reservedWidth : minWidth; // Kín đáoiconvàpadding
        if (maxWidth <= 0) return text;
        String ellipsis = "...";
        int width = fm.stringWidth(text);
        if (width <= maxWidth) return text;
        for (int i = text.length() - 1; i > 0; i--) {
            String sub = text.substring(0, i) + ellipsis;
            if (fm.stringWidth(sub) <= maxWidth) {
                return sub;
            }
        }
        return text;
    }
}