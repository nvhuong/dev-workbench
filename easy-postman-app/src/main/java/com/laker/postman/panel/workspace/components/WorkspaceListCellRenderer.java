package com.laker.postman.panel.workspace.components;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.GitRepoSource;
import com.laker.postman.model.Workspace;
import com.laker.postman.model.WorkspaceType;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * Trình kết xuất ô danh sách tùy chỉnh
 */
public class WorkspaceListCellRenderer extends DefaultListCellRenderer {
    private static final String HTML_START = "<html>";
    private static final String HTML_END = "</html>";

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof Workspace workspace) {
            configureWorkspaceIcon(workspace);
            configureWorkspaceText(workspace);
            configureWorkspaceStyle();
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }
        return this;
    }

    private void configureWorkspaceIcon(Workspace workspace) {
        FlatSVGIcon icon;
        if (workspace.getType() == WorkspaceType.LOCAL) {
            icon = IconUtil.createThemed("icons/local.svg", 20, 20);
        } else if (workspace.getGitRemoteUrl() == null || workspace.getGitRemoteUrl().trim().isEmpty()) {
            icon = IconUtil.create("icons/git-warning.svg", 20, 20);
        } else if (workspace.getGitRepoSource() == GitRepoSource.CLONED) {
            icon = IconUtil.create("icons/git-remote.svg", 20, 20);
        } else {
            icon = IconUtil.create("icons/git.svg", 20, 20);
        }
        setIcon(icon);
    }

    private void configureWorkspaceText(Workspace workspace) {
        StringBuilder text = new StringBuilder();
        text.append(HTML_START);

        Workspace current = WorkspaceService.getInstance().getCurrentWorkspace();
        boolean isCurrent = current != null && current.getId().equals(workspace.getId());
        String primaryColor = toHex(ModernColors.getPrimary());
        String secondaryColor = toHex(ModernColors.getTextSecondary());

        // Nếu đó là không gian làm việc hiện tại, hãy đặt màu văn bản thành màu chủ đề
        if (isCurrent) {
            text.append("<b style='color: ").append(primaryColor).append(";'>").append(workspace.getName()).append("</b>");
        } else {
            text.append("<b>").append(workspace.getName()).append("</b>");
        }

        text.append("<br>");

        // Phần mô tả cũng có màu khác nhau tùy thuộc vào việc nó có được đặt cho không gian làm việc hiện tại hay không
        if (isCurrent) {
            text.append("<small style='color: ").append(primaryColor).append(";'>");
        } else {
            text.append("<small style='color: ").append(secondaryColor).append(";'>");
        }

        text.append(workspace.getType() == WorkspaceType.LOCAL ?
                I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_LOCAL) : I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_GIT));

        // Một thông báo mô tả được hiển thị (trạng thái cảnh báo được biểu thị bằng biểu tượng màu cam)
        if (workspace.getDescription() != null && !workspace.getDescription().trim().isEmpty()) {
            text.append(" - ").append(workspace.getDescription());
        }

        text.append("</small>");
        text.append(HTML_END);

        setText(text.toString());
    }

    private void configureWorkspaceStyle() {
        setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        setIconTextGap(5); // Khoảng cách biểu tượng và văn bản
    }

    /**
     * Tự động tính toán chiều cao ô
     * Dựa trên kích thước phông chữ và nội dung (hai dòng văn bản: name+mô tả)
     */
    public static int calculateCellHeight() {
        Font font = FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1);
        FontMetrics metrics = new JLabel().getFontMetrics(font);

        // Chiều cao của hai dòng văn bản + Đệm trên và dưới + khoảng cách dòng
        int lineHeight = metrics.getHeight();
        int twoLinesHeight = lineHeight * 2;
        int verticalPadding = 10; // Lên và xuống5pixel
        int lineSpacing = 4; // khoảng cách dòng

        return twoLinesHeight + verticalPadding + lineSpacing;
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
