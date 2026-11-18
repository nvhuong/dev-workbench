package com.nvhuong.devworkbench.common.component;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;
import java.text.ParseException;

/**
 * Phiên bản nâng cao JSpinner，Tự động gửi đầu vào
 *
 * <h2>Các tính năng chức năng</h2>
 * <ul>
 *   <li>Hiệu quả trong thời gian thực: dừng đầu vào300msSau đó tự động gửi (chống rung)</li>
 *   <li>Gửi ngoài tiêu điểm: Gửi ngay lập tức khi các điều khiển khác được nhấp vào</li>
 *   <li>Enter để gửi: Nhấn phím Enter để gửi ngay và chuyển tiêu điểm</li>
 *   <li>Xử lý ngoại lệ: Tự động khôi phục về giá trị hợp lệ cuối cùng khi đầu vào không hợp lệ</li>
 * </ul>
 *
 * <h2>Trải nghiệm người dùng</h2>
 * <ul>
 *   <li>✓ Đợi sau khi vào300ms → Gửi tự động</li>
 *   <li>✓ Bấm vào các điều khiển khác → Gửi ngay bây giờ</li>
 *   <li>✓ Bấm vào một khu vực trống → 300mstự động gửi</li>
 *   <li>✓ Nhấn phím Enter → Gửi và nhảy ngay bây giờ</li>
 * </ul>
 *
 * @author nvhuong
 * @since 4.2.20
 */
public class DevWorkbenchJSpinner extends JSpinner {

    /**
     * Thời gian trễ chống rung (mili giây)
     */
    private static final int DEBOUNCE_DELAY = 300;

    /**
     * Chống rung Timer
     */
    private final Timer debounceTimer;

    /**
     * Được tạo bằng cách sử dụng các mô hình dữ liệu DevWorkbenchJSpinner
     *
     * @param model Spinner mô hình dữ liệu
     */
    public DevWorkbenchJSpinner(SpinnerModel model) {
        super(model);

        // Tạo khả năng chống rung Timer
        this.debounceTimer = new Timer(DEBOUNCE_DELAY, e -> commitValue());
        this.debounceTimer.setRepeats(false);

        // Đầu vào tham số cấu hình duy trì hiển thị số nguyên thô để tránh định dạng mặc định 2,000。
        initNumberFormat();

        // Khởi tạo chức năng gửi tự động
        initAutoCommit();
    }

    private void initNumberFormat() {
        JComponent editor = getEditor();
        if (!(editor instanceof NumberEditor numberEditor)) {
            return;
        }

        DecimalFormat format = numberEditor.getFormat();
        format.setGroupingUsed(false);

        JFormattedTextField textField = numberEditor.getTextField();
        textField.setValue(getValue());
    }

    /**
     * Khởi tạo chức năng gửi tự động
     */
    private void initAutoCommit() {
        JComponent editor = getEditor();
        if (!(editor instanceof DefaultEditor)) {
            return;
        }

        JFormattedTextField textField = ((DefaultEditor) editor).getTextField();

        // Đặt hành vi mất tiêu điểm: giữ các chỉnh sửa hiện tại (tránh tự động hoàn nguyên về giá trị cũ)
        // PERSIST Ở chế độ này, việc gửi sẽ được thử khi mất tiêu điểm nhưng sẽ không được khôi phục tự động
        textField.setFocusLostBehavior(JFormattedTextField.PERSIST);

        // 1. Giám sát thay đổi văn bản: Tự động gửi sau khi dừng nhập liệu (có tính năng chống rung)
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                // Trì hoãn việc gửi trong khi người dùng đang gõ
                debounceTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                // Trì hoãn việc gửi trong khi người dùng đang xóa
                debounceTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Trì hoãn việc gửi khi thuộc tính văn bản thay đổi
                debounceTimer.restart();
            }
        });

        // 2. Giám sát ngoài tiêu điểm: Gửi ngay khi tiêu điểm rời đi
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                debounceTimer.stop();  // Dừng chống rung
                commitValue();         // Gửi ngay bây giờ
            }
        });

        // 3. Nhập key nghe: Gửi ngay và nhảy tập trung
        textField.addActionListener(e -> {
            debounceTimer.stop();  // Dừng chống rung
            if (commitValue()) {
                textField.transferFocus();
            }
        });
    }

    /**
     * Gửi giá trị hiện tại
     *
     * <p>Chiến lược cải tiến:
     * <ul>
     *   <li>Lưu và khôi phục vị trí con trỏ</li>
     *   <li>Nếu việc gửi không thành công, không làm gì cả và giữ nguyên thông tin đầu vào của người dùng</li>
     *   <li>Tránh định dạng không mong muốn trong quá trình nhập của người dùng</li>
     *   <li>Sử dụng việc trì hoãn thực thi để đảm bảo rằng vị trí con trỏ được khôi phục chính xác</li>
     * </ul>
     *
     * @return Liệu việc gửi có thành công hay không
     */
    private boolean commitValue() {
        JComponent editor = getEditor();
        if (!(editor instanceof DefaultEditor)) {
            return false;
        }

        JFormattedTextField textField = ((DefaultEditor) editor).getTextField();

        // Lưu vị trí con trỏ hiện tại và văn bản
        int caretPosition = textField.getCaretPosition();
        String currentText = textField.getText();

        try {
            // Hãy thử gửi bản chỉnh sửa
            commitEdit();

            // Sử dụng sau khi gửi thành công invokeLater Đảm bảo khôi phục con trỏ sau khi hoàn tất mọi định dạng
            SwingUtilities.invokeLater(() -> {
                try {
                    String newText = textField.getText();
                    // Tính toán vị trí con trỏ thích hợp
                    int newCaretPos;
                    if (newText.equals(currentText)) {
                        // Văn bản không thay đổi và vị trí con trỏ ban đầu được khôi phục
                        newCaretPos = Math.min(caretPosition, newText.length());
                    } else {
                        // Văn bản đã được định dạng, đặt con trỏ ở cuối
                        newCaretPos = newText.length();
                    }
                    textField.setCaretPosition(newCaretPos);
                } catch (Exception e) {
                    // Bỏ qua các ngoại lệ cài đặt con trỏ và không ảnh hưởng đến chức năng
                }
            });

            return true;
        } catch (ParseException | IllegalArgumentException ex) {
            // Gửi không thành công (ví dụ: dữ liệu nhập không hợp lệ hoặc nằm ngoài phạm vi)
            // Giữ văn bản hiện tại và vị trí con trỏ không thay đổi, cho phép người dùng tiếp tục chỉnh sửa
            // Điều này tránh định dạng bất thường trong quá trình nhập (ví dụ: "10001" -> "0010"）
            return false;
        }
    }

    /**
     * Dừng chống rung và chuyển giao văn bản hiện đang được chỉnh sửa ngay lập tức.
     *
     * @return Liệu văn bản đã chỉnh sửa hiện tại có được gửi tới thành công hay không spinner Người mẫu
     */
    public boolean commitCurrentEdit() {
        debounceTimer.stop();
        return commitValue();
    }

    /**
     * Gửi văn bản chỉnh sửa trước, sau đó đọc giá trị mô hình hiện tại.
     */
    public Object getCommittedValue() {
        commitCurrentEdit();
        return getValue();
    }

    /**
     * Gửi văn bản đã chỉnh sửa trước, sau đó nhấn int Đọc giá trị hiện tại.
     */
    public int getCommittedIntValue() {
        return ((Number) getCommittedValue()).intValue();
    }

    /**
     * Buộc gửi giá trị hiện tại (đối với  Ctrl/Cmd+S khi lưu)
     */
    public void forceCommit() {
        commitCurrentEdit();
    }

    public static DevWorkbenchJSpinner intSpinner(int value, int minimum, Integer maximum, int stepSize) {
        return new DevWorkbenchJSpinner(new SpinnerNumberModel(
                Integer.valueOf(value),
                Integer.valueOf(minimum),
                maximum == null ? null : Integer.valueOf(maximum),
                Integer.valueOf(stepSize)
        ));
    }
}
