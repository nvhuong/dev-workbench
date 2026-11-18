package com.nvhuong.devworkbench.panel.performance;

import com.nvhuong.devworkbench.common.component.DevWorkbenchComboBox;
import com.nvhuong.devworkbench.performance.core.model.WebSocketPerformanceData;
import com.nvhuong.devworkbench.performance.core.model.NodeType;
import com.nvhuong.devworkbench.performance.model.PerformanceTreeNode;
import com.nvhuong.devworkbench.test.AbstractSwingUiTest;
import com.nvhuong.devworkbench.util.MessageKeys;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;

public class WebSocketStagePropertyPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldResolveReadModeHintForEveryCompletionMode() {
        for (WebSocketPerformanceData.CompletionMode mode : WebSocketPerformanceData.CompletionMode.values()) {
            String hint = WebSocketStagePropertyPanel.resolveReadModeHint(mode);

            assertFalse(hint == null || hint.isBlank(), mode.name());
            assertFalse(!hint.contains("Văn bản tin nhắn")
                    && !hint.toLowerCase(Locale.ROOT).contains("message bodies"), mode.name());
        }
    }

    @Test
    public void shouldUseDevWorkbenchComboBoxForReadMode() throws Exception {
        Field field = WebSocketStagePropertyPanel.class.getDeclaredField("completionModeBox");

        assertEquals(field.getType(), DevWorkbenchComboBox.class);
    }

    @Test
    public void nonSendStagesShouldNotCreateSendEditorsBeforeSelection() throws Exception {
        WebSocketStagePropertyPanel connectPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.CONNECT);
        WebSocketStagePropertyPanel readPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.READ);
        WebSocketStagePropertyPanel closePanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.CLOSE);

        assertNull(fieldValue(connectPanel, "customSendBodyArea", RSyntaxTextArea.class));
        assertNull(fieldValue(readPanel, "customSendBodyArea", RSyntaxTextArea.class));
        assertNull(fieldValue(closePanel, "customSendBodyArea", RSyntaxTextArea.class));
    }

    @Test
    public void programmaticSendNodeLoadShouldNotBeUndoable() throws Exception {
        WebSocketStagePropertyPanel[] holder = new WebSocketStagePropertyPanel[1];
        RSyntaxTextArea[] bodyEditor = new RSyntaxTextArea[1];
        RSyntaxTextArea[] scriptEditor = new RSyntaxTextArea[1];

        SwingUtilities.invokeAndWait(() -> {
            WebSocketPerformanceData data = new WebSocketPerformanceData();
            data.sendContentSource = WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT;
            data.customSendBody = "loaded body";
            data.sendPreScript = "loaded script";
            PerformanceTreeNode node = new PerformanceTreeNode("WS Send", NodeType.WS_SEND);
            node.webSocketPerformanceData = data;

            holder[0] = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.SEND);
            holder[0].setNode(node);
            bodyEditor[0] = readTextArea(holder[0], "customSendBodyArea");
            scriptEditor[0] = readTextArea(holder[0], "sendPreScriptArea");
            bodyEditor[0].undoLastAction();
            scriptEditor[0].undoLastAction();
        });

        assertEquals(bodyEditor[0].getText(), "loaded body");
        assertEquals(scriptEditor[0].getText(), "loaded script");
    }

    @Test
    public void shouldUseCompactWebSocketSendLabels() {
        ResourceBundle zh = ResourceBundle.getBundle("messages", Locale.forLanguageTag("vi"));
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_SEND_NONE), "Không được gửi");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY), "gửi");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY_REPEAT), "Gửi đi gửi lại nhiều lần");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_SEND_TAB_MESSAGE_TEMPLATE), "Content");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_SEND_TAB_PRE_SCRIPT), "Pre-script");

        ResourceBundle en = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_SEND_NONE), "No Send");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY), "Send");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY_REPEAT), "Repeat");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_SEND_TAB_MESSAGE_TEMPLATE), "Content");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_SEND_TAB_PRE_SCRIPT), "Pre-script");
    }

    @Test
    public void shouldDescribeReadAsNonClosingStep() {
        ResourceBundle zh = ResourceBundle.getBundle("messages", Locale.forLanguageTag("vi"));
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_MENU_ADD_WS_READ), "Thêm vào WS Read");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_NODE_READ), "WS Read");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_READ_MODE), "Chế độ đọc");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_FIRST_MESSAGE), "đọc 1 dải");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_MATCHED_MESSAGE), "Đọc có chứa văn bản");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_MESSAGE_COUNT), "đọc N dải");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_FIXED_DURATION), "Quan sát một lúc");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_MESSAGE_FILTER), "Chứa văn bản");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_HINT_MEMORY),
                "Nội dung thư chỉ được giữ lại nếu quá trình lọc, xác nhận hoặc trích xuất yêu cầu.");
        String firstMessageHint = zh.getString(MessageKeys.PERFORMANCE_WS_HINT_FIRST_MESSAGE);
        assertEquals(firstMessageHint, "đọc 1 Thanh tiếp tục; hết thời gian chờ không thành công.");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_HINT_MATCHED_MESSAGE),
                "Tiếp tục khi đọc văn bản có chứa; đây là lọc, không phải khẳng định.");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_HINT_FIXED_DURATION),
                "Tiếp tục sau khi quan sát thời gian đã đặt; không ngắt kết nối sớm.");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_HINT_MESSAGE_COUNT),
                "Tiếp tục khi đạt được số mục tiêu; thất bại nếu không đạt được mục tiêu sau khi hết thời gian chờ.");
        assertEquals(zh.getString(MessageKeys.PERFORMANCE_WS_CLOSE_HINT),
                "Chủ động đóng bước hiện tại khi thực hiện bước này WebSocket kết nối;Read Bước này chỉ chịu trách nhiệm đọc tin nhắn và không chịu trách nhiệm đóng kết nối.");

        ResourceBundle en = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        assertEquals(en.getString(MessageKeys.PERFORMANCE_MENU_ADD_WS_READ), "Add WS Read");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_NODE_READ), "WS Read");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_READ_MODE), "Read");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_FIRST_MESSAGE), "1 message");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_MATCHED_MESSAGE), "Until contains");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_MESSAGE_COUNT), "N messages");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_COMPLETION_FIXED_DURATION), "Observe duration");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_MESSAGE_FILTER), "Contains");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_HINT_MEMORY),
                "Message bodies are retained only for filters, assertions, or extractors.");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_HINT_FIRST_MESSAGE),
                "Continue after 1 message; timeout fails.");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_HINT_MATCHED_MESSAGE),
                "Continue when a message contains text; this filters, not asserts.");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_HINT_FIXED_DURATION),
                "Continue after the duration; early close fails.");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_HINT_MESSAGE_COUNT),
                "Continue at target count; timeout before target fails.");
        assertEquals(en.getString(MessageKeys.PERFORMANCE_WS_CLOSE_HINT),
                "When this step is reached, the current WebSocket connection is actively closed. Read steps only receive messages; they do not close the connection.");
    }

    private static <T> T fieldValue(Object owner, String fieldName, Class<T> type) throws Exception {
        Field field = WebSocketStagePropertyPanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(owner));
    }

    private static RSyntaxTextArea readTextArea(WebSocketStagePropertyPanel panel, String fieldName) {
        try {
            return fieldValue(panel, fieldName, RSyntaxTextArea.class);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
