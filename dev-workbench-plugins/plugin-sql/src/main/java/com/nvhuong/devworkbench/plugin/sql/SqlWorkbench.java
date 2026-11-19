package com.nvhuong.devworkbench.plugin.sql;

import com.nvhuong.devworkbench.common.component.ToolWindowChrome;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.util.FontsUtil;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

final class SqlWorkbench {
    private SqlWorkbench() {}
    static void applyRoot(JPanel panel) { panel.setLayout(new BorderLayout()); ToolWindowSurfaceStyle.applyCard(panel); panel.setBorder(BorderFactory.createEmptyBorder(8,10,8,10)); }
    static JPanel statusBar(Component item) { JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0)); p.setOpaque(false); p.setBorder(new EmptyBorder(4,0,0,0)); p.add(item); return p; }
    static JLabel sectionTitle(String text) { JLabel l=new JLabel(text); l.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD,-1)); return l; }
    static JPanel editorSection(String title, JComponent editor) { JPanel p=new JPanel(new BorderLayout(0,4)); p.setOpaque(false); p.add(sectionTitle(title),BorderLayout.NORTH); p.add(editor,BorderLayout.CENTER); return p; }
    static JSplitPane editorSplit(Component a, Component b, int location) { return ToolWindowChrome.createVerticalInnerSplitPane(a, b, location); }
    static JSplitPane horizontalSplit(Component a, Component b, int location) { return ToolWindowChrome.createHorizontalInnerSplitPane(a, b, location); }
}
