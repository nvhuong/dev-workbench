package com.nvhuong.devworkbench.plugin.sql;

import com.formdev.flatlaf.FlatClientProperties;
import com.nvhuong.devworkbench.common.component.ToolWindowSurfaceStyle;
import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.util.FontsUtil;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/** "History" and "Saved" side panels of the SQL tool. */
final class SqlLibraryPanels {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss");

    /** What the side panels need from the SQL tool. */
    interface Host {
        void openSql(String title, String sql, String savedName);

        void insertSql(String sql);

        /** Saves the active editor; {@code askName} forces the name prompt ("Save as"). */
        void saveActiveEditor(boolean askName);

        /** Asks for a name and saves {@code sql}; returns the saved name or {@code null} if cancelled. */
        String saveAs(String suggestedName, String sql);

        String askNewName(String currentName);

        boolean confirm(String message);

        void copy(String text);

        void status(String text);
    }

    private SqlLibraryPanels() { }

    static String compact(String sql, int max) {
        String compact = sql == null ? "" : sql.replaceAll("\\s+", " ").trim();
        return compact.length() <= max ? compact : compact.substring(0, max - 1) + "…";
    }

    /** Executed statements, newest first, with search, re-open, insert, save and delete. */
    static final class HistoryPanel extends JPanel {
        final JTextField search = new JTextField();
        final DefaultListModel<SqlHistory.Entry> model = new DefaultListModel<>();
        final JList<SqlHistory.Entry> list = new WidthTrackingList<>(model);
        private final SqlHistory history;
        private final Host host;

        HistoryPanel(SqlHistory history, Host host) {
            super(new BorderLayout(0, 4));
            this.history = history;
            this.host = host;
            setOpaque(false);
            search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, SqlI18n.t("history_search"));
            search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
            search.getDocument().addDocumentListener(onChange(this::refresh));
            add(search, BorderLayout.NORTH);

            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer(new TwoLineRenderer<>(entry -> new String[]{
                    (entry.success() ? "" : "✗ ") + compact(entry.sql(), 90),
                    TIME.format(Instant.ofEpochMilli(entry.executedAt()).atZone(ZoneId.systemDefault()))
                            + join(entry.profile(), entry.summary(), entry.durationMillis() + " ms")},
                    entry -> !entry.success()));
            list.addMouseListener(doubleClickAndMenu(list, this::open, this::showMenu));
            list.getInputMap().put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open-history");
            list.getActionMap().put("open-history", swingAction(this::open));
            JScrollPane scroll = new JScrollPane(list);
            ToolWindowSurfaceStyle.applyFramedScrollPaneCard(scroll);
            add(scroll, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new GridLayout(3, 2, 4, 4));
            buttons.setOpaque(false);
            buttons.add(button("history_open", this::open));
            buttons.add(button("history_insert", this::insert));
            buttons.add(button("history_save", this::save));
            buttons.add(button("history_copy", this::copy));
            buttons.add(button("history_delete", this::delete));
            buttons.add(button("history_clear", this::clear));
            add(buttons, BorderLayout.SOUTH);
            refresh();
        }

        void refresh() {
            SqlHistory.Entry selected = list.getSelectedValue();
            model.clear();
            history.search(search.getText()).forEach(model::addElement);
            if (selected != null) list.setSelectedValue(selected, false);
        }

        private SqlHistory.Entry selected() {
            SqlHistory.Entry entry = list.getSelectedValue();
            if (entry == null) host.status(SqlI18n.t("history_select"));
            return entry;
        }

        void open() {
            SqlHistory.Entry entry = selected();
            if (entry != null) host.openSql(null, entry.sql(), null);
        }

        private void insert() {
            SqlHistory.Entry entry = selected();
            if (entry != null) host.insertSql(entry.sql());
        }

        private void save() {
            SqlHistory.Entry entry = selected();
            if (entry != null) host.saveAs(compact(entry.sql(), 40), entry.sql());
        }

        private void copy() {
            SqlHistory.Entry entry = selected();
            if (entry != null) host.copy(entry.sql());
        }

        private void delete() {
            SqlHistory.Entry entry = selected();
            if (entry == null) return;
            history.remove(entry);
            refresh();
        }

        private void clear() {
            if (history.entries().isEmpty() || !host.confirm(SqlI18n.t("history_clear_confirm"))) return;
            history.clear();
            refresh();
        }

        private void showMenu(MouseEvent event) {
            JPopupMenu menu = new JPopupMenu();
            ToolWindowSurfaceStyle.applyPopupMenuCard(menu);
            menu.add(menuItem("history_open", this::open));
            menu.add(menuItem("history_insert", this::insert));
            menu.add(menuItem("history_save", this::save));
            menu.add(menuItem("history_copy", this::copy));
            menu.addSeparator();
            menu.add(menuItem("history_delete", this::delete));
            menu.show(list, event.getX(), event.getY());
        }
    }

    /** Named scripts the user saved (one or many statements). */
    static final class SavedPanel extends JPanel {
        final JTextField search = new JTextField();
        final DefaultListModel<SqlSavedQueries.SavedQuery> model = new DefaultListModel<>();
        final JList<SqlSavedQueries.SavedQuery> list = new WidthTrackingList<>(model);
        private final SqlSavedQueries saved;
        private final Host host;

        SavedPanel(SqlSavedQueries saved, Host host) {
            super(new BorderLayout(0, 4));
            this.saved = saved;
            this.host = host;
            setOpaque(false);
            search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, SqlI18n.t("saved_search"));
            search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
            search.getDocument().addDocumentListener(onChange(this::refresh));
            add(search, BorderLayout.NORTH);

            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer(new TwoLineRenderer<>(query -> new String[]{query.name(), compact(query.sql(), 90)},
                    query -> false));
            list.addMouseListener(doubleClickAndMenu(list, this::open, this::showMenu));
            list.getInputMap().put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open-saved");
            list.getActionMap().put("open-saved", swingAction(this::open));
            JScrollPane scroll = new JScrollPane(list);
            ToolWindowSurfaceStyle.applyFramedScrollPaneCard(scroll);
            add(scroll, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new MigLayout("insets 0, novisualpadding, fillx, wrap 2", "[grow,fill][grow,fill]"));
            buttons.setOpaque(false);
            buttons.add(button("saved_open", this::open));
            buttons.add(button("saved_save_current", () -> host.saveActiveEditor(true)));
            buttons.add(button("saved_rename", this::rename));
            buttons.add(button("saved_delete", this::delete));
            JLabel hint = new JLabel(SqlI18n.t("saved_hint"));
            hint.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
            hint.setForeground(ModernColors.getTextSecondary());
            buttons.add(hint, "span 2");
            add(buttons, BorderLayout.SOUTH);
            refresh();
        }

        void refresh() {
            SqlSavedQueries.SavedQuery selected = list.getSelectedValue();
            model.clear();
            saved.search(search.getText()).forEach(model::addElement);
            if (selected != null) {
                for (int i = 0; i < model.size(); i++) {
                    if (model.get(i).name().equalsIgnoreCase(selected.name())) list.setSelectedIndex(i);
                }
            }
        }

        private SqlSavedQueries.SavedQuery selected() {
            SqlSavedQueries.SavedQuery query = list.getSelectedValue();
            if (query == null) host.status(SqlI18n.t("saved_select"));
            return query;
        }

        void open() {
            SqlSavedQueries.SavedQuery query = selected();
            if (query != null) host.openSql(query.name(), query.sql(), query.name());
        }

        private void rename() {
            SqlSavedQueries.SavedQuery query = selected();
            if (query == null) return;
            String name = host.askNewName(query.name());
            if (name == null || name.isBlank() || name.trim().equals(query.name())) return;
            if (!saved.rename(query.name(), name, System.currentTimeMillis())) {
                host.status(SqlI18n.t("saved_name_taken", name.trim()));
                return;
            }
            refresh();
            host.status(SqlI18n.t("saved_renamed", name.trim()));
        }

        private void delete() {
            SqlSavedQueries.SavedQuery query = selected();
            if (query == null || !host.confirm(SqlI18n.t("saved_delete_confirm", query.name()))) return;
            saved.delete(query.name());
            refresh();
        }

        private void showMenu(MouseEvent event) {
            JPopupMenu menu = new JPopupMenu();
            ToolWindowSurfaceStyle.applyPopupMenuCard(menu);
            menu.add(menuItem("saved_open", this::open));
            menu.add(menuItem("history_insert", () -> {
                SqlSavedQueries.SavedQuery query = selected();
                if (query != null) host.insertSql(query.sql());
            }));
            menu.add(menuItem("history_copy", () -> {
                SqlSavedQueries.SavedQuery query = selected();
                if (query != null) host.copy(query.sql());
            }));
            menu.add(menuItem("saved_rename", this::rename));
            menu.addSeparator();
            menu.add(menuItem("saved_delete", this::delete));
            menu.show(list, event.getX(), event.getY());
        }
    }

    /** Fits the list to the panel width so long SQL is cut with "…" instead of needing a horizontal scrollbar. */
    private static final class WidthTrackingList<T> extends JList<T> {
        WidthTrackingList(javax.swing.ListModel<T> model) {
            super(model);
        }

        @Override public boolean getScrollableTracksViewportWidth() {
            return true;
        }
    }

    /** List cell: bold-ish first line (SQL or name), small grey second line (details). */
    private static final class TwoLineRenderer<T> extends JPanel implements javax.swing.ListCellRenderer<T> {
        private final JLabel first = new JLabel();
        private final JLabel second = new JLabel();
        private final Function<T, String[]> lines;
        private final java.util.function.Predicate<T> failed;

        TwoLineRenderer(Function<T, String[]> lines, java.util.function.Predicate<T> failed) {
            super(new BorderLayout(0, 1));
            this.lines = lines;
            this.failed = failed;
            // User SQL must never be interpreted as HTML by Swing labels.
            first.putClientProperty("html.disable", Boolean.TRUE);
            second.putClientProperty("html.disable", Boolean.TRUE);
            second.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
            add(first, BorderLayout.NORTH);
            add(second, BorderLayout.SOUTH);
            setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            // Renderers are stamped by a CellRendererPane and never validated again after resizing: lay out the two
            // lines for every size the list paints with, otherwise they stay laid out for width 0 (invisible).
            doLayout();
        }

        @Override
        public java.awt.Component getListCellRendererComponent(JList<? extends T> list, T value, int index,
                                                               boolean selected, boolean focus) {
            String[] text = value == null ? new String[]{"", ""} : lines.apply(value);
            first.setText(text[0]);
            second.setText(text[1]);
            Color background = selected ? list.getSelectionBackground() : list.getBackground();
            Color foreground = selected ? list.getSelectionForeground() : list.getForeground();
            setBackground(background);
            setOpaque(true);
            first.setForeground(value != null && failed.test(value) && !selected ? ModernColors.getError() : foreground);
            second.setForeground(selected ? foreground : ModernColors.getTextSecondary());
            setToolTipText(value == null ? null : text[0]);
            return this;
        }
    }

    private static String join(String... parts) {
        StringBuilder text = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) continue;
            text.append("  ·  ").append(compact(part, 60));
        }
        return text.toString();
    }

    private static JButton button(String key, Runnable action) {
        JButton button = new JButton(SqlI18n.t(key));
        button.addActionListener(event -> action.run());
        return button;
    }

    private static JMenuItem menuItem(String key, Runnable action) {
        JMenuItem item = new JMenuItem(SqlI18n.t(key));
        item.addActionListener(event -> action.run());
        return item;
    }

    private static javax.swing.Action swingAction(Runnable runnable) {
        return new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { runnable.run(); }
        };
    }

    private static DocumentListener onChange(Runnable runnable) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { runnable.run(); }
            @Override public void removeUpdate(DocumentEvent event) { runnable.run(); }
            @Override public void changedUpdate(DocumentEvent event) { runnable.run(); }
        };
    }

    private static <T> MouseAdapter doubleClickAndMenu(JList<T> list, Runnable open,
                                                       java.util.function.Consumer<MouseEvent> menu) {
        return new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { popup(event); }

            @Override public void mouseReleased(MouseEvent event) { popup(event); }

            private void popup(MouseEvent event) {
                if (!event.isPopupTrigger()) return;
                int index = list.locationToIndex(event.getPoint());
                if (index >= 0) list.setSelectedIndex(index);
                if (list.getSelectedValue() != null) menu.accept(event);
            }

            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && javax.swing.SwingUtilities.isLeftMouseButton(event)) open.run();
            }
        };
    }
}
