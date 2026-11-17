package com.laker.postman.common.component.table;

import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.InputMethodEvent;
import java.text.AttributedString;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class EasySmartValueCellEditorTest extends AbstractSwingUiTest {

    private static final String LONG_TEXT = "Đây là nội dung đầu vào bằng tiếng Trung sẽ vượt quá đáng kể chiều rộng ô hiện tại và kích hoạt tính năng gói từ tự động";

    @Test
    public void shouldSwitchBetweenShortAndLongEditorsAfterInputSettles() throws Exception {
        AtomicReference<EasySmartValueCellEditor> editorRef = new AtomicReference<>();
        AtomicReference<JTable> tableRef = new AtomicReference<>();
        AtomicReference<JTextArea> textAreaRef = new AtomicReference<>();
        int[] originalRowHeight = new int[1];

        SwingUtilities.invokeAndWait(() -> {
            JTable table = createNarrowTable();
            EasySmartValueCellEditor editor = new EasySmartValueCellEditor();
            editor.getTableCellEditorComponent(table, "Văn bản ngắn", false, 0, 1);
            editor.containerPanel.setSize(80, table.getRowHeight());

            assertTrue(editor.textField.isVisible(), "Nội dung ngắn nên được sử dụng JTextField");
            originalRowHeight[0] = table.getRowHeight(0);
            editor.textField.setText(LONG_TEXT);

            editorRef.set(editor);
            tableRef.set(table);
        });
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            EasySmartValueCellEditor editor = editorRef.get();
            JTable table = tableRef.get();
            JScrollPane scrollPane = findMultiLineScrollPane(editor);
            JTextArea textArea = (JTextArea) scrollPane.getViewport().getView();

            assertFalse(editor.textField.isVisible());
            assertTrue(scrollPane.isVisible(), "Nội dung dài nên chuyển sang  JTextArea");
            assertEquals(editor.getCellEditorValue(), LONG_TEXT);
            assertTrue(table.getRowHeight(0) > originalRowHeight[0], "Nội dung dài sẽ mở rộng ngay lập tức đến chiều cao dòng");

            textAreaRef.set(textArea);
            textArea.setText("Văn bản ngắn");
        });
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            EasySmartValueCellEditor editor = editorRef.get();
            JTable table = tableRef.get();
            assertTrue(editor.textField.isVisible(), "Nó nên được chuyển trở lại khi nội dung trở nên ngắn hơn JTextField");
            assertFalse(findMultiLineScrollPane(editor).isVisible());
            assertEquals(editor.getCellEditorValue(), "Văn bản ngắn");
            assertEquals(table.getRowHeight(0), originalRowHeight[0]);
            assertEquals(textAreaRef.get().getText(), "Văn bản ngắn");
        });
    }

    @Test
    public void shouldDeferComponentSwitchUntilInputMethodCompositionFinishes() throws Exception {
        AtomicReference<EasySmartValueCellEditor> editorRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            JTable table = createNarrowTable();
            EasySmartValueCellEditor editor = new EasySmartValueCellEditor();
            editor.getTableCellEditorComponent(table, "Văn bản ngắn", false, 0, 1);
            editor.containerPanel.setSize(80, table.getRowHeight());

            editor.updateInputMethodComposition(inputMethodEvent(editor, "zhong", 0));
            editor.textField.setText(LONG_TEXT);
            editorRef.set(editor);
        });
        flushEdt();

        SwingUtilities.invokeAndWait(() ->
                assertTrue(editorRef.get().textField.isVisible(),
                        "Phương thức nhập không thể chuyển đổi thành phần tiêu điểm trong khi vẫn kết hợp văn bản")
        );

        SwingUtilities.invokeAndWait(() -> {
            EasySmartValueCellEditor editor = editorRef.get();
            editor.updateInputMethodComposition(inputMethodEvent(editor, "ở giữa", 1));
        });
        flushEdt();

        SwingUtilities.invokeAndWait(() -> {
            EasySmartValueCellEditor editor = editorRef.get();
            assertFalse(editor.textField.isVisible());
            assertTrue(findMultiLineScrollPane(editor).isVisible(),
                    "Sau khi phương thức nhập được kết hợp, quá trình chuyển đổi nhiều dòng tự động sẽ tiếp tục");
        });
    }

    private InputMethodEvent inputMethodEvent(EasySmartValueCellEditor editor,
                                              String text,
                                              int committedCharacterCount) {
        return new InputMethodEvent(
                editor.textField,
                InputMethodEvent.INPUT_METHOD_TEXT_CHANGED,
                new AttributedString(text).getIterator(),
                committedCharacterCount,
                null,
                null
        );
    }

    private JTable createNarrowTable() {
        JTable table = new JTable(1, 2);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setWidth(80);
        return table;
    }

    private JScrollPane findMultiLineScrollPane(EasySmartValueCellEditor editor) {
        for (Component component : editor.containerPanel.getComponents()) {
            if (component instanceof JScrollPane scrollPane) {
                return scrollPane;
            }
        }
        throw new AssertionError("Sẽ tồn tại một bảng cuộn trình soạn thảo nhiều dòng");
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // Drain the event-driven mode reevaluation.
        });
        SwingUtilities.invokeAndWait(() -> {
            // Drain nested focus/layout callbacks.
        });
    }
}
