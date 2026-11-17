package com.laker.postman.common.component.editor;

import com.laker.postman.plugin.api.ScriptCompletionItem;
import com.laker.postman.plugin.api.ScriptCompletionKind;
import com.laker.postman.plugin.runtime.PluginRuntime;
import com.laker.postman.snippet.SnippetType;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class ScriptSnippetManagerTest {

    @AfterMethod
    public void clearPluginContributors() {
        PluginRuntime.getRegistry().clear();
    }

    @Test(description = "Khi chỉnh sửa ở giữa tập lệnh hiện có, tính năng tự động hoàn thành sẽ được kích hoạt dựa trên ký tự trước con trỏ")
    public void testAutoActivateUsesCaretPositionInsideExistingScript() {
        DefaultCompletionProvider provider =
                (DefaultCompletionProvider) ScriptSnippetManager.createCompletionProvider();
        JTextArea textArea = new JTextArea("const token = pm.response.text();");

        int caretAfterPmDot = textArea.getText().indexOf("pm.") + "pm.".length();
        textArea.setCaretPosition(caretAfterPmDot);

        assertTrue(provider.isAutoActivateOkay(textArea));
    }

    @Test(description = "Tự động hoàn thành không được kích hoạt khi con trỏ ở đầu")
    public void testAutoActivateReturnsFalseAtDocumentStart() {
        DefaultCompletionProvider provider =
                (DefaultCompletionProvider) ScriptSnippetManager.createCompletionProvider();
        JTextArea textArea = new JTextArea("pm.response.json()");

        textArea.setCaretPosition(0);

        assertFalse(provider.isAutoActivateOkay(textArea));
    }

    @Test(description = "Đóng góp hoàn thành trung lập với plugin cuối cùng sẽ được điều chỉnh cho phù hợp RSyntax basic/shorthand completion")
    public void shouldAdaptNeutralPluginCompletionsToEditorProvider() {
        PluginRuntime.getRegistry().registerScriptCompletionContributor(sink -> {
            sink.basic("pm.neutral.basic", "Neutral basic completion");
            sink.shorthand("neutral.snippet", "pm.neutral.run();", "Neutral shorthand completion");
        });

        DefaultCompletionProvider provider =
                (DefaultCompletionProvider) ScriptSnippetManager.createCompletionProvider();

        Completion basic = findCompletion(provider, "pm.neutral.basic");
        assertNotNull(basic);
        assertTrue(basic instanceof BasicCompletion);
        assertFalse(basic instanceof ShorthandCompletion);

        Completion shorthand = findCompletion(provider, "neutral.snippet");
        assertNotNull(shorthand);
        assertTrue(shorthand instanceof ShorthandCompletion);
        assertTrue("pm.neutral.run();".equals(shorthand.getReplacementText()));
    }

    @Test(description = "BASIC Nên sử dụng hoàn thiện trung lập inputText như provider Nhập")
    public void shouldUseInputTextForDirectBasicCompletionItems() {
        PluginRuntime.getRegistry().registerScriptCompletionContributor(sink ->
                sink.add(new ScriptCompletionItem(
                        ScriptCompletionKind.BASIC,
                        "pm.direct.basic",
                        "pm.direct.replacement",
                        "Direct basic completion"
                )));

        DefaultCompletionProvider provider =
                (DefaultCompletionProvider) ScriptSnippetManager.createCompletionProvider();

        assertNotNull(findCompletion(provider, "pm.direct.basic"));
        assertNull(findCompletion(provider, "pm.direct.replacement"));
    }

    @Test(description = "Ngoại lệ trong một lần đóng góp hoàn thành trình cắm thêm sẽ không cản trở việc hoàn thành trình cắm thêm tiếp theo")
    public void shouldContinueAfterPluginCompletionContributorThrows() {
        PluginRuntime.getRegistry().registerScriptCompletionContributor(sink -> {
            throw new IllegalStateException("broken contributor");
        });
        PluginRuntime.getRegistry().registerScriptCompletionContributor(sink ->
                sink.basic("pm.after.failure", "Contributor after failure"));

        DefaultCompletionProvider provider =
                (DefaultCompletionProvider) ScriptSnippetManager.createCompletionProvider();

        assertNotNull(findCompletion(provider, "pm.after.failure"));
    }

    @Test(description = "Các đoạn mã tích hợp nên được sử dụng trước tiên Postman chính thức pm.environment，hơn là lịch sử pm.env Viết tắt")
    public void builtInSnippetsShouldUseCanonicalEnvironmentApi() {
        for (SnippetType snippetType : SnippetType.values()) {
            assertFalse(snippetType.code.contains("pm.env."),
                    snippetType.name() + " should not advertise pm.env in built-in snippets");
        }
    }

    @Test(description = "Việc hoàn thành không có nghĩa là thời gian chạy hiện tại không thể hỗ trợ nó một cách đáng tin cậy Chai Phương pháp từ khóa")
    public void shouldNotAdvertiseUnsupportedChaiKeywordMethods() {
        DefaultCompletionProvider provider =
                (DefaultCompletionProvider) ScriptSnippetManager.createCompletionProvider();

        assertNull(findCompletion(provider, "expect.instanceof"));
        assertNull(findCompletion(provider, "expect.throw"));
        assertNull(findCompletion(provider, "expect.throw.error"));
    }

    private static Completion findCompletion(DefaultCompletionProvider provider, String inputText) {
        List<Completion> completions = provider.getCompletionByInputText(inputText);
        return completions == null || completions.isEmpty() ? null : completions.get(0);
    }
}
