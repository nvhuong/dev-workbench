package com.nvhuong.devworkbench.mock.script;

import com.nvhuong.devworkbench.mock.model.MockRequest;
import com.nvhuong.devworkbench.mock.model.MockResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MockScriptContext {
    private final MockRequest request;
    private final MockResponse response;
    private final MockState state;
}
