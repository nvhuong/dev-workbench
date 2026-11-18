package com.nvhuong.devworkbench.startup;

import com.nvhuong.devworkbench.collection.cli.CollectionCliCommand;
import com.nvhuong.devworkbench.functional.cli.FunctionalCliCommand;
import com.nvhuong.devworkbench.mock.cli.MockCliCommand;
import com.nvhuong.devworkbench.mcp.McpCliCommand;
import com.nvhuong.devworkbench.performance.cli.PerformanceCliCommand;

import java.io.PrintStream;
import java.util.OptionalInt;

public class AppCommandRouter {
    private final PerformanceCliCommand performanceCliCommand;
    private final CollectionCliCommand collectionCliCommand;
    private final FunctionalCliCommand functionalCliCommand;
    private final MockCliCommand mockCliCommand;
    private final McpCliCommand mcpCliCommand;

    public AppCommandRouter() {
        this.performanceCliCommand = new PerformanceCliCommand();
        this.collectionCliCommand = new CollectionCliCommand();
        this.functionalCliCommand = new FunctionalCliCommand();
        this.mockCliCommand = new MockCliCommand();
        this.mcpCliCommand = new McpCliCommand();
    }

    public OptionalInt route(String[] args, PrintStream out, PrintStream err) {
        if (PerformanceCliCommand.matches(args)) {
            System.setProperty("java.awt.headless", "true");
            return OptionalInt.of(performanceCliCommand.run(args, out, err));
        }
        if (CollectionCliCommand.matches(args)) {
            System.setProperty("java.awt.headless", "true");
            return OptionalInt.of(collectionCliCommand.run(args, out, err));
        }
        if (FunctionalCliCommand.matches(args)) {
            System.setProperty("java.awt.headless", "true");
            return OptionalInt.of(functionalCliCommand.run(args, out, err));
        }
        if (MockCliCommand.matches(args)) {
            System.setProperty("java.awt.headless", "true");
            return OptionalInt.of(mockCliCommand.run(args, out, err));
        }
        if (McpCliCommand.matches(args)) {
            System.setProperty("java.awt.headless", "true");
            return OptionalInt.of(mcpCliCommand.run(args, out, err));
        }
        return OptionalInt.empty();
    }
}
