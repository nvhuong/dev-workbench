package com.nvhuong.devworkbench.mock.cli;

import com.nvhuong.devworkbench.collection.model.CollectionDocument;
import com.nvhuong.devworkbench.mock.model.MockServerDefinition;

import java.nio.file.Path;
import java.util.List;

record MockRunWorkspace(Path directory,
                        CollectionDocument collections,
                        List<MockServerDefinition> servers) {
}
