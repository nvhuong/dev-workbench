package com.nvhuong.devworkbench.panel.toolbox;

import com.nvhuong.devworkbench.common.component.MarkdownEditorPanel;
import javax.swing.*;
import java.awt.*;

/**
 * Markdown Bảng công cụ soạn thảo
 * cung cấp Markdown Khả năng chỉnh sửa và xem trước trực tiếp
 */
public class MarkdownToolPanel extends JPanel {

    private MarkdownEditorPanel markdownEditor;

    public MarkdownToolPanel() {
        initUI();
    }

    private void initUI() {
        ToolboxWorkbench.applyRoot(this);

        // Tạo Markdown Biên tập viên
        markdownEditor = new MarkdownEditorPanel();

        // Thiết lập nội dung mẫu
        String sampleContent = """
                # Chào mừng Markdown Biên tập viên

                Đây là một trong những mạnh mẽ Markdown Công cụ chỉnh sửa hỗ trợ xem trước theo thời gian thực.

                ## Cú pháp được hỗ trợ

                ### Định dạng văn bản
                - **In đậm** sử dụng `**văn bản**`
                - _in nghiêng_ sử dụng `_văn bản_`
                - ~~Gạch ngang~~ sử dụng `~~văn bản~~`
                - `Mã nội tuyến` Sử dụng dấu tích ngược

                ### danh sách
                1. Danh sách đặt hàng
                2. Mục 2

                - Danh sách không có thứ tự
                - Một mặt hàng khác

                ### Danh sách nhiệm vụ
                - [x] Nhiệm vụ đã hoàn thành
                - [ ] Danh sách việc cần làm

                ### khối mã
                ```java
                public class Example {
                    public static void main(String[] args) {
                        System.out.println("Hello, Markdown!");
                    }
                }
                ```

                ### cái bàn
                | Chức năng | trạng thái |
                | --- | --- |
                | Xem trước trực tiếp | ✅ |
                | Syntax highlighting | ✅ |
                | Xuất khẩu HTML | ✅ |

                ---

                Bắt đầu chỉnh sửa của bạn Markdown Hãy ghi lại nó! .
                """;

        markdownEditor.setText(sampleContent);

        add(markdownEditor, BorderLayout.CENTER);
    }
}
