package com.nvhuong.devworkbench.mock.app;

import com.nvhuong.devworkbench.ioc.Component;
import com.nvhuong.devworkbench.mock.script.MockScriptContext;
import com.nvhuong.devworkbench.mock.script.MockScriptExecutor;
import com.nvhuong.devworkbench.service.js.JsScriptExecutor;

import java.util.Map;

@Component
public class MockScriptExecutorAdapter implements MockScriptExecutor {
    @Override
    public void execute(String script, MockScriptContext context) throws Exception {
        JsScriptExecutor.executeScript(script, Map.of("pm", new MockPmApi(context)), ignored -> {
        });
    }
}
